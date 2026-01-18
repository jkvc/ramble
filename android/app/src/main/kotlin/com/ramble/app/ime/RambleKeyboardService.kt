package com.ramble.app.ime

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.*
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ramble.app.RambleApp
import com.ramble.app.audio.AudioRecorder
import com.ramble.app.soniox.SonioxWebSocketClient
import com.ramble.app.ui.theme.RambleTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.delay

/**
 * Ramble Voice Keyboard - InputMethodService
 * 
 * Provides a record button for voice-to-text transcription.
 * Text is inserted directly into the active text field.
 */
class RambleKeyboardService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    
    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore = store
    
    private val sonioxClient = SonioxWebSocketClient()
    private val audioRecorder = AudioRecorder()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var isRecording = mutableStateOf(false)
    private var isConnecting = mutableStateOf(false)
    private var error = mutableStateOf<String?>(null)
    private var provisional = mutableStateOf("")
    private var isPendingDisconnect = false
    private var disconnectJob: kotlinx.coroutines.Job? = null
    
    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        
        // Listen for Soniox events
        scope.launch {
            sonioxClient.events.collect { event ->
                when (event) {
                    is SonioxWebSocketClient.Event.Connected -> {
                        // Connection established, audio was already started in startRecording
                        isConnecting.value = false
                        // isRecording is already true from startRecording
                    }
                    is SonioxWebSocketClient.Event.FinalWords -> {
                        // Insert final text into the text field
                        currentInputConnection?.commitText(event.text, 1)
                        provisional.value = ""
                        
                        // If we're pending disconnect and got final words, schedule disconnect
                        if (isPendingDisconnect) {
                            scheduleDisconnect()
                        }
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
        // Ensure lifecycle is resumed when input view starts
        if (lifecycleRegistry.currentState != Lifecycle.State.RESUMED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
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
        // Lifecycle must be RESUMED before Compose can work
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        
        // Use custom ComposeView that sets up its own lifecycle before attaching
        val view = ImeComposeView(this, this@RambleKeyboardService)
        
        view.setContent {
            RambleTheme {
                KeyboardView(
                    isRecording = isRecording.value,
                    isConnecting = isConnecting.value,
                    error = error.value,
                    isLoggedIn = RambleApp.instance.authManager.isLoggedIn,
                    onRecordClick = { toggleRecording() },
                    onSettingsClick = { openSettings() },
                    onKeyClick = { key -> commitText(key) },
                    onBackspace = { deleteBackward() },
                    onEnter = { sendEnter() },
                    onSpace = { commitText(" ") }
                )
            }
        }
        
        return view
    }
    
    private fun commitText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }
    
    private fun deleteBackward() {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }
    
    private fun sendEnter() {
        currentInputConnection?.commitText("\n", 1)
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
        isRecording.value = true  // Show recording state immediately
        
        // Start buffering audio immediately
        sonioxClient.startBuffering()
        
        // Start audio capture immediately (buffers while connecting)
        audioRecorder.start { audioData ->
            sonioxClient.sendAudio(audioData)
        }
        
        // Connect to Soniox in parallel
        scope.launch {
            sonioxClient.connect()
        }
    }
    
    private fun stopRecording() {
        val wasRecording = isRecording.value
        isRecording.value = false
        isConnecting.value = false
        
        // Stop audio capture immediately
        audioRecorder.stop()
        
        if (wasRecording) {
            // Don't disconnect immediately - wait for remaining tokens
            isPendingDisconnect = true
            // Schedule disconnect after a delay to allow remaining tokens to arrive
            scheduleDisconnect()
        } else {
            // Not recording, just disconnect
            sonioxClient.disconnect()
            currentInputConnection?.finishComposingText()
            provisional.value = ""
        }
    }
    
    private fun scheduleDisconnect() {
        // Cancel any existing disconnect job
        disconnectJob?.cancel()
        
        // Wait 500ms for any remaining tokens, then disconnect
        disconnectJob = scope.launch {
            delay(500)
            finalizeDisconnect()
        }
    }
    
    private fun finalizeDisconnect() {
        isPendingDisconnect = false
        disconnectJob?.cancel()
        disconnectJob = null
        
        sonioxClient.disconnect()
        
        // Finish any composing text
        currentInputConnection?.finishComposingText()
        provisional.value = ""
        
        // Insert a space so next recording doesn't stick to previous word
        currentInputConnection?.commitText(" ", 1)
    }
    
    private fun openSettings() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("start_destination", "settings")
        }
        intent?.let { startActivity(it) }
    }
}
