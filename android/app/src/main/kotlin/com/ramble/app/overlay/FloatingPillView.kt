package com.ramble.app.overlay

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
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

    fun updatePillState(pill: FrameLayout, state: PillState) {
        val background = pill.background as? GradientDrawable ?: return
        val icon = pill.getChildAt(0) as? ImageView ?: return

        val color = when (state) {
            PillState.NO_API_KEY -> COLOR_NO_API_KEY
            PillState.READY -> COLOR_READY
            PillState.CONNECTING -> COLOR_CONNECTING
            PillState.RECORDING -> COLOR_RECORDING
        }
        background.setColor(color)

        val iconRes = when (state) {
            PillState.RECORDING -> R.drawable.ic_stop
            else -> R.drawable.ic_mic
        }
        icon.setImageResource(iconRes)

        pill.alpha = when (state) {
            PillState.RECORDING -> 1.0f
            PillState.CONNECTING -> 0.9f
            else -> 0.7f
        }
    }

    // Colors
    private const val COLOR_NO_API_KEY = 0xFF9E9E9E.toInt()  // Gray
    private const val COLOR_READY = 0xFF2196F3.toInt()        // Blue
    private const val COLOR_CONNECTING = 0xFFFFC107.toInt()   // Amber
    private const val COLOR_RECORDING = 0xFFF44336.toInt()    // Red
}
