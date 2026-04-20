package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ColorUtil

object FloatingOrbsAnimation : Animation {
    override val id = "floating-orbs"
    override val displayName = "Floating Orbs"
    override val category = "Abstract"
    override val defaultBackground = Color.rgb(12, 12, 29)

    private class Orb(var x: Float, var y: Float, val r: Float, var vx: Float, var vy: Float, val color: Int)
    private class State(val orbs: Array<Orb>, val bg: Paint, val paint: Paint)

    override fun initialize(width: Int, height: Int, scale: Float): Any {
        val orbs = arrayOf(
            Orb(width * 0.2f, height * 0.3f, 150f, 0.15f, -0.2f, Color.argb(38, 195, 217, 94)),
            Orb(width * 0.7f, height * 0.6f, 125f, -0.12f, 0.18f, Color.argb(31, 168, 85, 247)),
            Orb(width * 0.5f, height * 0.2f, 100f, 0.2f, 0.1f, Color.argb(25, 78, 205, 196)),
            Orb(width * 0.3f, height * 0.7f, 140f, -0.1f, -0.15f, Color.argb(20, 255, 107, 107))
        )
        val bg = Paint().apply { color = defaultBackground }
        val paint = Paint().apply { isAntiAlias = true }
        return State(orbs, bg, paint)
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
        for ((idx, o) in s.orbs.withIndex()) {
            o.x += o.vx * speed
            o.y += o.vy * speed
            val r = o.r * scale
            if (o.x < 0 || o.x > width) o.vx = -o.vx
            if (o.y < 0 || o.y > height) o.vy = -o.vy
            val color = tintColor?.let {
                ColorUtil.withAlpha(ColorUtil.hsl((ColorUtil.hueOf(it) + idx * 30f) % 360f, 0.6f, 0.55f), 0.15f)
            } ?: o.color
            s.paint.shader = RadialGradient(o.x, o.y, r, color, Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawRect(o.x - r, o.y - r, o.x + r, o.y + r, s.paint)
            s.paint.shader = null
        }
    }
}
