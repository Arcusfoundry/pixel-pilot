package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.arcusfoundry.labs.pixelpilot.R
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.AssetLoader
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * After Dark classic. Uses the original 384x64 toaster sprite sheet (6 frames
 * for wing flap animation) and 64x64 toast sprite from Sparkpages/SparkforgeOS.
 */
object FlyingToastersAnimation : Animation {
    override val id = "flying-toasters"
    override val displayName = "Flying Toasters"
    override val category = "Classics"
    override val defaultBackground = Color.BLACK
    override val legacy = true

    private const val FRAME_SIZE = 64
    private const val FRAME_COUNT = 6

    private class Sprite(
        var x: Float, var y: Float,
        val kind: Kind,
        val depth: Float,
        val speedJitter: Float,
        var wingPhase: Float
    )
    private enum class Kind { TOASTER, TOAST }

    private class State(
        val sprites: MutableList<Sprite>,
        val bg: Paint,
        val spritePaint: Paint,
        val toaster: Bitmap?,
        val toast: Bitmap?,
        val frameRect: Rect,
        val destRect: RectF
    )

    private fun spawn(w: Int, h: Int, fromOffscreen: Boolean): Sprite {
        val roll = Random.nextFloat()
        val depth = when {
            roll < 0.35f -> 0.4f
            roll < 0.75f -> 0.7f
            else -> 1.0f
        }
        return Sprite(
            x = if (fromOffscreen) w + 64f + Random.nextFloat() * w * 0.4f
                else Random.nextFloat() * w * 1.2f - w * 0.1f,
            y = if (fromOffscreen) -64f - Random.nextFloat() * h * 0.4f
                else Random.nextFloat() * h * 1.2f - h * 0.2f,
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
        val spritePaint = Paint().apply {
            isAntiAlias = false
            isFilterBitmap = false // nearest-neighbor preserves pixel-art edges
        }
        val toasterBmp = AssetLoader.loadBitmap(R.drawable.toaster_sprite)
        val toastBmp = AssetLoader.loadBitmap(R.drawable.toast_sprite)
        return State(
            sprites = sprites,
            bg = bg,
            spritePaint = spritePaint,
            toaster = toasterBmp,
            toast = toastBmp,
            frameRect = Rect(),
            destRect = RectF()
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

        // Apply tint if provided — color-matrix multiply against tint channel.
        applyTintFilter(s.spritePaint, tintColor)

        // Back-to-front rendering for proper depth sort.
        val sorted = s.sprites.sortedBy { it.depth }
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
            s.spritePaint.alpha = (alpha * 255f).toInt()

            val half = drawSize / 2f
            s.destRect.set(sp.x - half, sp.y - half, sp.x + half, sp.y + half)

            if (sp.kind == Kind.TOASTER && s.toaster != null) {
                val frame = (sp.wingPhase * FRAME_COUNT).toInt().mod(FRAME_COUNT)
                s.frameRect.set(frame * FRAME_SIZE, 0, (frame + 1) * FRAME_SIZE, FRAME_SIZE)
                canvas.drawBitmap(s.toaster, s.frameRect, s.destRect, s.spritePaint)
            } else if (sp.kind == Kind.TOAST && s.toast != null) {
                s.frameRect.set(0, 0, FRAME_SIZE, FRAME_SIZE)
                canvas.drawBitmap(s.toast, s.frameRect, s.destRect, s.spritePaint)
            }
        }

        // Reset paint state.
        s.spritePaint.colorFilter = null
        s.spritePaint.alpha = 255
    }

    /**
     * Builds a color matrix that desaturates the sprite and multiplies by the
     * tint color. Gives a "red toaster" / "green toaster" effect matching how
     * tint works on procedural animations.
     */
    private fun applyTintFilter(paint: Paint, tintColor: Int?) {
        if (tintColor == null) {
            paint.colorFilter = null
            return
        }
        val tr = Color.red(tintColor) / 255f
        val tg = Color.green(tintColor) / 255f
        val tb = Color.blue(tintColor) / 255f
        val lr = 0.299f; val lg = 0.587f; val lb = 0.114f
        val strength = 0.7f
        val s = strength
        val inv = 1f - strength
        val m = ColorMatrix(floatArrayOf(
            inv + s * lr * tr,  s * lg * tr,        s * lb * tr,        0f, 0f,
            s * lr * tg,         inv + s * lg * tg, s * lb * tg,        0f, 0f,
            s * lr * tb,         s * lg * tb,        inv + s * lb * tb, 0f, 0f,
            0f,                  0f,                 0f,                 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(m)
    }
}
