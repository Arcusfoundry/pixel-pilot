package com.arcusfoundry.labs.pixelpilot.wallpaper

import android.app.WallpaperColors
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.media3.common.util.UnstableApi
import com.arcusfoundry.labs.pixelpilot.prefs.WallpaperPreferences
import com.arcusfoundry.labs.pixelpilot.render.AssetLoader
import com.arcusfoundry.labs.pixelpilot.render.ProceduralRenderer
import com.arcusfoundry.labs.pixelpilot.render.VideoRenderer
import com.arcusfoundry.labs.pixelpilot.render.WallpaperRenderer
import com.arcusfoundry.labs.pixelpilot.render.animations.AnimationRegistry
import com.arcusfoundry.labs.pixelpilot.source.WallpaperSource

/**
 * The wallpaper service. Wires WallpaperSource from prefs to the matching
 * WallpaperRenderer (procedural / video / local file) and forwards lifecycle.
 */
class VideoWallpaperService : WallpaperService() {

    override fun onCreate() {
        super.onCreate()
        AssetLoader.initialize(this)
    }

    @OptIn(UnstableApi::class)
    override fun onCreateEngine(): Engine = PixelPilotEngine()

    @OptIn(UnstableApi::class)
    private inner class PixelPilotEngine : Engine() {

        private var renderer: WallpaperRenderer? = null
        private var currentSource: WallpaperSource? = null
        private val prefs by lazy { WallpaperPreferences(this@VideoWallpaperService) }
        private val mainHandler = Handler(Looper.getMainLooper())
        private var surfaceWidth = 0
        private var surfaceHeight = 0

        private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null) return@OnSharedPreferenceChangeListener
            when {
                key == WallpaperPreferences.KEY_SOURCE ->
                    mainHandler.post { reloadSource() }
                key == WallpaperPreferences.KEY_SYSTEM_SYNC_COLOR ->
                    mainHandler.post { notifyColorsChanged() }
                key in WallpaperPreferences.ALL_PARAM_KEYS ->
                    mainHandler.post { renderer?.updateParams(prefs.renderParams()) }
            }
        }

        override fun onComputeColors(): WallpaperColors? {
            val c = prefs.systemSyncColor ?: return super.onComputeColors()
            return WallpaperColors(Color.valueOf(c), null, null)
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
            prefs.registerChangeListener(prefsListener)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            val frame = holder.surfaceFrame
            surfaceWidth = frame.width()
            surfaceHeight = frame.height()
            attachRenderer()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            detachRenderer()
            attachRenderer()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            detachRenderer()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            renderer?.setVisible(visible)
        }

        override fun onDestroy() {
            super.onDestroy()
            prefs.unregisterChangeListener(prefsListener)
            detachRenderer()
        }

        private fun reloadSource() {
            detachRenderer()
            attachRenderer()
        }

        private fun attachRenderer() {
            val holder = surfaceHolder ?: return
            val surface = holder.surface
            if (!surface.isValid) return
            if (surfaceWidth == 0 || surfaceHeight == 0) {
                val frame = holder.surfaceFrame
                surfaceWidth = frame.width()
                surfaceHeight = frame.height()
                if (surfaceWidth == 0 || surfaceHeight == 0) return
            }

            val source = prefs.source ?: WallpaperSource.Procedural(AnimationRegistry.default.id)
            currentSource = source

            val newRenderer: WallpaperRenderer = when (source) {
                is WallpaperSource.Procedural -> {
                    val animation = AnimationRegistry.get(source.animationId) ?: AnimationRegistry.default
                    ProceduralRenderer(animation)
                }
                is WallpaperSource.Video -> VideoRenderer(this@VideoWallpaperService, source.uri)
                is WallpaperSource.LocalFile -> VideoRenderer(this@VideoWallpaperService, source.path)
            }

            newRenderer.updateParams(prefs.renderParams())
            newRenderer.attach(surface, surfaceWidth, surfaceHeight)
            newRenderer.setVisible(isVisible)
            renderer = newRenderer
        }

        private fun detachRenderer() {
            renderer?.release()
            renderer = null
        }
    }
}
