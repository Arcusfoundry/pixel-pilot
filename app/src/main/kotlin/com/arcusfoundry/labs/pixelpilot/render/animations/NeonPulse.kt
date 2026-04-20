package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ColorUtil
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

object NeonPulseAnimation : Animation {
    override val id = "neon-pulse"
    override val displayName = "Neon Pulse"
    override val category = "Energy"
    override val defaultBackground = Color.rgb(10, 10, 10)

    private class State(val fade: Paint, val ring: Paint)

    override fun initialize(width: Int, height: Int, scale: Float): Any {
        val fade = Paint().apply { color = Color.argb(38, 10, 10, 10) }
        val ring = Paint().apply { style = Paint.Style.STROKE; isAntiAlias = true }
        return State(fade, ring)
    }

    override fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        state: Any,
        timeMs: Long,
        speed: Float,
        scale: Float,
        tintColor: Int?
    ) {
        val s = state as State
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), s.fade)
        val time = timeMs * 0.002f * speed
        val defaults = intArrayOf(
            Color.rgb(195, 217, 94),
            Color.rgb(255, 107, 107),
            Color.rgb(78, 205, 196),
            Color.rgb(168, 85, 247)
        )
        val base = tintColor?.let { ColorUtil.hueOf(it) }
        for (i in 0 until 4) {
            val phase = time + i * 1.5f
            val radius = abs((sin(phase) * 0.3f + 0.5f) * min(width, height) * 0.4f * scale)
            val alpha = (sin(phase) * 0.15f + 0.2f).coerceIn(0f, 1f)
            val color = if (base != null) ColorUtil.hsl((base + i * 30f) % 360f, 0.8f, 0.6f) else defaults[i]
            s.ring.color = ColorUtil.withAlpha(color, alpha)
            s.ring.strokeWidth = 2f * scale
            canvas.drawCircle(width / 2f, height / 2f, radius, s.ring)
        }
    }
}
