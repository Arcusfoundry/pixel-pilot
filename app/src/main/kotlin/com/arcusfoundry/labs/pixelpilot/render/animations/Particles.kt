package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ColorUtil
import kotlin.math.sqrt
import kotlin.random.Random

object ParticlesAnimation : Animation {
    override val id = "particles"
    override val displayName = "Particles"
    override val category = "Abstract"
    override val defaultBackground = Color.rgb(10, 10, 46)

    private class Pt(var x: Float, var y: Float, var vx: Float, var vy: Float, val r: Float)
    private class State(val pts: Array<Pt>, val bg: Paint, val dot: Paint, val line: Paint)

    override fun initialize(width: Int, height: Int, scale: Float): Any {
        val pts = Array(60) {
            Pt(
                Random.nextFloat() * width, Random.nextFloat() * height,
                (Random.nextFloat() - 0.5f) * 0.5f, (Random.nextFloat() - 0.5f) * 0.5f,
                Random.nextFloat() * 2f + 1f
            )
        }
        val bg = Paint().apply { color = defaultBackground }
        val dot = Paint().apply { isAntiAlias = true; color = Color.argb(128, 195, 217, 94) }
        val line = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true }
        return State(pts, bg, dot, line)
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
        val color = tintColor ?: Color.rgb(195, 217, 94)
        s.dot.color = ColorUtil.withAlpha(color, 0.5f)
        val linkDist = 120f * scale
        for (p in s.pts) {
            p.x += p.vx * speed
            p.y += p.vy * speed
            if (p.x < 0 || p.x > width) p.vx = -p.vx
            if (p.y < 0 || p.y > height) p.vy = -p.vy
            canvas.drawCircle(p.x, p.y, p.r * scale, s.dot)
        }
        for (i in s.pts.indices) {
            for (j in i + 1 until s.pts.size) {
                val dx = s.pts[i].x - s.pts[j].x
                val dy = s.pts[i].y - s.pts[j].y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < linkDist) {
                    s.line.color = ColorUtil.withAlpha(color, 0.15f * (1f - dist / linkDist))
                    canvas.drawLine(s.pts[i].x, s.pts[i].y, s.pts[j].x, s.pts[j].y, s.line)
                }
            }
        }
    }
}
