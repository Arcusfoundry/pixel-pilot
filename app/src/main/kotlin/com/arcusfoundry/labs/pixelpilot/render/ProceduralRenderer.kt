package com.arcusfoundry.labs.pixelpilot.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.util.Log
import android.view.Choreographer
import android.view.Surface

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

    private val dimPaint = Paint()
    private val tintPaint = Paint().apply { isAntiAlias = false }

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
        }
    }

    override fun detach() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        surface = null
        state = null
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
        val s = state ?: return
        if (!surf.isValid) return
        val canvas: Canvas = try {
            surf.lockCanvas(null)
        } catch (e: Exception) {
            Log.w(TAG, "lockCanvas failed: ${e.message}")
            return
        } ?: return
        try {
            val t = SystemClock.uptimeMillis() - startTime
            val tint = effectiveTintColor()
            animation.draw(canvas, width, height, s, t, params.speed, params.scale, tint)
            if (params.dim > 0f) {
                val a = (params.dim.coerceIn(0f, 1f) * 255f).toInt()
                dimPaint.color = (a shl 24)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
            }
        } catch (e: Exception) {
            Log.e(TAG, "render error", e)
        } finally {
            try { surf.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
        }
    }

    companion object {
        private const val TAG = "ProceduralRenderer"
    }
}
