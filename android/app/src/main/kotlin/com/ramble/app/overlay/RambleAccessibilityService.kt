package com.ramble.app.overlay

import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import com.ramble.app.RambleApp
import com.ramble.app.audio.AudioRecorder
import com.ramble.app.soniox.SonioxWebSocketClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class RambleAccessibilityService : AccessibilityService() {

    companion object {
        val isRunning = MutableStateFlow(false)

        private const val PREFS_NAME = "ramble_overlay"
        private const val PREF_Y_POSITION = "pill_y"
        private const val PREF_RIGHT_EDGE = "pill_right_edge"
        private const val DRAG_THRESHOLD_DP = 10
    }

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: SharedPreferences
    private var pillView: FrameLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val sonioxClient = SonioxWebSocketClient()
    private val audioRecorder = AudioRecorder()

    private var pillState = PillState.READY

    // Transcript accumulated during recording — final only, provisional shown in pill UI
    private val finalTranscript = StringBuilder()
    private var provisionalText = ""

    private var isFinalizing = false

    override fun onServiceConnected() {
        super.onServiceConnected()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val pill = FloatingPillView.createPillView(this)
        pillView = pill

        val display = resources.displayMetrics
        val isRightEdge = prefs.getBoolean(PREF_RIGHT_EDGE, true)
        val savedY = prefs.getInt(PREF_Y_POSITION, display.heightPixels / 2)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (isRightEdge) display.widthPixels else 0
            y = savedY
        }
        layoutParams = params

        windowManager.addView(pill, params)
        setupTouchListener(pill, params)
        setupEventCollection()

        updatePillState()
        isRunning.value = true
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning.value = false
        scope.cancel()
        sonioxClient.disconnect()
        audioRecorder.stop()
        pillView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        pillView = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        stopRecording(paste = false)
    }


    private fun setupTouchListener(pill: View, params: WindowManager.LayoutParams) {
        val threshold = (DRAG_THRESHOLD_DP * resources.displayMetrics.density).toInt()
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        pill.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (!isDragging && (dx * dx + dy * dy > threshold * threshold)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(pill, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        snapToEdge(pill, params)
                    } else {
                        onPillTapped()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun snapToEdge(pill: View, params: WindowManager.LayoutParams) {
        val display = resources.displayMetrics
        val screenWidth = display.widthPixels
        val midX = screenWidth / 2

        val isRightEdge = params.x + (pill.width / 2) > midX
        val targetX = if (isRightEdge) screenWidth else 0

        val animator = ValueAnimator.ofInt(params.x, targetX)
        animator.duration = 200
        animator.addUpdateListener { animation ->
            params.x = animation.animatedValue as Int
            try { windowManager.updateViewLayout(pill, params) } catch (_: Exception) {}
        }
        animator.start()

        prefs.edit()
            .putBoolean(PREF_RIGHT_EDGE, isRightEdge)
            .putInt(PREF_Y_POSITION, params.y)
            .apply()
    }

    private fun onPillTapped() {
        when (pillState) {
            PillState.NO_API_KEY -> {
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                intent?.let { startActivity(it) }
            }
            PillState.READY -> startRecording()
            PillState.CONNECTING, PillState.RECORDING -> stopRecording(paste = true)
        }
    }

    private fun setupEventCollection() {
        scope.launch {
            sonioxClient.events.collect { event ->
                when (event) {
                    is SonioxWebSocketClient.Event.Connected -> {
                        pillState = PillState.RECORDING
                        updatePillState()
                    }
                    is SonioxWebSocketClient.Event.FinalWords -> {
                        finalTranscript.append(event.text)
                        provisionalText = ""
                        updatePillTranscript()
                    }
                    is SonioxWebSocketClient.Event.ProvisionalWords -> {
                        provisionalText = event.text
                        updatePillTranscript()
                    }
                    is SonioxWebSocketClient.Event.Error -> {
                        isFinalizing = false
                        stopRecording(paste = false)
                    }
                    is SonioxWebSocketClient.Event.Disconnected -> {
                        if (isFinalizing) {
                            isFinalizing = false
                            pasteAndReset()
                        }
                    }
                }
            }
        }

        scope.launch {
            RambleApp.instance.apiKeyManager.apiKey.collect { key ->
                if (key == null && pillState != PillState.RECORDING && pillState != PillState.CONNECTING) {
                    pillState = PillState.NO_API_KEY
                    updatePillState()
                } else if (key != null && pillState == PillState.NO_API_KEY) {
                    pillState = PillState.READY
                    updatePillState()
                }
            }
        }
    }

    private fun startRecording() {
        val apiKey = RambleApp.instance.apiKeyManager.apiKey.value ?: return

        finalTranscript.clear()
        provisionalText = ""

        pillState = PillState.CONNECTING
        updatePillState()

        sonioxClient.startBuffering()

        audioRecorder.start { audioData ->
            sonioxClient.sendAudio(audioData)
        }

        scope.launch {
            sonioxClient.connect(apiKey)
        }
    }

    private fun stopRecording(paste: Boolean) {
        val wasRecording = pillState == PillState.RECORDING || pillState == PillState.CONNECTING

        audioRecorder.stop()

        if (wasRecording && paste) {
            // Signal Soniox that audio is done — it will flush remaining tokens and close the
            // WebSocket. The Disconnected event will then trigger pasteAndReset().
            isFinalizing = true
            sonioxClient.finalizeAudio()
        } else {
            isFinalizing = false
            sonioxClient.disconnect()
            finalTranscript.clear()
            provisionalText = ""
        }

        pillState = if (RambleApp.instance.apiKeyManager.hasApiKey) PillState.READY else PillState.NO_API_KEY
        updatePillState()
    }

    private fun pasteAndReset() {
        val text = finalTranscript.toString().trim()
        finalTranscript.clear()
        provisionalText = ""
        sonioxClient.disconnect()

        if (text.isNotEmpty()) {
            pasteTextAtCursor(text)
        }
    }

    private fun pasteTextAtCursor(text: String) {
        val node = findFocusedInputNode() ?: return
        val currentText = node.text?.toString() ?: ""
        val cursor = node.textSelectionEnd.let { if (it >= 0) it else currentText.length }

        // Insert with a trailing space
        val insert = if (cursor > 0 && currentText.getOrNull(cursor - 1) != ' ') " $text " else "$text "
        val newText = currentText.substring(0, cursor) + insert + currentText.substring(cursor)
        val newCursor = cursor + insert.length

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

        val selArgs = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newCursor)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newCursor)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selArgs)
        node.recycle()
    }

    private fun findFocusedInputNode(): AccessibilityNodeInfo? {
        return rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
    }

    private fun updatePillTranscript() {
        pillView?.let {
            FloatingPillView.updateTranscript(it, finalTranscript.toString(), provisionalText)
        }
    }

    private fun updatePillState() {
        pillView?.let { FloatingPillView.updatePillState(it, pillState) }
    }
}
