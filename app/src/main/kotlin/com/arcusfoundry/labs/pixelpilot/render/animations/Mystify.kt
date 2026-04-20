package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ColorUtil
import kotlin.random.Random

object MystifyAnimation : Animation {
    override val id = "mystify"
    override val displayName = "Mystify"
    override val category = "Classics"
    override val defaultBackground = Color.rgb(0, 0, 16)
    override val legacy = true

    private class Vert(var x: Float, var y: Float, var vx: Float, var vy: Float)
    private class Poly(var hue: Float, val verts: Array<Vert>)
    private class State(val polys: Array<Poly>, val fade: Paint, val stroke: Paint)

    override fun initialize(width: Int, height: Int, scale: Float): Any {
        val polys = Array(2) { idx ->
            Poly(
                hue = idx * 180f,
                verts = Array(4) {
                    Vert(
                        Random.nextFloat() * width,
                        Random.nextFloat() * height,
                        (Random.nextFloat() - 0.5f) * 4f,
                        (Random.nextFloat() - 0.5f) * 4f
                    )
                }
            )
        }
        val fade = Paint().apply { color = Color.argb(20, 0, 0, 16) }
        val stroke = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        return State(polys, fade, stroke)
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
        val baseHue = tintColor?.let { ColorUtil.hueOf(it) }
        val path = Path()
        for ((idx, p) in s.polys.withIndex()) {
            for (v in p.verts) {
                v.x += v.vx * speed
                v.y += v.vy * speed
                if (v.x <= 0 || v.x >= width) v.vx = -v.vx
                if (v.y <= 0 || v.y >= height) v.vy = -v.vy
                v.x = v.x.coerceIn(0f, width.toFloat())
                v.y = v.y.coerceIn(0f, height.toFloat())
            }
            p.hue = (p.hue + 0.4f) % 360f
            val hue = if (baseHue != null) (baseHue + idx * 40f + p.hue * 0.1f) % 360f else p.hue
            s.stroke.color = ColorUtil.hsl(hue, 0.9f, 0.65f)
            path.reset()
            path.moveTo(p.verts[0].x, p.verts[0].y)
            for (i in 1 until p.verts.size) path.lineTo(p.verts[i].x, p.verts[i].y)
            path.close()
            canvas.drawPath(path, s.stroke)
        }
    }
}
