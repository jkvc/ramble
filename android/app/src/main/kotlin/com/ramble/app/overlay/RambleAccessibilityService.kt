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
    private var provisionalState: ProvisionalState? = null
    private var isInsertingText = false

    private var isPendingDisconnect = false
    private var disconnectJob: Job? = null

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
        stopRecording()
        pillView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        pillView = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (isInsertingText) return

        // We track focus events but don't need to store the node —
        // we find the focused node fresh each time we insert text
    }

    override fun onInterrupt() {
        stopRecording()
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
                // Open the app
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                intent?.let { startActivity(it) }
            }
            PillState.READY -> startRecording()
            PillState.CONNECTING, PillState.RECORDING -> stopRecording()
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
                        insertFinalText(event.text)
                        if (isPendingDisconnect) {
                            scheduleDisconnect()
                        }
                    }
                    is SonioxWebSocketClient.Event.ProvisionalWords -> {
                        insertProvisionalText(event.text)
                    }
                    is SonioxWebSocketClient.Event.Error -> {
                        stopRecording()
                    }
                    is SonioxWebSocketClient.Event.Disconnected -> {
                        stopRecording()
                    }
                }
            }
        }

        // Watch API key state
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

    private fun stopRecording() {
        val wasRecording = pillState == PillState.RECORDING || pillState == PillState.CONNECTING

        audioRecorder.stop()

        if (wasRecording) {
            isPendingDisconnect = true
            scheduleDisconnect()
        } else {
            sonioxClient.disconnect()
        }

        provisionalState = null
        pillState = if (RambleApp.instance.apiKeyManager.hasApiKey) PillState.READY else PillState.NO_API_KEY
        updatePillState()
    }

    private fun scheduleDisconnect() {
        disconnectJob?.cancel()
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

        // Insert a trailing space after final transcription
        val node = findFocusedInputNode() ?: return
        val currentText = node.text?.toString() ?: ""
        val cursorEnd = getCursorEnd(node, currentText)
        val result = TextInsertion.insertTextAtCursor(currentText, cursorEnd, cursorEnd, " ")
        performTextAction(node, result)
        node.recycle()
    }

    private fun insertFinalText(text: String) {
        val node = findFocusedInputNode() ?: return
        val currentText = node.text?.toString() ?: ""
        val cursorPos = getCursorEnd(node, currentText)

        val (result, newProvisional) = TextInsertion.applyFinalText(
            currentText, cursorPos, provisionalState, text
        )
        provisionalState = newProvisional
        performTextAction(node, result)
        node.recycle()
    }

    private fun insertProvisionalText(text: String) {
        val node = findFocusedInputNode() ?: return
        val currentText = node.text?.toString() ?: ""
        val cursorPos = getCursorEnd(node, currentText)

        val (result, newProvisional) = TextInsertion.applyProvisionalText(
            currentText, cursorPos, provisionalState, text
        )
        provisionalState = newProvisional
        performTextAction(node, result)
        node.recycle()
    }

    private fun findFocusedInputNode(): AccessibilityNodeInfo? {
        return rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
    }

    private fun getCursorEnd(node: AccessibilityNodeInfo, text: String): Int {
        val selection = node.textSelectionEnd
        return if (selection >= 0) selection else text.length
    }

    private fun performTextAction(node: AccessibilityNodeInfo, result: InsertionResult) {
        isInsertingText = true
        try {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, result.text)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

            // Set cursor position
            val selectionArgs = Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, result.cursorPosition)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, result.cursorPosition)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs)
        } finally {
            isInsertingText = false
        }
    }

    private fun updatePillState() {
        pillView?.let { FloatingPillView.updatePillState(it, pillState) }
    }
}
