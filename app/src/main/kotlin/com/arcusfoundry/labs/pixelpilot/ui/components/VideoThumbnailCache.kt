package com.arcusfoundry.labs.pixelpilot.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Extracts a representative still frame from a user-provided video (local file
 * or content URI) via MediaMetadataRetriever, then caches the Bitmap in-memory
 * so the selector cards don't re-decode on every recomposition.
 *
 * Uses the file's path/URI string as the cache key.
 */
object VideoThumbnailCache {

    private val cache = ConcurrentHashMap<String, Bitmap>()

    @Composable
    fun thumbnailFor(source: String): Bitmap? {
        val context = LocalContext.current
        var bitmap by remember(source) { mutableStateOf(cache[source]) }

        LaunchedEffect(source) {
            if (bitmap != null) return@LaunchedEffect
            val cached = cache[source]
            if (cached != null) { bitmap = cached; return@LaunchedEffect }

            val generated = withContext(Dispatchers.IO) { extract(context, source) }
            if (generated != null) {
                cache[source] = generated
                bitmap = generated
            }
        }

        return bitmap
    }

    private fun extract(context: Context, source: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            when {
                source.startsWith("content://") -> retriever.setDataSource(context, Uri.parse(source))
                source.startsWith("file://") -> retriever.setDataSource(Uri.parse(source).path)
                else -> retriever.setDataSource(source)
            }
            // 1-second mark dodges any black intro frames.
            retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
