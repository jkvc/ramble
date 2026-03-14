package com.ramble.app.overlay

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.ramble.app.R

object FloatingPillView {

    private fun dpToPx(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    fun createPillView(context: Context): FrameLayout {
        val cornerRadius = dpToPx(context, 24).toFloat()

        val pill = FrameLayout(context)

        val background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(
                cornerRadius, cornerRadius,
                cornerRadius, cornerRadius,
                cornerRadius, cornerRadius,
                cornerRadius, cornerRadius
            )
            setColor(COLOR_READY)
        }
        pill.background = background
        pill.elevation = dpToPx(context, 6).toFloat()
        pill.alpha = 0.7f

        // Collapsed view: just the mic icon
        val collapsedView = FrameLayout(context).apply {
            tag = TAG_COLLAPSED
            layoutParams = FrameLayout.LayoutParams(dpToPx(context, 48), dpToPx(context, 72))
        }
        val iconSize = dpToPx(context, 28)
        val icon = ImageView(context).apply {
            setImageResource(R.drawable.ic_mic)
            setColorFilter(0xFFFFFFFF.toInt())
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.CENTER
            }
        }
        collapsedView.addView(icon)
        pill.addView(collapsedView)

        // Expanded view: transcript text + stop icon, hidden initially
        val expandedView = LinearLayout(context).apply {
            tag = TAG_EXPANDED
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(context, 72)
            )
            setPadding(dpToPx(context, 12), 0, dpToPx(context, 12), 0)
            visibility = android.view.View.GONE
        }

        val transcriptText = TextView(context).apply {
            tag = TAG_TRANSCRIPT
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
            maxWidth = dpToPx(context, 220)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.START
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        expandedView.addView(transcriptText)

        val stopIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_stop)
            setColorFilter(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                marginStart = dpToPx(context, 8)
            }
        }
        expandedView.addView(stopIcon)

        pill.addView(expandedView)

        return pill
    }

    fun updatePillState(pill: FrameLayout, state: PillState) {
        val background = pill.background as? GradientDrawable ?: return
        val collapsedView = pill.findViewWithTag<FrameLayout>(TAG_COLLAPSED) ?: return
        val expandedView = pill.findViewWithTag<LinearLayout>(TAG_EXPANDED) ?: return
        val icon = collapsedView.getChildAt(0) as? ImageView ?: return

        val color = when (state) {
            PillState.NO_API_KEY -> COLOR_NO_API_KEY
            PillState.READY -> COLOR_READY
            PillState.CONNECTING -> COLOR_CONNECTING
            PillState.RECORDING -> COLOR_RECORDING
        }
        background.setColor(color)

        pill.alpha = when (state) {
            PillState.RECORDING, PillState.CONNECTING -> 1.0f
            else -> 0.7f
        }

        when (state) {
            PillState.RECORDING -> {
                collapsedView.visibility = android.view.View.GONE
                expandedView.visibility = android.view.View.VISIBLE
            }
            else -> {
                collapsedView.visibility = android.view.View.VISIBLE
                expandedView.visibility = android.view.View.GONE
                icon.setImageResource(R.drawable.ic_mic)
            }
        }
    }

    fun updateTranscript(pill: FrameLayout, finalText: String, provisionalText: String) {
        val transcriptView = pill.findViewWithTag<TextView>(TAG_TRANSCRIPT) ?: return
        val display = if (provisionalText.isNotEmpty()) "$finalText $provisionalText…" else finalText
        transcriptView.text = display.trimStart()
    }

    // Colors
    private const val COLOR_NO_API_KEY = 0xFF9E9E9E.toInt()
    private const val COLOR_READY = 0xFF2196F3.toInt()
    private const val COLOR_CONNECTING = 0xFFFFC107.toInt()
    private const val COLOR_RECORDING = 0xFFF44336.toInt()

    private const val TAG_COLLAPSED = "collapsed"
    private const val TAG_EXPANDED = "expanded"
    private const val TAG_TRANSCRIPT = "transcript"
}
