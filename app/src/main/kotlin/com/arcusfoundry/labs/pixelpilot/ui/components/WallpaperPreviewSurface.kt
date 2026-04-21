package com.arcusfoundry.labs.pixelpilot.ui.components

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import com.arcusfoundry.labs.pixelpilot.render.ProceduralRenderer
import com.arcusfoundry.labs.pixelpilot.render.RenderParams
import com.arcusfoundry.labs.pixelpilot.render.VideoRenderer
import com.arcusfoundry.labs.pixelpilot.render.WallpaperRenderer
import com.arcusfoundry.labs.pixelpilot.render.animations.AnimationRegistry
import com.arcusfoundry.labs.pixelpilot.source.WallpaperSource

/**
 * Renders the currently-active wallpaper source into an in-app TextureView
 * so the user sees live preview of their choices without leaving the app.
 *
 * Uses the same WallpaperRenderer implementations as the wallpaper service,
 * so behavior matches what the system wallpaper will show. Procedural
 * animations preview fully. Video sources currently show only the first
 * frame plus static playback (Media3 Effects not wired for TextureView yet).
 */
@OptIn(UnstableApi::class)
@Composable
fun WallpaperPreviewSurface(
    source: WallpaperSource?,
    params: RenderParams,
    modifier: Modifier = Modifier
) {
    val state = remember { PreviewState() }

    DisposableEffect(Unit) {
        onDispose {
            state.detachAndRelease()
        }
    }

    // Push the latest params to the live renderer whenever any slider / tint
    // changes. Cannot rely on AndroidView.update alone — Compose can short-circuit
    // if it considers successive lambda invocations identical, and for data-class
    // params with subtle changes that can miss.
    LaunchedEffect(params) {
        state.renderer?.updateParams(params)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                isOpaque = true
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                        state.context = ctx
                        state.bind(st, width, height, source, params)
                    }
                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                        state.resize(width, height, source, params)
                    }
                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        state.detachAndRelease()
                        return true
                    }
                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
            }
        },
        update = {
            // Source change → full rebuild. Params change → in-place update.
            if (state.lastSource != source) {
                state.rebuildIfReady(source, params)
            } else {
                state.renderer?.updateParams(params)
            }
        }
    )
}

@OptIn(UnstableApi::class)
private class PreviewState {
    var renderer: WallpaperRenderer? = null
    var surface: Surface? = null
    var width: Int = 0
    var height: Int = 0
    var lastSource: WallpaperSource? = null
    var context: android.content.Context? = null

    fun bind(st: SurfaceTexture, w: Int, h: Int, source: WallpaperSource?, params: RenderParams) {
        width = w; height = h
        surface = Surface(st)
        lastSource = source
        buildRenderer(source, params)
    }

    fun resize(w: Int, h: Int, source: WallpaperSource?, params: RenderParams) {
        width = w; height = h
        detachAndRelease(keepSurface = true)
        buildRenderer(source, params)
    }

    fun rebuildIfReady(source: WallpaperSource?, params: RenderParams) {
        if (surface == null) { lastSource = source; return }
        lastSource = source
        detachAndRelease(keepSurface = true)
        buildRenderer(source, params)
    }

    private fun buildRenderer(source: WallpaperSource?, params: RenderParams) {
        val surf = surface ?: return
        val ctx = context ?: return
        val r: WallpaperRenderer = when (source) {
            null -> ProceduralRenderer(AnimationRegistry.default)
            is WallpaperSource.Procedural -> {
                val animation = AnimationRegistry.get(source.animationId) ?: AnimationRegistry.default
                ProceduralRenderer(animation)
            }
            is WallpaperSource.Video -> VideoRenderer(ctx, source.uri)
            is WallpaperSource.LocalFile -> VideoRenderer(ctx, source.path)
        }
        r.updateParams(params)
        r.attach(surf, width, height)
        r.setVisible(true)
        renderer = r
    }

    fun detachAndRelease(keepSurface: Boolean = false) {
        renderer?.release()
        renderer = null
        if (!keepSurface) {
            surface?.release()
            surface = null
            context = null
            lastSource = null
        }
    }
}
