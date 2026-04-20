package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ColorUtil
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

object FlyingToastersAnimation : Animation {
    override val id = "flying-toasters"
    override val displayName = "Flying Toasters"
    override val category = "Classics"
    override val defaultBackground = Color.BLACK
    override val legacy = true

    private class Sprite(
        var x: Float, var y: Float,
        val kind: Kind,
        val depth: Float,
        val speedJitter: Float,
        var wingPhase: Float
    )
    private enum class Kind { TOASTER, TOAST }
    private class State(val sprites: MutableList<Sprite>, val bg: Paint, val body: Paint, val wing: Paint, val detail: Paint, val toast: Paint, val crust: Paint)

    private fun spawn(w: Int, h: Int, fromOffscreen: Boolean): Sprite {
        val roll = Random.nextFloat()
        val depth = when {
            roll < 0.35f -> 0.4f
            roll < 0.75f -> 0.7f
            else -> 1.0f
        }
        return Sprite(
            x = if (fromOffscreen) w + 64f + Random.nextFloat() * w * 0.4f else Random.nextFloat() * w * 1.2f - w * 0.1f,
            y = if (fromOffscreen) -64f - Random.nextFloat() * h * 0.4f else Random.nextFloat() * h * 1.2f - h * 0.2f,
            kind = if (Random.nextFloat() < 0.78f) Kind.TOASTER else Kind.TOAST,
            depth = depth,
            speedJitter = 0.85f + Random.nextFloat() * 0.3f,
            wingPhase = Random.nextFloat()
        )
    }

    override fun initialize(width: Int, height: Int, scale: Float): Any {
        val count = max(6, min(14, (width * height) / 60000))
        val sprites = MutableList(count) { spawn(width, height, false) }
        val bg = Paint().apply { color = Color.BLACK }
        val body = Paint().apply { isAntiAlias = true }
        val wing = Paint().apply { isAntiAlias = true; color = Color.argb(230, 220, 220, 230) }
        val detail = Paint().apply { isAntiAlias = true; color = Color.argb(255, 70, 70, 80); style = Paint.Style.STROKE; strokeWidth = 2f }
        val toast = Paint().apply { isAntiAlias = true; color = Color.rgb(210, 160, 90) }
        val crust = Paint().apply { isAntiAlias = true; color = Color.rgb(140, 90, 40) }
        return State(sprites, bg, body, wing, detail, toast, crust)
    }

    private fun drawToaster(canvas: Canvas, cx: Float, cy: Float, size: Float, wingPhase: Float, bodyColor: Int, state: State) {
        val half = size / 2f
        state.body.color = bodyColor
        val rect = RectF(cx - half, cy - half * 0.75f, cx + half, cy + half * 0.75f)
        canvas.drawRoundRect(rect, size * 0.12f, size * 0.12f, state.body)
        val slot = RectF(cx - half * 0.6f, cy - half * 0.65f, cx + half * 0.6f, cy - half * 0.25f)
        canvas.drawRect(slot, state.detail)
        canvas.drawCircle(cx + half * 0.75f, cy + half * 0.3f, size * 0.07f, state.detail)
        val flap = sin(wingPhase * PI.toFloat() * 2f)
        val wingLen = size * 0.9f
        val wingH = size * 0.4f * (0.7f + flap * 0.5f)
        val leftWing = Path().apply {
            moveTo(cx - half, cy)
            lineTo(cx - half - wingLen, cy - wingH / 2f)
            lineTo(cx - half - wingLen * 0.9f, cy + wingH / 2f)
            close()
        }
        val rightWing = Path().apply {
            moveTo(cx + half, cy)
            lineTo(cx + half + wingLen, cy - wingH / 2f)
            lineTo(cx + half + wingLen * 0.9f, cy + wingH / 2f)
            close()
        }
        canvas.drawPath(leftWing, state.wing)
        canvas.drawPath(rightWing, state.wing)
    }

    private fun drawToast(canvas: Canvas, cx: Float, cy: Float, size: Float, state: State) {
        val half = size / 2f
        val rect = RectF(cx - half * 0.75f, cy - half * 0.55f, cx + half * 0.75f, cy + half * 0.55f)
        canvas.drawRoundRect(rect, size * 0.18f, size * 0.18f, state.crust)
        val inner = RectF(rect.left + size * 0.08f, rect.top + size * 0.08f, rect.right - size * 0.08f, rect.bottom - size * 0.08f)
        canvas.drawRoundRect(inner, size * 0.12f, size * 0.12f, state.toast)
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
        val sorted = s.sprites.sortedBy { it.depth }
        val bodyColor = tintColor ?: Color.rgb(192, 192, 210)
        for (sp in sorted) {
            val baseSpeed = (1.4f + sp.depth * 1.6f) * sp.speedJitter * speed
            sp.x -= baseSpeed * 1.4f
            sp.y += baseSpeed * 1.0f
            sp.wingPhase += 0.12f * speed
            val drawSize = 64f * scale * (0.7f + sp.depth * 1.1f)
            if (sp.x < -drawSize || sp.y > height + drawSize) {
                val fresh = spawn(width, height, true)
                sp.x = fresh.x; sp.y = fresh.y
                sp.wingPhase = fresh.wingPhase
            }
            val alpha = (0.65f + sp.depth * 0.35f).coerceIn(0f, 1f)
            val colorWithAlpha = ColorUtil.withAlpha(bodyColor, alpha)
            if (sp.kind == Kind.TOASTER) {
                drawToaster(canvas, sp.x, sp.y, drawSize, sp.wingPhase, colorWithAlpha, s)
            } else {
                drawToast(canvas, sp.x, sp.y, drawSize, s)
            }
        }
    }
}
