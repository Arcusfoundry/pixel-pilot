package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ColorUtil
import kotlin.math.sin

object AuroraAnimation : Animation {
    override val id = "aurora"
    override val displayName = "Aurora"
    override val category = "Nature"
    override val defaultBackground = Color.rgb(10, 10, 46)

    private class State(val bg: Paint, val band: Paint)

    override fun initialize(width: Int, height: Int, scale: Float): Any {
        val bg = Paint().apply { color = defaultBackground }
        val band = Paint().apply { isAntiAlias = true }
        return State(bg, band)
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
        val time = timeMs * 0.0005f * speed
        val base = tintColor?.let { ColorUtil.hueOf(it) }
        val defaultColors = intArrayOf(
            Color.argb(20, 30, 200, 150),
            Color.argb(15, 100, 50, 200),
            Color.argb(12, 50, 255, 100)
        )
        val path = Path()
        for (band in 0 until 3) {
            path.reset()
            var first = true
            var x = 0
            while (x <= width) {
                val y = height * (0.3f + band * 0.1f) +
                    sin(x * 0.005f + time + band) * 60f * scale +
                    sin(x * 0.002f + time * 1.3f) * 30f * scale
                if (first) { path.moveTo(x.toFloat(), y); first = false } else path.lineTo(x.toFloat(), y)
                x += 4
            }
            path.lineTo(width.toFloat(), height.toFloat())
            path.lineTo(0f, height.toFloat())
            path.close()
            s.band.color = if (base != null) {
                ColorUtil.hsl((base + band * 30f) % 360f, 0.7f, 0.5f, 0.08f - band * 0.015f)
            } else defaultColors[band]
            canvas.drawPath(path, s.band)
        }
    }
}
