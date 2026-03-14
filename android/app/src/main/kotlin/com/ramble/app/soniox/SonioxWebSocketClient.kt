package com.ramble.app.soniox

import com.ramble.app.RambleApp
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okio.ByteString.Companion.toByteString

/**
 * WebSocket client for Soniox real-time transcription.
 *
 * Matches the web implementation configuration:
 * - Model: stt-rt-v3
 * - Audio format: pcm_s16le (raw PCM 16-bit signed little-endian)
 * - Languages: en, zh, es, fr, de, ja, ko
 */
class SonioxWebSocketClient {

  sealed class Event {
    object Connected : Event()

    data class FinalWords(val text: String) : Event()

    data class ProvisionalWords(val text: String) : Event()

    data class Error(val message: String) : Event()

    object Disconnected : Event()
  }

  @Serializable
  data class SonioxConfig(
    val api_key: String,
    val model: String = "stt-rt-v3",
    val audio_format: String = "pcm_s16le",
    val sample_rate: Int = 16000,
    val num_channels: Int = 1,
    val language_hints: List<String> = listOf("en", "zh", "es", "fr", "de", "ja", "ko"),
    val enable_endpoint_detection: Boolean = true,
  )

  @Serializable data class SonioxToken(val text: String, val is_final: Boolean = false)

  @Serializable
  data class SonioxMessage(val tokens: List<SonioxToken>? = null, val error: String? = null)

  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true // Include fields with default values
  }

  // Filter out special tokens like <end>, <comma>, <period>, etc.
  private fun filterSpecialTokens(text: String): String {
    return text.replace(Regex("<[^>]+>"), "")
  }

  private val client =
    OkHttpClient.Builder()
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(0, TimeUnit.SECONDS) // No read timeout for WebSocket
      .writeTimeout(30, TimeUnit.SECONDS)
      .build()

  private var webSocket: WebSocket? = null
  private var isConnected = false

  // Tracks the cumulative final text seen so far in this session, used to compute deltas
  private var lastFinalText = ""

  // Buffer for audio recorded while connecting
  private val audioBuffer = mutableListOf<ByteArray>()
  private var isBuffering = false

  private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 64)
  val events: SharedFlow<Event> = _events.asSharedFlow()

  // Start buffering audio before connection is ready
  fun startBuffering() {
    isBuffering = true
    audioBuffer.clear()
  }

  suspend fun connect(apiKey: String) {
    if (isConnected) return
    lastFinalText = ""

    try {
      // Connect to Soniox WebSocket directly
      val request = Request.Builder().url(WEBSOCKET_URL).build()

      webSocket =
        client.newWebSocket(
          request,
          object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
              android.util.Log.d("SonioxWS", "WebSocket opened")
              isConnected = true

              // Get language hints from settings
              val languageHints =
                RambleApp.instance.settingsManager.settings.value.languageHints.toList()

              // Send configuration
              val config =
                SonioxConfig(api_key = apiKey, language_hints = languageHints)
              val configJson = json.encodeToString(SonioxConfig.serializer(), config)
              android.util.Log.d("SonioxWS", "Sending config: $configJson")
              webSocket.send(configJson)

              // Send all buffered audio that was recorded while connecting
              if (audioBuffer.isNotEmpty()) {
                android.util.Log.d("SonioxWS", "Sending ${audioBuffer.size} buffered audio chunks")
                for (chunk in audioBuffer) {
                  webSocket.send(chunk.toByteString())
                }
                audioBuffer.clear()
              }
              isBuffering = false

              _events.tryEmit(Event.Connected)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
              android.util.Log.d("SonioxWS", "Message received: $text")
              try {
                val message = json.decodeFromString<SonioxMessage>(text)

                if (message.error != null) {
                  android.util.Log.e("SonioxWS", "Error from Soniox: ${message.error}")
                  _events.tryEmit(Event.Error(message.error))
                  return
                }

                // Handle tokens format (stt-rt-v3)
                // Each message is a rolling window: final tokens come first, then non-final.
                // We emit newly finalized text (delta since last message) and the current
                // provisional tail as a single coordinated update.
                message.tokens?.let { tokens ->
                  if (tokens.isNotEmpty()) {
                    var finalText = ""
                    var nonFinalText = ""

                    for (token in tokens) {
                      val cleanText = filterSpecialTokens(token.text)
                      if (token.is_final) {
                        finalText += cleanText
                      } else {
                        nonFinalText += cleanText
                      }
                    }

                    // Compute the delta: only emit final text that is new since last message
                    val newFinalText = if (finalText.startsWith(lastFinalText)) {
                      finalText.substring(lastFinalText.length)
                    } else {
                      finalText
                    }
                    lastFinalText = finalText

                    if (newFinalText.isNotEmpty()) {
                      _events.tryEmit(Event.FinalWords(newFinalText))
                    }
                    // Always sync provisional (replaces whatever was showing before)
                    _events.tryEmit(Event.ProvisionalWords(nonFinalText))
                  }
                }
              } catch (e: Exception) {
                // Ignore parse errors for unknown message formats
              }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
              android.util.Log.e("SonioxWS", "WebSocket failure: ${t.message}", t)
              isConnected = false
              _events.tryEmit(Event.Error(t.message ?: "Connection failed"))
              _events.tryEmit(Event.Disconnected)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
              android.util.Log.d("SonioxWS", "WebSocket closing: code=$code, reason=$reason")
              webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
              android.util.Log.d("SonioxWS", "WebSocket closed: code=$code, reason=$reason")
              isConnected = false
              _events.tryEmit(Event.Disconnected)
            }
          },
        )
    } catch (e: Exception) {
      _events.emit(Event.Error(e.message ?: "Connection error"))
    }
  }

  fun sendAudio(data: ByteArray) {
    if (isConnected) {
      webSocket?.send(data.toByteString())
    } else if (isBuffering) {
      // Buffer audio while connecting
      audioBuffer.add(data.copyOf())
    }
  }

  // Send empty binary frame to signal end-of-audio, letting Soniox finalize remaining tokens.
  // The WebSocket will close naturally after Soniox sends its last message, firing Disconnected.
  fun finalizeAudio() {
    if (isConnected) {
      webSocket?.send(ByteArray(0).toByteString())
    }
  }

  fun disconnect() {
    isConnected = false
    isBuffering = false
    lastFinalText = ""
    audioBuffer.clear()
    webSocket?.close(1000, "User stopped")
    webSocket = null
  }

  companion object {
    private const val WEBSOCKET_URL = "wss://stt-rt.soniox.com/transcribe-websocket"
  }
}
