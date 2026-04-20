package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ColorUtil
import kotlin.random.Random

object CyberRainAnimation : Animation {
    override val id = "cyber-rain"
    override val displayName = "Cyber Rain"
    override val category = "Tech"
    override val defaultBackground = Color.rgb(0, 0, 17)

    private class Drop(var x: Float, var y: Float, val spd: Float, val length: Float)
    private class State(val drops: Array<Drop>, val bgFade: Paint, val line: Paint)

    override fun initialize(width: Int, height: Int, scale: Float): Any {
        val drops = Array(100) {
            Drop(
                Random.nextFloat() * width, Random.nextFloat() * height,
                1f + Random.nextFloat() * 3f,
                20f + Random.nextFloat() * 60f
            )
        }
        val fade = Paint().apply { color = Color.argb(26, 0, 0, 17) }
        val line = Paint().apply { style = Paint.Style.STROKE; isAntiAlias = true }
        return State(drops, fade, line)
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
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), s.bgFade)
        val base = tintColor ?: Color.rgb(0, 150, 255)
        s.line.strokeWidth = 1f * scale
        for (d in s.drops) {
            val len = d.length * scale
            s.line.shader = LinearGradient(
                d.x, d.y, d.x, d.y + len,
                ColorUtil.withAlpha(base, 0f),
                ColorUtil.withAlpha(base, 0.4f),
                Shader.TileMode.CLAMP
            )
            canvas.drawLine(d.x, d.y, d.x, d.y + len, s.line)
            d.y += d.spd * speed
            if (d.y > height) { d.y = -len; d.x = Random.nextFloat() * width }
        }
        s.line.shader = null
    }
}
