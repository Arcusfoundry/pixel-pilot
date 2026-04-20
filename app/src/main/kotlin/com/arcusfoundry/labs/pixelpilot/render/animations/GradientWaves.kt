package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ColorUtil
import kotlin.math.sin

object GradientWavesAnimation : Animation {
    override val id = "gradient-waves"
    override val displayName = "Gradient Waves"
    override val category = "Abstract"
    override val defaultBackground = Color.rgb(102, 126, 234)

    private class State(val bg: Paint, val wave: Paint)

    override fun initialize(width: Int, height: Int, scale: Float): Any {
        val bg = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                Color.rgb(102, 126, 234), Color.rgb(118, 75, 162),
                Shader.TileMode.CLAMP
            )
        }
        val wave = Paint().apply { isAntiAlias = true; color = Color.argb(38, 255, 255, 255) }
        return State(bg, wave)
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
        if (tintColor != null) {
            s.bg.shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                ColorUtil.hsl((ColorUtil.hueOf(tintColor) + 0f), 0.6f, 0.6f),
                ColorUtil.hsl((ColorUtil.hueOf(tintColor) + 40f), 0.6f, 0.45f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), s.bg)
        val time = timeMs * 0.001f * speed
        val path = Path()
        for (i in 0 until 5) {
            path.reset()
            var first = true
            var x = 0
            while (x <= width) {
                val y = height / 2f +
                    sin(x * 0.008f + time + i * 1.2f) * (80f + i * 30f) * scale +
                    sin(x * 0.003f + time * 0.7f) * 40f * scale
                if (first) { path.moveTo(x.toFloat(), y); first = false } else path.lineTo(x.toFloat(), y)
                x += 4
            }
            path.lineTo(width.toFloat(), height.toFloat())
            path.lineTo(0f, height.toFloat())
            path.close()
            canvas.drawPath(path, s.wave)
        }
    }
}
