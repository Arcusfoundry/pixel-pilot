package com.arcusfoundry.labs.pixelpilot.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.arcusfoundry.labs.pixelpilot.render.Animation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Generates and caches a representative still-frame Bitmap for each animation,
 * letting the user preview visually in selector cards without running 21 live
 * renderers simultaneously.
 *
 * Generation runs 120 draw() iterations on a background thread — enough for
 * fade/trail animations to establish their characteristic appearance. Thumbnails
 * are cached by animation id, so re-entering a screen pulls from memory.
 */
object AnimationThumbnailCache {

    private const val WIDTH = 256
    private const val HEIGHT = 192
    private const val STABILIZE_FRAMES = 120

    private val cache = ConcurrentHashMap<String, Bitmap>()

    @Composable
    fun thumbnailFor(animation: Animation): Bitmap? {
        var bitmap by remember(animation.id) { mutableStateOf(cache[animation.id]) }

        LaunchedEffect(animation.id) {
            if (bitmap != null) return@LaunchedEffect
            val cached = cache[animation.id]
            if (cached != null) {
                bitmap = cached
                return@LaunchedEffect
            }
            val generated = withContext(Dispatchers.Default) {
                generate(animation)
            }
            cache[animation.id] = generated
            bitmap = generated
        }

        return bitmap
    }

    private fun generate(animation: Animation): Bitmap {
        val bm = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)
        canvas.drawColor(animation.defaultBackground)
        val state = animation.initialize(WIDTH, HEIGHT, 1f)
        for (i in 0 until STABILIZE_FRAMES) {
            try {
                animation.draw(
                    canvas, WIDTH, HEIGHT, state,
                    timeMs = (i * 16L),
                    speed = 1f,
                    scale = 1f,
                    tintColor = null
                )
            } catch (_: Throwable) {
                // One animation glitching shouldn't break the whole picker.
                break
            }
        }
        return bm
    }
}
