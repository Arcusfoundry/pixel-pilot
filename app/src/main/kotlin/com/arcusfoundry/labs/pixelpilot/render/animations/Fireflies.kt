package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ColorUtil
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object FirefliesAnimation : Animation {
    override val id = "fireflies"
    override val displayName = "Fireflies"
    override val category = "Nature"
    override val defaultBackground = Color.rgb(10, 18, 8)

    private class Fly(var x: Float, var y: Float, val phase: Float, val spd: Float)
    private class State(val flies: Array<Fly>, val fade: Paint, val glow: Paint)

    override fun initialize(width: Int, height: Int, scale: Float): Any {
        val flies = Array(40) {
            Fly(
                Random.nextFloat() * width, Random.nextFloat() * height,
                Random.nextFloat() * PI.toFloat() * 2f,
                Random.nextFloat() * 0.5f + 0.2f
            )
        }
        val fade = Paint().apply { color = Color.argb(26, 10, 18, 8) }
        val glow = Paint().apply { isAntiAlias = true }
        return State(flies, fade, glow)
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
        val time = timeMs * 0.001f * speed
        val base = tintColor ?: Color.rgb(255, 230, 100)
        for (f in s.flies) {
            f.x += sin(time * f.spd + f.phase) * 0.5f * speed
            f.y += cos(time * f.spd * 0.7f + f.phase) * 0.3f * speed
            if (f.x < 0) f.x = width.toFloat()
            if (f.x > width) f.x = 0f
            if (f.y < 0) f.y = height.toFloat()
            if (f.y > height) f.y = 0f
            val glow = (sin(time * 2f + f.phase) + 1f) * 0.5f
            val r = (3f + glow * 4f) * scale
            s.glow.shader = RadialGradient(
                f.x, f.y, r * 3f,
                ColorUtil.withAlpha(base, glow * 0.8f),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(f.x - r * 3f, f.y - r * 3f, f.x + r * 3f, f.y + r * 3f, s.glow)
        }
        s.glow.shader = null
    }
}
