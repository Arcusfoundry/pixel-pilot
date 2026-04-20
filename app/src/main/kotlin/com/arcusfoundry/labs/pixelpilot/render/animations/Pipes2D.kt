package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ColorUtil
import kotlin.random.Random

object Pipes2DAnimation : Animation {
    override val id = "pipes"
    override val displayName = "Pipes (2D)"
    override val category = "Classics"
    override val defaultBackground = Color.rgb(10, 10, 20)
    override val legacy = true

    private class Pipe(var x: Int, var y: Int, var dir: Int, var hue: Float, var len: Int)
    private class State(
        val cell: Int,
        val pipes: Array<Pipe>,
        var needsClear: Boolean,
        val line: Paint,
        val joint: Paint
    )

    override fun initialize(width: Int, height: Int, scale: Float): Any {
        val cell = 22
        val pipes = Array(4) {
            Pipe(
                x = (Random.nextInt(width) / cell) * cell,
                y = (Random.nextInt(height) / cell) * cell,
                dir = Random.nextInt(4),
                hue = Random.nextFloat() * 360f,
                len = 0
            )
        }
        val line = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        val joint = Paint().apply { isAntiAlias = true }
        return State(cell, pipes, true, line, joint)
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
        val baseHue = tintColor?.let { ColorUtil.hueOf(it) }
        val dxs = intArrayOf(s.cell, 0, -s.cell, 0)
        val dys = intArrayOf(0, s.cell, 0, -s.cell)
        for (p in s.pipes) {
            if (p.len > 2 && Random.nextFloat() < 0.18f) {
                val turn = if (Random.nextFloat() < 0.5f) -1 else 1
                p.dir = ((p.dir + turn) % 4 + 4) % 4
                p.len = 0
                p.hue = (p.hue + 8f) % 360f
            }
            val nx = p.x + dxs[p.dir]
            val ny = p.y + dys[p.dir]
            val hue = if (baseHue != null) (baseHue + p.hue * 0.15f) % 360f else p.hue
            s.line.color = ColorUtil.hsl(hue, 0.75f, 0.58f)
            s.joint.color = ColorUtil.hsl(hue, 0.9f, 0.75f)
            canvas.drawLine(
                p.x + s.cell / 2f, p.y + s.cell / 2f,
                nx + s.cell / 2f, ny + s.cell / 2f,
                s.line
            )
            canvas.drawCircle(nx + s.cell / 2f, ny + s.cell / 2f, 4f, s.joint)
            p.x = nx
            p.y = ny
            p.len += 1
            if (p.x < 0 || p.x > width || p.y < 0 || p.y > height) {
                p.x = (Random.nextInt(width) / s.cell) * s.cell
                p.y = (Random.nextInt(height) / s.cell) * s.cell
                p.dir = Random.nextInt(4)
                p.hue = Random.nextFloat() * 360f
                p.len = 0
            }
        }
    }
}
