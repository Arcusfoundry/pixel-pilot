package com.arcusfoundry.labs.pixelpilot.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.Surface
import com.arcusfoundry.labs.pixelpilot.R

/**
 * Renders a static welcome image (logo + brief instruction text) instead of
 * an animated scene. Used when the wallpaper engine is running in
 * system-picker preview mode, so the user sees a clear "what to do" screen
 * instead of a teaser of the actual scene.
 */
class InstructionRenderer(private val context: Context) : WallpaperRenderer {

    private var surface: Surface? = null
    private var width = 0
    private var height = 0
    private var logoBitmap: Bitmap? = null

    override fun attach(surface: Surface, width: Int, height: Int) {
        this.surface = surface
        this.width = width
        this.height = height
        if (logoBitmap == null) {
            logoBitmap = runCatching {
                BitmapFactory.decodeResource(context.resources, R.drawable.pp_logo)
            }.getOrNull()
        }
        renderOnce()
    }

    override fun setVisible(visible: Boolean) {
        if (visible) renderOnce()
    }

    override fun updateParams(params: RenderParams) { /* static */ }

    override fun detach() {
        surface = null
        logoBitmap?.recycle()
        logoBitmap = null
    }

    override fun release() { detach() }

    private fun renderOnce() {
        val s = surface ?: return
        if (!s.isValid) return
        val canvas = try { s.lockCanvas(null) } catch (_: Exception) { null } ?: return
        try {
            canvas.drawColor(Color.rgb(12, 12, 14))

            val logo = logoBitmap
            // Logo target: 55% of the shorter dimension so it scales
            // gracefully across phone, foldable inner, foldable cover.
            val shortDim = minOf(width, height)
            val logoTarget = (shortDim * 0.55f).toInt().coerceAtLeast(120)
            val logoLeft = (width - logoTarget) / 2f
            val logoTop = (height / 2f) - logoTarget * 0.85f
            if (logo != null) {
                val src = Rect(0, 0, logo.width, logo.height)
                val dst = RectF(logoLeft, logoTop, logoLeft + logoTarget, logoTop + logoTarget)
                // Grayscale + slight darken so the logo doesn't leak its
                // saturated green into the system's lock-screen color
                // sampling. The wallpaper's dominant pixel color drives PIN
                // screen tint, AOD accents, etc., so we keep the rendered
                // surface chromatically neutral during preview.
                val matrix = ColorMatrix().apply { setSaturation(0f) }
                val paint = Paint().apply {
                    isFilterBitmap = true
                    isAntiAlias = true
                    colorFilter = ColorMatrixColorFilter(matrix)
                }
                canvas.drawBitmap(logo, src, dst, paint)
            }

            val bodySize = (width * 0.038f).coerceIn(20f, 56f)
            val bodyPaint = Paint().apply {
                color = Color.argb(220, 255, 255, 255)
                textSize = bodySize
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val cx = width / 2f
            val firstLineY = logoTop + logoTarget + bodySize * 1.6f
            canvas.drawText("Tap 'Set wallpaper' to begin", cx, firstLineY, bodyPaint)
            canvas.drawText(
                "Choose Both — home and lock screen",
                cx,
                firstLineY + bodySize * 1.6f,
                bodyPaint
            )
        } finally {
            try { s.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
        }
    }
}
