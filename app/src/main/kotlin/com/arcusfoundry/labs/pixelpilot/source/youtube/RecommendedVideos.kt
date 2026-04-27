package com.arcusfoundry.labs.pixelpilot.source.youtube

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class RecommendedVideo(
    val title: String,
    val youtubeUrl: String,
    val videoId: String
) {
    val thumbnailUrl: String get() = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
}

/**
 * Pulls a curated list of YouTube wallpaper videos from the repo and surfaces
 * them as one-tap downloads. The list lives in
 * `recommended-videos.json` at the repo root so it can be edited without an
 * app release. Cached locally for [CACHE_TTL_MS]; falls back to the cache when
 * offline, and to a bundled empty list on first run with no network.
 */
class RecommendedVideosFetcher(private val context: Context) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val cacheFile = File(context.filesDir, CACHE_FILE_NAME)

    suspend fun fetch(): List<RecommendedVideo> = withContext(Dispatchers.IO) {
        val cacheAge = if (cacheFile.exists())
            System.currentTimeMillis() - cacheFile.lastModified() else Long.MAX_VALUE

        if (cacheAge < CACHE_TTL_MS) {
            readCache()?.let { return@withContext it }
        }

        runCatching { fetchRemote() }.getOrNull()?.let { fresh ->
            writeCache(fresh)
            return@withContext fresh
        }

        readCache() ?: readBundled()
    }

    private fun fetchRemote(): List<RecommendedVideo>? {
        val req = Request.Builder().url(REMOTE_URL).build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                Log.w(TAG, "remote fetch HTTP ${resp.code}")
                return null
            }
            val json = resp.body?.string() ?: return null
            return parse(json)
        }
    }

    private fun readCache(): List<RecommendedVideo>? = runCatching {
        if (!cacheFile.exists()) return null
        parse(cacheFile.readText())
    }.getOrNull()

    private fun writeCache(videos: List<RecommendedVideo>) {
        runCatching {
            val json = JSONObject().apply {
                put("videos", org.json.JSONArray().apply {
                    videos.forEach { v ->
                        put(JSONObject().apply {
                            put("title", v.title)
                            put("youtubeUrl", v.youtubeUrl)
                        })
                    }
                })
            }.toString()
            cacheFile.writeText(json)
        }
    }

    private fun readBundled(): List<RecommendedVideo> = runCatching {
        context.assets.open(BUNDLED_ASSET).bufferedReader().use {
            parse(it.readText())
        }
    }.getOrDefault(emptyList())

    private fun parse(json: String): List<RecommendedVideo> {
        val root = JSONObject(json)
        val arr = root.optJSONArray("videos") ?: return emptyList()
        val out = mutableListOf<RecommendedVideo>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val title = obj.optString("title").takeIf { it.isNotBlank() } ?: continue
            val rawUrl = obj.optString("youtubeUrl").takeIf { it.isNotBlank() } ?: continue
            val canonical = YouTubeDownloadService.canonicalizeYouTubeUrl(rawUrl)
            val videoId = extractVideoId(canonical) ?: continue
            out.add(RecommendedVideo(title = title, youtubeUrl = canonical, videoId = videoId))
        }
        return out
    }

    companion object {
        private const val TAG = "RecommendedVideos"
        private const val REMOTE_URL =
            "https://raw.githubusercontent.com/Arcusfoundry/pixel-pilot/main/recommended-videos.json"
        private const val CACHE_FILE_NAME = "recommended-videos.json"
        private const val BUNDLED_ASSET = "recommended-videos.json"
        private const val CACHE_TTL_MS = 24L * 60 * 60 * 1000

        private val ID_REGEX = Regex("""watch\?v=([A-Za-z0-9_-]{11})""")

        private fun extractVideoId(canonicalUrl: String): String? =
            ID_REGEX.find(canonicalUrl)?.groupValues?.get(1)
    }
}
