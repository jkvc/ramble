package com.ramble.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Captures audio from the microphone and sends it as raw PCM data.
 * 
 * Configuration matches Soniox requirements:
 * - 16kHz sample rate
 * - Mono channel
 * - 16-bit PCM (signed little-endian)
 */
class AudioRecorder {
    
    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_MULTIPLIER = 2
    }
    
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isRecording = false
    
    private val bufferSize = maxOf(
        AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_MULTIPLIER,
        3200 // At least 100ms of audio at 16kHz 16-bit mono
    )
    
    @SuppressLint("MissingPermission")
    fun start(onAudioData: (ByteArray) -> Unit) {
        if (isRecording) return
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                throw IllegalStateException("AudioRecord failed to initialize")
            }
            
            isRecording = true
            audioRecord?.startRecording()
            
            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                val buffer = ShortArray(bufferSize / 2)
                
                while (isActive && isRecording) {
                    val shortsRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    
                    if (shortsRead > 0) {
                        // Convert shorts to bytes (little-endian)
                        val byteBuffer = ByteBuffer.allocate(shortsRead * 2)
                        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                        for (i in 0 until shortsRead) {
                            byteBuffer.putShort(buffer[i])
                        }
                        
                        onAudioData(byteBuffer.array())
                    }
                }
            }
        } catch (e: Exception) {
            stop()
            throw e
        }
    }
    
    fun stop() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
        audioRecord = null
    }
}
