package com.ramble.app.ime

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ramble.app.RambleApp
import com.ramble.app.audio.AudioRecorder
import com.ramble.app.soniox.SonioxWebSocketClient
import com.ramble.app.ui.theme.RambleTheme
import kotlinx.coroutines.*

/**
 * Ramble Voice Keyboard - InputMethodService
 * 
 * Provides a record button for voice-to-text transcription.
 * Text is inserted directly into the active text field.
 */
class RambleKeyboardService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    
    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry
    
    private val sonioxClient = SonioxWebSocketClient()
    private val audioRecorder = AudioRecorder()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var isRecording = mutableStateOf(false)
    private var isConnecting = mutableStateOf(false)
    private var error = mutableStateOf<String?>(null)
    private var provisional = mutableStateOf("")
    
    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        
        // Listen for Soniox events
        scope.launch {
            sonioxClient.events.collect { event ->
                when (event) {
                    is SonioxWebSocketClient.Event.Connected -> {
                        isConnecting.value = false
                        isRecording.value = true
                        // Start audio capture
                        audioRecorder.start { audioData ->
                            sonioxClient.sendAudio(audioData)
                        }
                    }
                    is SonioxWebSocketClient.Event.FinalWords -> {
                        // Insert final text into the text field
                        currentInputConnection?.commitText(event.text, 1)
                        provisional.value = ""
                    }
                    is SonioxWebSocketClient.Event.ProvisionalWords -> {
                        // Show provisional text as composing
                        provisional.value = event.text
                        currentInputConnection?.setComposingText(event.text, 1)
                    }
                    is SonioxWebSocketClient.Event.Error -> {
                        error.value = event.message
                        stopRecording()
                    }
                    is SonioxWebSocketClient.Event.Disconnected -> {
                        stopRecording()
                    }
                }
            }
        }
    }
    
    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
    
    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        // Stop recording when keyboard is hidden
        if (isRecording.value) {
            stopRecording()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        stopRecording()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
    
    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@RambleKeyboardService)
            setViewTreeSavedStateRegistryOwner(this@RambleKeyboardService)
            
            setContent {
                RambleTheme {
                    KeyboardView(
                        isRecording = isRecording.value,
                        isConnecting = isConnecting.value,
                        error = error.value,
                        isLoggedIn = RambleApp.instance.authManager.isLoggedIn,
                        onRecordClick = { toggleRecording() },
                        onSettingsClick = { openSettings() }
                    )
                }
            }
        }
        
        return view
    }
    
    private fun toggleRecording() {
        // Check if user is logged in
        if (!RambleApp.instance.authManager.isLoggedIn) {
            error.value = "Please log in via the Ramble app first"
            return
        }
        
        // Check microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            error.value = "Microphone permission required. Open Ramble app to grant."
            return
        }
        
        if (isRecording.value || isConnecting.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }
    
    private fun startRecording() {
        error.value = null
        isConnecting.value = true
        
        scope.launch {
            sonioxClient.connect()
        }
    }
    
    private fun stopRecording() {
        isRecording.value = false
        isConnecting.value = false
        audioRecorder.stop()
        sonioxClient.disconnect()
        
        // Finish any composing text
        currentInputConnection?.finishComposingText()
        provisional.value = ""
    }
    
    private fun openSettings() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        intent?.let { startActivity(it) }
    }
}
