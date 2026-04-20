package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.arcusfoundry.labs.pixelpilot.render.Animation
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round
import kotlin.random.Random

object MatrixRainAnimation : Animation {
    override val id = "matrix"
    override val displayName = "Matrix Rain"
    override val category = "Tech"
    override val defaultBackground = Color.BLACK

    private const val CHARS = "アイウエオカキクケコサシスセソタチツテトナニヌネノ0123456789ABCDEF"

    private class Col(var pos: Float, var lastRow: Int)

    private class State(
        val fontSize: Int,
        val colGap: Int,
        val rowStep: Int,
        val cols: Array<Col>,
        val paintFade: Paint,
        val paintChar: Paint
    )

    override fun initialize(width: Int, height: Int, scale: Float): Any {
        val fontSize = max(8, round(14f * scale).toInt())
        val colGap = round(fontSize * 1.8f).toInt()
        val colCount = max(1, width / colGap)
        val rowStep = round(fontSize * 3f).toInt()
        val cols = Array(colCount) { Col(Random.nextFloat() * -20f, -999) }
        val fade = Paint().apply {
            color = Color.argb(6, 0, 0, 0)
            style = Paint.Style.FILL
        }
        val charPaint = Paint().apply {
            isAntiAlias = true
            textSize = fontSize.toFloat()
            typeface = Typeface.MONOSPACE
        }
        return State(fontSize, colGap, rowStep, cols, fade, charPaint)
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
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), s.paintFade)
        s.paintChar.color = tintColor ?: Color.rgb(191, 255, 192)
        for (i in s.cols.indices) {
            val col = s.cols[i]
            col.pos += 0.15f * speed
            val row = floor(col.pos).toInt()
            if (row != col.lastRow) {
                col.lastRow = row
                val x = i * s.colGap + (s.colGap - s.fontSize) / 2f
                val y = (row * s.rowStep).toFloat()
                if (y > 0f && y < height + s.rowStep) {
                    val ch = CHARS[Random.nextInt(CHARS.length)]
                    canvas.drawText(ch.toString(), x, y, s.paintChar)
                }
            }
            if (col.pos * s.rowStep > height + s.rowStep) {
                col.pos = -Random.nextFloat() * (height.toFloat() / s.rowStep) - 1f
                col.lastRow = -999
            }
        }
    }
}
