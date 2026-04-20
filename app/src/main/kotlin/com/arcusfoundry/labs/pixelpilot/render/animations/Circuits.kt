package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ColorUtil
import kotlin.math.sin
import kotlin.random.Random

object CircuitsAnimation : Animation {
    override val id = "circuits"
    override val displayName = "Circuits"
    override val category = "Tech"
    override val defaultBackground = Color.rgb(13, 17, 23)

    private class State(var needsClear: Boolean, val bgFade: Paint, val node: Paint, val line: Paint)

    override fun initialize(width: Int, height: Int, scale: Float): Any {
        val fade = Paint().apply { color = Color.argb(12, 13, 17, 23) }
        val node = Paint().apply { isAntiAlias = true }
        val line = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true }
        return State(true, fade, node, line)
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
        if (s.needsClear) { canvas.drawColor(defaultBackground); s.needsClear = false }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), s.bgFade)
        val base = tintColor ?: Color.rgb(195, 217, 94)
        val time = timeMs * 0.001f * speed
        val spacing = 60f * scale
        s.line.color = ColorUtil.withAlpha(base, 0.15f)
        var x = spacing
        while (x < width) {
            var y = spacing
            while (y < height) {
                val pulse = (sin(time + x * 0.01f + y * 0.01f) + 1f) * 0.5f
                if (pulse > 0.7f) {
                    s.node.color = ColorUtil.withAlpha(base, pulse * 0.4f)
                    canvas.drawRect(x - 2f * scale, y - 2f * scale, x + 2f * scale, y + 2f * scale, s.node)
                    if (Random.nextFloat() > 0.98f) {
                        val dir = Random.nextInt(4)
                        val dx = floatArrayOf(spacing, 0f, -spacing, 0f)[dir]
                        val dy = floatArrayOf(0f, spacing, 0f, -spacing)[dir]
                        canvas.drawLine(x, y, x + dx, y + dy, s.line)
                    }
                }
                y += spacing
            }
            x += spacing
        }
    }
}
