package com.arcusfoundry.labs.pixelpilot.ui.components

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ProceduralRenderer

/**
 * Hosts a TextureView running a ProceduralRenderer for live preview of a single
 * animation in a selector card. LazyRow composes items only while visible, so
 * only visible cards run their renderer — scrolling a card out of view releases
 * its renderer via the TextureView lifecycle.
 */
@Composable
fun LiveAnimationThumbnail(
    animation: Animation,
    modifier: Modifier = Modifier
) {
    val state = remember { ThumbState() }
    DisposableEffect(animation.id) {
        onDispose { state.tearDown() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                isOpaque = true
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                        state.bind(animation, st, width, height)
                    }
                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                        state.resize(animation, width, height)
                    }
                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        state.tearDown()
                        return true
                    }
                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
            }
        }
    )
}

private class ThumbState {
    private var renderer: ProceduralRenderer? = null
    private var surface: Surface? = null

    fun bind(animation: Animation, st: SurfaceTexture, w: Int, h: Int) {
        tearDown()
        val s = Surface(st)
        surface = s
        renderer = ProceduralRenderer(animation).apply {
            attach(s, w, h)
            setVisible(true)
        }
    }

    fun resize(animation: Animation, w: Int, h: Int) {
        val s = surface ?: return
        renderer?.release()
        renderer = ProceduralRenderer(animation).apply {
            attach(s, w, h)
            setVisible(true)
        }
    }

    fun tearDown() {
        renderer?.release()
        renderer = null
        surface?.release()
        surface = null
    }
}
