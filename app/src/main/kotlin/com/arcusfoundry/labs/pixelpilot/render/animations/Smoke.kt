package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ColorUtil
import kotlin.random.Random

object SmokeAnimation : Animation {
    override val id = "smoke"
    override val displayName = "Smoke"
    override val category = "Energy"
    override val defaultBackground = Color.rgb(17, 17, 17)

    private class Puff(var x: Float, var y: Float, val r: Float, var vx: Float, var vy: Float)
    private class State(val puffs: Array<Puff>, val bg: Paint, val paint: Paint)

    override fun initialize(width: Int, height: Int, scale: Float): Any {
        val puffs = Array(8) {
            Puff(
                Random.nextFloat() * width, Random.nextFloat() * height,
                80f + Random.nextFloat() * 120f,
                (Random.nextFloat() - 0.5f) * 0.2f,
                (Random.nextFloat() - 0.5f) * 0.15f
            )
        }
        val bg = Paint().apply { color = defaultBackground }
        val paint = Paint().apply { isAntiAlias = true }
        return State(puffs, bg, paint)
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
        val base = tintColor ?: Color.WHITE
        for (p in s.puffs) {
            p.x += p.vx * speed
            p.y += p.vy * speed
            val r = p.r * scale
            if (p.x < -r || p.x > width + r) p.vx = -p.vx
            if (p.y < -r || p.y > height + r) p.vy = -p.vy
            s.paint.shader = RadialGradient(
                p.x, p.y, r,
                ColorUtil.withAlpha(base, 0.03f),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(p.x - r, p.y - r, p.x + r, p.y + r, s.paint)
        }
        s.paint.shader = null
    }
}
