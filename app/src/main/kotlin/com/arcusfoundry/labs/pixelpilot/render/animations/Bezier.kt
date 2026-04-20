package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ColorUtil
import kotlin.random.Random

object BezierAnimation : Animation {
    override val id = "bezier"
    override val displayName = "Bezier"
    override val category = "Classics"
    override val defaultBackground = Color.rgb(0, 0, 24)
    override val legacy = true

    private class Pt(var x: Float, var y: Float, var vx: Float, var vy: Float)
    private class Curve(var hue: Float, val pts: Array<Pt>)
    private class State(val curves: Array<Curve>, val fade: Paint, val stroke: Paint)

    override fun initialize(width: Int, height: Int, scale: Float): Any {
        val curves = Array(3) { i ->
            Curve(
                hue = (i * 120f) % 360f,
                pts = Array(4) {
                    Pt(
                        Random.nextFloat() * width,
                        Random.nextFloat() * height,
                        (Random.nextFloat() - 0.5f) * 3.5f,
                        (Random.nextFloat() - 0.5f) * 3.5f
                    )
                }
            )
        }
        val fade = Paint().apply { color = Color.argb(12, 0, 0, 24) }
        val stroke = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        return State(curves, fade, stroke)
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
        for ((idx, c) in s.curves.withIndex()) {
            for (p in c.pts) {
                p.x += p.vx * speed
                p.y += p.vy * speed
                if (p.x <= 0 || p.x >= width) p.vx = -p.vx
                if (p.y <= 0 || p.y >= height) p.vy = -p.vy
                p.x = p.x.coerceIn(0f, width.toFloat())
                p.y = p.y.coerceIn(0f, height.toFloat())
            }
            c.hue = (c.hue + 0.5f) % 360f
            val hue = if (baseHue != null) (baseHue + idx * 45f) % 360f else c.hue
            s.stroke.color = ColorUtil.hsl(hue, 0.85f, 0.6f)
            path.reset()
            path.moveTo(c.pts[0].x, c.pts[0].y)
            path.cubicTo(c.pts[1].x, c.pts[1].y, c.pts[2].x, c.pts[2].y, c.pts[3].x, c.pts[3].y)
            canvas.drawPath(path, s.stroke)
        }
    }
}
