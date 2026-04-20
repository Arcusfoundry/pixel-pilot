package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ColorUtil
import kotlin.random.Random

object BokehAnimation : Animation {
    override val id = "bokeh"
    override val displayName = "Bokeh"
    override val category = "Abstract"
    override val defaultBackground = Color.rgb(17, 0, 28)

    private class Orb(var x: Float, var y: Float, val r: Float, var vx: Float, var vy: Float, val hue: Float, val alpha: Float)
    private class State(val orbs: Array<Orb>, val bg: Paint, val paint: Paint)

    override fun initialize(width: Int, height: Int, scale: Float): Any {
        val hues = floatArrayOf(80f, 0f, 270f, 180f)
        val orbs = Array(15) {
            Orb(
                Random.nextFloat() * width, Random.nextFloat() * height,
                40f + Random.nextFloat() * 100f,
                (Random.nextFloat() - 0.5f) * 0.3f, (Random.nextFloat() - 0.5f) * 0.3f,
                hues[Random.nextInt(hues.size)],
                0.05f + Random.nextFloat() * 0.1f
            )
        }
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
        val baseHue = tintColor?.let { ColorUtil.hueOf(it) }
        for (o in s.orbs) {
            o.x += o.vx * speed
            o.y += o.vy * speed
            val r = o.r * scale
            if (o.x < -r || o.x > width + r) o.vx = -o.vx
            if (o.y < -r || o.y > height + r) o.vy = -o.vy
            val hue = baseHue?.let { (it + o.hue * 0.3f) % 360f } ?: o.hue
            s.paint.shader = RadialGradient(
                o.x, o.y, r,
                ColorUtil.hsl(hue, 0.7f, 0.6f, o.alpha),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(o.x - r, o.y - r, o.x + r, o.y + r, s.paint)
            s.paint.shader = null
        }
    }
}
