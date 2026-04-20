package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ColorUtil
import kotlin.math.max

object GraphPaperAnimation : Animation {
    override val id = "graph-paper"
    override val displayName = "Graph Paper"
    override val category = "Default"
    override val defaultBackground = Color.rgb(26, 28, 10)

    private class State(val bg: Paint, val line: Paint)

    override fun initialize(width: Int, height: Int, scale: Float): Any {
        val bg = Paint().apply { color = defaultBackground }
        val line = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.argb(10, 195, 217, 94)
        }
        return State(bg, line)
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
        s.line.color = tintColor?.let { ColorUtil.withAlpha(it, 0.08f) } ?: Color.argb(10, 195, 217, 94)
        val spacing = max(12f, 40f * scale)
        var x = 0f
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), s.line)
            x += spacing
        }
        var y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, s.line)
            y += spacing
        }
    }
}
