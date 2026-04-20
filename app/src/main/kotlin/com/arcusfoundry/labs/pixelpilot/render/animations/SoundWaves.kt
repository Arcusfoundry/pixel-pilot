package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ColorUtil
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sin

object SoundWavesAnimation : Animation {
    override val id = "sound-waves"
    override val displayName = "Sound Waves"
    override val category = "Energy"
    override val defaultBackground = Color.rgb(26, 10, 46)

    private class State(val bg: Paint, val bar: Paint)

    override fun initialize(width: Int, height: Int, scale: Float): Any {
        val bg = Paint().apply { color = defaultBackground }
        val bar = Paint().apply { isAntiAlias = true }
        return State(bg, bar)
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
        val time = timeMs * 0.002f * speed
        val bars = max(8, round(40f / scale).toInt())
        val barW = width.toFloat() / bars
        val base = tintColor ?: Color.rgb(195, 217, 94)
        for (i in 0 until bars) {
            val h = (sin(time + i * 0.3f) * 0.3f + 0.5f) * height * 0.4f * scale
            val alpha = (0.2f + sin(time + i * 0.2f) * 0.15f).coerceIn(0f, 1f)
            s.bar.color = ColorUtil.withAlpha(base, alpha)
            canvas.drawRect(i * barW + 2f, height / 2f - h / 2f, (i + 1) * barW - 2f, height / 2f + h / 2f, s.bar)
        }
    }
}
