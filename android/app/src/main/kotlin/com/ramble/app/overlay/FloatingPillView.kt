package com.ramble.app.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
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
        val widthPx = dpToPx(context, 48)
        val heightPx = dpToPx(context, 72)
        val cornerRadius = dpToPx(context, 24).toFloat()

        val pill = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(widthPx, heightPx)
        }

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

        val iconSize = dpToPx(context, 28)
        val icon = ImageView(context).apply {
            setImageResource(R.drawable.ic_mic)
            setColorFilter(0xFFFFFFFF.toInt())
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.CENTER
            }
        }
        pill.addView(icon)

        pill.alpha = 0.7f
        pill.elevation = dpToPx(context, 6).toFloat()

        return pill
    }

    private var pulseAnimator: ValueAnimator? = null

    fun updatePillState(pill: FrameLayout, state: PillState) {
        val background = pill.background as? GradientDrawable ?: return
        val icon = pill.getChildAt(0) as? ImageView ?: return

        // Stop any existing pulse animation
        pulseAnimator?.cancel()
        pulseAnimator = null

        if (state == PillState.RECORDING) {
            // Gradient background for recording
            background.colors = intArrayOf(COLOR_READY, COLOR_RECORDING)
            background.orientation = GradientDrawable.Orientation.TL_BR
            // Breathing pulse animation
            pulseAnimator = ValueAnimator.ofFloat(0.85f, 1.0f).apply {
                duration = 1500
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { pill.alpha = it.animatedValue as Float }
                start()
            }
        } else {
            val color = when (state) {
                PillState.NO_API_KEY -> COLOR_NO_API_KEY
                PillState.READY -> COLOR_READY
                PillState.CONNECTING -> COLOR_CONNECTING
                else -> COLOR_READY
            }
            background.colors = null
            background.setColor(color)
            pill.alpha = when (state) {
                PillState.CONNECTING -> 0.9f
                else -> 0.7f
            }
        }

        val iconRes = when (state) {
            PillState.RECORDING -> R.drawable.ic_stop
            else -> R.drawable.ic_mic
        }
        icon.setImageResource(iconRes)
    }

    // Unified brand colors
    private const val COLOR_NO_API_KEY = 0xFF8888A0.toInt()   // Muted
    private const val COLOR_READY = 0xFF0066FF.toInt()        // Blue accent
    private const val COLOR_CONNECTING = 0xFFFFAA00.toInt()   // Amber
    private const val COLOR_RECORDING = 0xFF9933FF.toInt()    // Purple accent
}
