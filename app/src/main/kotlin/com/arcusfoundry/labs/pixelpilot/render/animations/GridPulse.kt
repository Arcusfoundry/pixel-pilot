package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ColorUtil
import kotlin.math.max

object GridPulseAnimation : Animation {
    override val id = "grid-pulse"
    override val displayName = "Grid Pulse"
    override val category = "Tech"
    override val defaultBackground = Color.rgb(10, 10, 26)

    private class State(val bg: Paint, val grid: Paint, val pulse: Paint)

    override fun initialize(width: Int, height: Int, scale: Float): Any {
        val bg = Paint().apply { color = defaultBackground }
        val grid = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f }
        val pulse = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true }
        return State(bg, grid, pulse)
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
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), s.bg)
        val base = tintColor ?: Color.rgb(195, 217, 94)
        s.grid.color = ColorUtil.withAlpha(base, 0.08f)
        val time = timeMs * 0.001f * speed
        val spacing = 40f * scale
        var x = 0f
        while (x < width) { canvas.drawLine(x, 0f, x, height.toFloat(), s.grid); x += spacing }
        var y = 0f
        while (y < height) { canvas.drawLine(0f, y, width.toFloat(), y, s.grid); y += spacing }
        for (i in 0 until 3) {
            val phase = (time + i * 2f) % 6f
            val radius = phase * max(width, height) * 0.2f * scale
            val alpha = max(0f, 0.3f - phase * 0.05f)
            s.pulse.color = ColorUtil.withAlpha(base, alpha)
            canvas.drawCircle(width / 2f, height / 2f, radius, s.pulse)
        }
    }
}
