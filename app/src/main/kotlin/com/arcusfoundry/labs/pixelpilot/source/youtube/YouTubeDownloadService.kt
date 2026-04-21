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
import org.schabi.newpipe.extractor.stream.DeliveryMethod
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

            // Progressive HTTP streams only — single-URL complete files that
            // play standalone. DASH / HLS entries are fragments and need a
            // manifest to reassemble; fetching one alone yields a file the
            // thumbnail extractor can read but ExoPlayer can't play.
            val allCandidates = (extractor.videoStreams.orEmpty() + extractor.videoOnlyStreams.orEmpty())
                .filter { !it.content.isNullOrBlank() }
            val progressive = allCandidates.filter {
                it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP
            }
            Log.w(TAG, "candidates total=${allCandidates.size} progressive=${progressive.size}")
            val videoStream = (if (progressive.isNotEmpty()) progressive else allCandidates)
                .maxByOrNull { it.resolution?.let { r -> parseResolution(r) } ?: 0 }
                ?: throw IllegalStateException("No playable video streams found")

            val streamUrl = videoStream.content ?: error("Missing stream URL")
            Log.i(TAG, "Selected stream: res=${videoStream.resolution} fmt=${videoStream.format}")
            // filesDir (not cacheDir) so Android doesn't reclaim the wallpaper source
            // under storage pressure. Lives in a dedicated subdir for tidy listing.
            val dir = File(context.filesDir, "downloaded-videos").apply { mkdirs() }
            val outFile = File(dir, "pp_${youtubeUrl.hashCode()}_${System.currentTimeMillis()}.mp4")

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
                        Log.i(TAG, "Downloaded ${read} bytes (Content-Length was $total)")
                    }
                }
            }
            // Verify the file looks like a recognizable video container.
            if (!isKnownMediaContainer(outFile)) {
                outFile.delete()
                error("Downloaded file does not start with a recognized video container header")
            }
            Log.i(TAG, "Download complete: ${outFile.length()} bytes at ${outFile.absolutePath}")
            outFile
        }.onFailure { Log.e(TAG, "YouTube download failed", it) }
    }

    private fun parseResolution(raw: String): Int =
        raw.takeWhile { it.isDigit() }.toIntOrNull() ?: 0

    /**
     * Sniffs the file's leading bytes for a known container header. Catches
     * the case where NewPipe returned a URL whose HTTP response body is
     * something other than a playable video (e.g. a JSON error, HTML, or
     * encrypted fragment that requires different handling).
     */
    private fun isKnownMediaContainer(file: File): Boolean = try {
        file.inputStream().use { input ->
            val header = ByteArray(12)
            val n = input.read(header)
            if (n < 12) return@use false
            // MP4 / QuickTime: 'ftyp' at bytes 4-7.
            val isMp4 = header[4] == 'f'.code.toByte() &&
                header[5] == 't'.code.toByte() &&
                header[6] == 'y'.code.toByte() &&
                header[7] == 'p'.code.toByte()
            // WebM / EBML: 0x1A 0x45 0xDF 0xA3 at the start.
            val isWebm = header[0] == 0x1A.toByte() &&
                header[1] == 0x45.toByte() &&
                (header[2].toInt() and 0xFF) == 0xDF &&
                (header[3].toInt() and 0xFF) == 0xA3
            isMp4 || isWebm
        }
    } catch (_: Throwable) { false }

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
