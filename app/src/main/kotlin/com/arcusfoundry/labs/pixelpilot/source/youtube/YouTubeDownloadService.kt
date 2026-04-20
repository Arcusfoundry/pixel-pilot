package com.arcusfoundry.labs.pixelpilot.source.youtube

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.localization.Localization
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Extracts a YouTube video's direct video stream URL via NewPipeExtractor and downloads
 * it to the app's cache directory. After download completes, the file is a plain MP4
 * that VideoRenderer can play locally, with no further network traffic.
 */
class YouTubeDownloadService(private val context: Context) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    init {
        ensureNewPipeInitialized()
    }

    suspend fun downloadToLocal(
        youtubeUrl: String,
        onProgress: (readBytes: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            ensureNewPipeInitialized()
            val service = NewPipe.getService(ServiceList.YouTube.serviceId)
            val extractor = service.getStreamExtractor(youtubeUrl)
            extractor.fetchPage()

            val videoStream = extractor.videoStreams
                ?.filter { it.content != null }
                ?.maxByOrNull { it.resolution?.let { r -> parseResolution(r) } ?: 0 }
                ?: throw IllegalStateException("No playable video streams found")

            val streamUrl = videoStream.content ?: error("Missing stream URL")
            val outFile = File(context.cacheDir, "pp_${youtubeUrl.hashCode()}_${System.currentTimeMillis()}.mp4")

            http.newCall(Request.Builder().url(streamUrl).build()).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code} fetching stream")
                val body = resp.body ?: error("Empty body")
                val total = body.contentLength()
                body.source().use { source ->
                    outFile.sink().buffer().use { sink ->
                        var read = 0L
                        val buf = okio.Buffer()
                        while (true) {
                            val n = source.read(buf, 8 * 1024L)
                            if (n == -1L) break
                            sink.write(buf, n)
                            read += n
                            onProgress(read, total)
                        }
                    }
                }
            }
            outFile
        }.onFailure { Log.e(TAG, "YouTube download failed", it) }
    }

    private fun parseResolution(raw: String): Int =
        raw.takeWhile { it.isDigit() }.toIntOrNull() ?: 0

    companion object {
        private const val TAG = "YouTubeDownloadService"

        @Volatile private var initialized = false

        @Synchronized
        fun ensureNewPipeInitialized() {
            if (initialized) return
            NewPipe.init(NewPipeDownloader.getInstance(), Localization.DEFAULT)
            initialized = true
        }
    }
}
