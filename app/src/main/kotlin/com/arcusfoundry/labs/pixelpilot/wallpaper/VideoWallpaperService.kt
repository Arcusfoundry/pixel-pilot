package com.arcusfoundry.labs.pixelpilot.wallpaper

import android.app.WallpaperColors
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.media3.common.util.UnstableApi
import com.arcusfoundry.labs.pixelpilot.prefs.WallpaperPreferences
import com.arcusfoundry.labs.pixelpilot.render.AssetLoader
import com.arcusfoundry.labs.pixelpilot.render.GlProceduralRenderer
import com.arcusfoundry.labs.pixelpilot.render.GlVideoRenderer
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
                key.startsWith(WallpaperPreferences.SCENE_KEY_PREFIX) -> {
                    val activeId = (currentSource as? WallpaperSource.Procedural)?.animationId
                    if (activeId != null && prefs.isSceneKeyFor(activeId, key)) {
                        mainHandler.post {
                            (renderer as? GlProceduralRenderer)?.reinitializeWithSceneConfig()
                        }
                    }
                }
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
            // Pin format so EGL window surface bind matches a known config.
            // Otherwise the surface is platform-default which can be RGBX-only
            // and reject our RGBA8888 EGL config on eglCreateWindowSurface.
            surfaceHolder.setFormat(android.graphics.PixelFormat.RGBA_8888)
            prefs.registerChangeListener(prefsListener)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            val frame = holder.surfaceFrame
            surfaceWidth = frame.width()
            surfaceHeight = frame.height()
            Log.d(TAG, "onSurfaceCreated ${surfaceWidth}x${surfaceHeight}")
            detachRenderer()
            attachRenderer()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            Log.d(TAG, "onSurfaceChanged ${width}x${height}")
            detachRenderer()
            attachRenderer()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            detachRenderer()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            // Self-heal: foldable fold/unfold sometimes drops us in a state with
            // no renderer attached. Any visibility transition to visible is a
            // safe time to rebuild if we're missing one.
            if (visible && renderer == null) {
                Log.d(TAG, "visibility true + no renderer, reattaching")
                attachRenderer()
            }
            renderer?.setVisible(visible)
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
            super.onSurfaceRedrawNeeded(holder)
            // System is requesting a redraw (e.g. after wake). Ensure we're attached.
            if (renderer == null) attachRenderer()
        }

        override fun onDestroy() {
            super.onDestroy()
            prefs.unregisterChangeListener(prefsListener)
            detachRenderer()
        }

        private fun reloadSource() {
            // Both procedural and video renderers go through EGL window surfaces
            // now, so source switches are always EGL→EGL handoffs. No producer
            // type change, no need to force surface recreation.
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
                    GlProceduralRenderer(animation, prefs)
                }
                is WallpaperSource.Video -> GlVideoRenderer(this@VideoWallpaperService, source.uri)
                is WallpaperSource.LocalFile -> GlVideoRenderer(this@VideoWallpaperService, source.path)
            }

            newRenderer.updateParams(prefs.renderParams())
            newRenderer.attach(surface, surfaceWidth, surfaceHeight)
            newRenderer.setVisible(isVisible)
            renderer = newRenderer
        }

        private val TAG = "PixelPilotEngine"

        private fun detachRenderer() {
            renderer?.release()
            renderer = null
        }
    }
}
