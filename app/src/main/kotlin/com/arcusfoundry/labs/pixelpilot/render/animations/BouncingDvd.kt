package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ColorUtil
import com.arcusfoundry.labs.pixelpilot.render.SceneConfig
import com.arcusfoundry.labs.pixelpilot.render.SettingSpec
import kotlin.math.abs
import kotlin.random.Random

object BouncingDvdAnimation : Animation {
    override val id = "bouncing-dvd"
    override val displayName = "Bouncing DVD"
    override val category = "Classics"
    override val defaultBackground = Color.BLACK
    override val legacy = true

    override val settings: List<SettingSpec> = listOf(
        SettingSpec.Text(
            key = "word",
            label = "Word",
            default = "DVD",
            maxLength = 16
        )
    )

    private class State(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        var hue: Float,
        val bg: Paint,
        val text: Paint,
        val small: Paint,
        val word: String
    )

    override fun initialize(width: Int, height: Int, scale: Float): Any =
        initialize(width, height, scale, SceneConfig.EMPTY)

    override fun initialize(width: Int, height: Int, scale: Float, config: SceneConfig): Any {
        val bg = Paint().apply { color = Color.BLACK }
        val text = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val small = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val word = config.string("word", "DVD").take(16)
        return State(
            x = Random.nextFloat() * (width - 200),
            y = Random.nextFloat() * (height - 100),
            vx = 1.6f, vy = 1.2f,
            hue = 60f, bg = bg, text = text, small = small,
            word = word
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
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), s.bg)
        val logoW = 200f * scale
        val logoH = 90f * scale
        s.x += s.vx * speed
        s.y += s.vy * speed
        var hit = false
        if (s.x <= 0) { s.x = 0f; s.vx = abs(s.vx); hit = true }
        if (s.x + logoW >= width) { s.x = width - logoW; s.vx = -abs(s.vx); hit = true }
        if (s.y <= 0) { s.y = 0f; s.vy = abs(s.vy); hit = true }
        if (s.y + logoH >= height) { s.y = height - logoH; s.vy = -abs(s.vy); hit = true }
        if (hit) s.hue = (s.hue + 47f) % 360f
        val baseHue = tintColor?.let { ColorUtil.hueOf(it) }
        val color = if (baseHue != null) ColorUtil.hsl((baseHue + s.hue * 0.15f) % 360f, 0.8f, 0.6f)
                    else ColorUtil.hsl(s.hue, 0.8f, 0.6f)
        s.text.color = color
        s.small.color = color
        s.text.textSize = logoH * 0.55f
        s.small.textSize = logoH * 0.22f
        val cx = s.x + logoW / 2f
        val cy = s.y + logoH / 2f
        canvas.drawText(s.word, cx, cy - 6f, s.text)
        canvas.drawText("VIDEO", cx, cy + logoH * 0.28f, s.small)
        if (hit) {
            s.text.setShadowLayer(30f, 0f, 0f, color)
            canvas.drawText(s.word, cx, cy - 6f, s.text)
            s.text.clearShadowLayer()
        }
    }
}
