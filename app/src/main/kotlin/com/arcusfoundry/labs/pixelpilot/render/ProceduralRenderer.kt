package com.arcusfoundry.labs.pixelpilot.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.util.Log
import android.view.Choreographer
import android.view.Surface

/**
 * Runs an Animation and writes its frames to the wallpaper Surface.
 *
 * Wallpaper Surface buffers are NOT guaranteed to preserve prior frame contents
 * between lockCanvas() calls (triple-buffering cycles through distinct buffers).
 * Animations like Matrix Rain, Mystify, Pipes rely on frame accumulation via
 * a subtle fade overlay to produce trails — they only work correctly when the
 * canvas contents persist.
 *
 * The fix: maintain a persistent Bitmap back-buffer. The animation draws into
 * the back-buffer (contents preserved across frames). Each frame we blit the
 * back-buffer to the Surface in one shot. Accumulation effects now work.
 */
class ProceduralRenderer(private val animation: Animation) : WallpaperRenderer {

    private var surface: Surface? = null
    private var width = 0
    private var height = 0
    private var state: Any? = null
    private var params = RenderParams.DEFAULTS
    private var running = false
    private var visible = false
    private val startTime = SystemClock.uptimeMillis()
    private var lastInitScale: Float = 1f

    private var backBuffer: Bitmap? = null
    private var backCanvas: Canvas? = null

    private val dimPaint = Paint()
    private val blitPaint: Paint? = null

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running || !visible) return
            renderFrame()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun attach(surface: Surface, width: Int, height: Int) {
        this.surface = surface
        this.width = width
        this.height = height
        this.lastInitScale = params.scale

        // Back-buffer preserves contents across Surface frame buffer swaps.
        val bb = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val bc = Canvas(bb)
        bc.drawColor(animation.defaultBackground)
        backBuffer = bb
        backCanvas = bc

        // Prime the output Surface immediately with the default background so the
        // system wallpaper picker preview isn't black for the first ~frame before
        // Choreographer fires. Rendered frames overwrite this.
        if (surface.isValid) {
            val primeCanvas = try { surface.lockCanvas(null) } catch (_: Exception) { null }
            if (primeCanvas != null) {
                primeCanvas.drawColor(animation.defaultBackground)
                try { surface.unlockCanvasAndPost(primeCanvas) } catch (_: Exception) {}
            }
        }

        state = animation.initialize(width, height, params.scale)
        running = true
        if (visible) Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun setVisible(visible: Boolean) {
        val wasVisible = this.visible
        this.visible = visible
        if (visible && !wasVisible && running) {
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    override fun updateParams(params: RenderParams) {
        this.params = params
        if (kotlin.math.abs(params.scale - lastInitScale) > 0.15f && running) {
            lastInitScale = params.scale
            state = animation.initialize(width, height, params.scale)
            // Scale change usually means a visual reset; clear back-buffer to bg.
            backCanvas?.drawColor(animation.defaultBackground)
        }
    }

    override fun detach() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        surface = null
        state = null
        backBuffer?.recycle()
        backBuffer = null
        backCanvas = null
    }

    override fun release() {
        detach()
    }

    private fun effectiveTintColor(): Int? = when (val mode = params.tint) {
        TintMode.None -> null
        is TintMode.Static -> mode.color
        is TintMode.Rainbow -> {
            val cycleMs = (mode.cycleSeconds * 1000f).coerceAtLeast(100f)
            val phase = ((SystemClock.uptimeMillis() - startTime) % cycleMs.toLong()) / cycleMs
            ColorUtil.hsl(phase * 360f, 0.7f, 0.55f)
        }
    }

    private fun renderFrame() {
        val surf = surface ?: return
        val bb = backBuffer ?: return
        val bc = backCanvas ?: return
        val s = state ?: return
        if (!surf.isValid) return

        try {
            val t = SystemClock.uptimeMillis() - startTime
            val tint = effectiveTintColor()

            // 1. Animation draws into the preserved back-buffer.
            animation.draw(bc, width, height, s, t, params.speed, params.scale, tint)

            // 2. Dim overlay applied to back-buffer so it doesn't stack with prior dim layers.
            //    (Dim is drawn fresh per frame rather than accumulating.)
            // Note: since back-buffer preserves contents, we'd normally accumulate dim too.
            // That's not what we want. Workaround: dim is applied after blit to surface instead.

            // 3. Blit back-buffer to wallpaper Surface.
            val canvas = try {
                surf.lockCanvas(null)
            } catch (e: Exception) {
                Log.w(TAG, "lockCanvas failed: ${e.message}")
                return
            } ?: return
            try {
                canvas.drawBitmap(bb, 0f, 0f, blitPaint)
                // Dim overlay on top of blit, so it doesn't accumulate into back-buffer.
                if (params.dim > 0f) {
                    val a = (params.dim.coerceIn(0f, 1f) * 255f).toInt()
                    dimPaint.color = (a shl 24)
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
                }
            } finally {
                try { surf.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "render error", e)
        }
    }

    companion object {
        private const val TAG = "ProceduralRenderer"
    }
}
