package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ColorUtil
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object Pipes3DAnimation : Animation {
    override val id = "pipes-3d"
    override val displayName = "3D Pipes"
    override val category = "Classics"
    override val defaultBackground = Color.rgb(5, 5, 15)
    override val legacy = true

    private class Pipe(var x: Int, var y: Int, var z: Int, var dir: Int, var hue: Float, var len: Int)
    private class State(
        val gridSize: Int,
        val pipes: Array<Pipe>,
        var needsClear: Boolean,
        val fade: Paint,
        val line: Paint,
        val joint: Paint
    )

    override fun initialize(width: Int, height: Int, scale: Float): Any {
        val gs = 14
        val pipes = Array(4) {
            Pipe(
                Random.nextInt(gs), Random.nextInt(gs), Random.nextInt(gs),
                Random.nextInt(6), Random.nextFloat() * 360f, 0
            )
        }
        val fade = Paint().apply { color = Color.argb(10, 5, 5, 15) }
        val line = Paint().apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        val joint = Paint().apply { isAntiAlias = true }
        return State(gs, pipes, true, fade, line, joint)
    }

    private data class Proj(val sx: Float, val sy: Float, val depth: Float)

    private fun project(gs: Int, cell: Float, cx: Float, cy: Float, gx: Int, gy: Int, gz: Int): Proj {
        val x = gx - gs / 2f
        val y = gy - gs / 2f
        val z = gz - gs / 2f
        return Proj(
            sx = cx + (x - z) * cell * 0.866f,
            sy = cy + (x + z) * cell * 0.5f - y * cell,
            depth = (x + z) / gs + 0.5f
        )
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
        if (s.needsClear) {
            canvas.drawColor(defaultBackground)
            s.needsClear = false
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), s.fade)
        val gs = s.gridSize
        val cell = min(width, height) / (gs * 1.6f)
        val cx = width / 2f
        val cy = height / 2f
        val baseHue = tintColor?.let { ColorUtil.hueOf(it) }
        val dxs = intArrayOf(1, -1, 0, 0, 0, 0)
        val dys = intArrayOf(0, 0, 1, -1, 0, 0)
        val dzs = intArrayOf(0, 0, 0, 0, 1, -1)
        for (p in s.pipes) {
            if (p.len > 1 && Random.nextFloat() < 0.22f) {
                var nd: Int
                do { nd = Random.nextInt(6) } while (nd == (p.dir xor 1))
                p.dir = nd
                p.len = 0
                val j = project(gs, cell, cx, cy, p.x, p.y, p.z)
                val r = max(2f, 6f * (0.5f + j.depth * 0.6f))
                val hue = if (baseHue != null) (baseHue + p.hue * 0.2f) % 360f else p.hue
                val shader = RadialGradient(
                    j.sx - r * 0.3f, j.sy - r * 0.3f, r,
                    ColorUtil.hsl(hue, 0.8f, 0.8f),
                    ColorUtil.hsl(hue, 0.6f, 0.35f),
                    Shader.TileMode.CLAMP
                )
                s.joint.shader = shader
                canvas.drawCircle(j.sx, j.sy, r, s.joint)
                s.joint.shader = null
            }
            val nx = p.x + dxs[p.dir]
            val ny = p.y + dys[p.dir]
            val nz = p.z + dzs[p.dir]
            if (nx !in 0 until gs || ny !in 0 until gs || nz !in 0 until gs) {
                p.x = Random.nextInt(gs); p.y = Random.nextInt(gs); p.z = Random.nextInt(gs)
                p.dir = Random.nextInt(6); p.hue = (p.hue + 33f) % 360f; p.len = 0
                continue
            }
            val a = project(gs, cell, cx, cy, p.x, p.y, p.z)
            val b = project(gs, cell, cx, cy, nx, ny, nz)
            val widthPx = max(2f, 7f * (0.4f + ((a.depth + b.depth) / 2f) * 0.8f))
            val hue = if (baseHue != null) (baseHue + p.hue * 0.2f) % 360f else p.hue
            s.line.color = ColorUtil.hsl(hue, 0.75f, 0.58f)
            s.line.strokeWidth = widthPx
            canvas.drawLine(a.sx, a.sy, b.sx, b.sy, s.line)
            p.x = nx; p.y = ny; p.z = nz
            p.len += 1
        }
    }
}
