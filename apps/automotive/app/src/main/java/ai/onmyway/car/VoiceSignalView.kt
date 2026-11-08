package ai.onmyway.car

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.view.View
import kotlin.math.sin

/** Compact speech indicator. Real microphone intensity is optional while waiting for SDK RMS. */
internal class VoiceSignalView(context: Context, color: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    private var running = false
    private var reactive = false
    private var level = 0f

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    fun showActivity(active: Boolean, microphone: Boolean = false) {
        if (running == active && reactive == microphone) return
        running = active
        reactive = microphone
        if (!active) level = 0f
        invalidate()
    }

    fun updateLevel(rms: Double) {
        if (rms.isFinite()) level = (rms.coerceIn(0.0, 12.0) / 12).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        val time = SystemClock.uptimeMillis() / 112.0
        val unit = width / 19f
        for (i in 0 until 9) {
            val movement = (sin(time + i * .74) * .5 + .5).toFloat()
            val strength = if (reactive) .12f + .78f * level else .62f
            val fraction = if (running) .13f + strength * movement else .15f
            val half = height * fraction / 2
            val x = unit * (1.5f + i * 2)
            paint.alpha = if (running) 255 else 110
            canvas.drawRoundRect(x - unit * .48f, height / 2f - half, x + unit * .48f, height / 2f + half,
                unit * .48f, unit * .48f, paint)
        }
        if (running && isShown) postInvalidateDelayed(34)
    }
}
