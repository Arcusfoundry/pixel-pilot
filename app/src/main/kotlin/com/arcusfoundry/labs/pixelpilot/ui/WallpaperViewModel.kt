package com.arcusfoundry.labs.pixelpilot.ui

import android.app.Application
import android.app.WallpaperManager
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcusfoundry.labs.pixelpilot.prefs.WallpaperPreferences
import com.arcusfoundry.labs.pixelpilot.source.WallpaperSource
import com.arcusfoundry.labs.pixelpilot.source.youtube.RecommendedVideo
import com.arcusfoundry.labs.pixelpilot.source.youtube.RecommendedVideosFetcher
import com.arcusfoundry.labs.pixelpilot.source.youtube.YouTubeDownloadService
import kotlinx.coroutines.launch

class WallpaperViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = WallpaperPreferences(app)
    private val youtube = YouTubeDownloadService(app)
    private val recommendedFetcher = RecommendedVideosFetcher(app)

    var source by mutableStateOf<WallpaperSource?>(prefs.source)
        private set
    // Playback params reflect the active scene's per-scene overrides for
    // procedural sources, falling back to globals. Video sources show
    // globals (per-video config can come later if useful).
    var speed by mutableStateOf(prefs.speed)
        private set
    var scale by mutableStateOf(prefs.scale)
        private set
    var dim by mutableStateOf(prefs.dim)
        private set
    var tintKind by mutableStateOf(prefs.tintKind)
        private set
    var tintColor by mutableStateOf(prefs.tintColor)
        private set
    var rainbowCycleSeconds by mutableStateOf(prefs.rainbowCycleSeconds)
        private set
    var tintStrength by mutableStateOf(prefs.tintStrength)
        private set
    var shuffleEnabled by mutableStateOf(prefs.shuffleEnabled)
        private set
    var favoriteIds by mutableStateOf<Set<String>>(prefs.allFavorites().toSet())
        private set
    var recommendedVideos by mutableStateOf<List<RecommendedVideo>>(emptyList())
        private set
    var syncThemedIcons by mutableStateOf(prefs.syncThemedIcons)
        private set
    var recents by mutableStateOf(prefs.recents)
        private set
    var pendingDownloads by mutableStateOf<List<PendingDownload>>(emptyList())
        private set
    var lastVideoError by mutableStateOf(prefs.lastVideoError)
        private set
    var lastVideoState by mutableStateOf(prefs.lastVideoState)
        private set
    var isPixelPilotActiveWallpaper by mutableStateOf(checkIsActive(app))
        private set

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when {
            key == WallpaperPreferences.KEY_SOURCE -> {
                source = prefs.source
                refreshActiveSceneParams()
            }
            key == WallpaperPreferences.KEY_SYNC_THEMED_ICONS ->
                syncThemedIcons = prefs.syncThemedIcons
            key == WallpaperPreferences.KEY_RECENTS -> recents = prefs.recents
            key == WallpaperPreferences.KEY_LAST_VIDEO_ERROR -> lastVideoError = prefs.lastVideoError
            key == WallpaperPreferences.KEY_LAST_VIDEO_STATE -> lastVideoState = prefs.lastVideoState
            key == WallpaperPreferences.KEY_SHUFFLE_ENABLED ->
                shuffleEnabled = prefs.shuffleEnabled
            key != null && key.startsWith(WallpaperPreferences.FAVORITE_PREFIX) ->
                favoriteIds = prefs.allFavorites().toSet()
            // Per-scene param updates only refresh the UI when they belong to
            // the currently-active animation. Other scenes' overrides change
            // silently in storage.
            key != null && key.startsWith(WallpaperPreferences.SCENE_PARAM_PREFIX) -> {
                val activeId = (prefs.source as? WallpaperSource.Procedural)?.animationId
                if (activeId != null && prefs.isSceneParamKeyFor(activeId, key)) {
                    refreshActiveSceneParams()
                }
            }
        }
    }

    init {
        prefs.registerChangeListener(prefsListener)
        refreshActiveSceneParams()
        viewModelScope.launch {
            recommendedVideos = recommendedFetcher.fetch()
        }
    }

    /**
     * Reload the seven playback param state vars from the active scene's
     * overrides. Call after the source changes or when one of the active
     * scene's per-scene params changes externally.
     */
    private fun refreshActiveSceneParams() {
        val animId = (prefs.source as? WallpaperSource.Procedural)?.animationId
        if (animId != null) {
            speed = prefs.sceneSpeed(animId)
            scale = prefs.sceneScale(animId)
            dim = prefs.sceneDim(animId)
            tintKind = prefs.sceneTintKind(animId)
            tintColor = prefs.sceneTintColor(animId)
            rainbowCycleSeconds = prefs.sceneRainbowCycle(animId)
            tintStrength = prefs.sceneTintStrength(animId)
        } else {
            speed = prefs.speed
            scale = prefs.scale
            dim = prefs.dim
            tintKind = prefs.tintKind
            tintColor = prefs.tintColor
            rainbowCycleSeconds = prefs.rainbowCycleSeconds
            tintStrength = prefs.tintStrength
        }
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterChangeListener(prefsListener)
    }

    fun selectSource(newSource: WallpaperSource) {
        prefs.source = newSource
        if (newSource is WallpaperSource.Video || newSource is WallpaperSource.LocalFile) {
            prefs.pushRecent(newSource.serialize())
        }
    }

    /**
     * Remove a video tile from the user's "Your Videos" row. For YouTube
     * downloads (LocalFile), also deletes the underlying MP4 from filesDir.
     * For SAF picks (Video), releases our persistable URI permission so the
     * user's content provider doesn't keep granting us access. If the removed
     * source was the active wallpaper, fall back to the default procedural
     * scene so the wallpaper engine has something to render.
     */
    fun removeVideoSource(source: WallpaperSource) {
        prefs.removeRecent(source.serialize())
        when (source) {
            is WallpaperSource.LocalFile -> {
                runCatching { java.io.File(source.path).delete() }
            }
            is WallpaperSource.Video -> {
                runCatching {
                    getApplication<Application>().contentResolver
                        .releasePersistableUriPermission(
                            android.net.Uri.parse(source.uri),
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                }
            }
            else -> {}
        }
        if (prefs.source?.serialize() == source.serialize()) {
            val defaultId = com.arcusfoundry.labs.pixelpilot.render.animations.AnimationRegistry.default.id
            prefs.source = WallpaperSource.Procedural(defaultId)
        }
    }

    /**
     * Builds a RenderParams snapshot from the current in-memory state. Safe to
     * call from the UI thread on every recomposition; no prefs IO.
     */
    fun renderParams(): com.arcusfoundry.labs.pixelpilot.render.RenderParams {
        val tintMode = when (tintKind) {
            "static" -> com.arcusfoundry.labs.pixelpilot.render.TintMode.Static(tintColor)
            "rainbow" -> com.arcusfoundry.labs.pixelpilot.render.TintMode.Rainbow(rainbowCycleSeconds)
            else -> com.arcusfoundry.labs.pixelpilot.render.TintMode.None
        }
        return com.arcusfoundry.labs.pixelpilot.render.RenderParams(
            speed = speed, scale = scale, dim = dim,
            tint = tintMode, tintStrength = tintStrength
        )
    }

    // Per-scene param writes go to the active animation's namespace if a
    // procedural source is active. For video sources (or no source), fall
    // back to global prefs so the slider still does something.
    private val activeAnimId: String?
        get() = (prefs.source as? WallpaperSource.Procedural)?.animationId

    fun updateSpeed(v: Float) {
        speed = v
        activeAnimId?.let { prefs.setSceneSpeed(it, v) } ?: run { prefs.speed = v }
    }
    fun updateScale(v: Float) {
        scale = v
        activeAnimId?.let { prefs.setSceneScale(it, v) } ?: run { prefs.scale = v }
    }
    fun updateDim(v: Float) {
        dim = v
        activeAnimId?.let { prefs.setSceneDim(it, v) } ?: run { prefs.dim = v }
    }
    fun updateTintKind(kind: String) {
        tintKind = kind
        activeAnimId?.let { prefs.setSceneTintKind(it, kind) } ?: run { prefs.tintKind = kind }
    }
    fun updateTintColor(color: Int) {
        tintColor = color
        activeAnimId?.let { prefs.setSceneTintColor(it, color) } ?: run { prefs.tintColor = color }
    }
    fun updateRainbowCycle(seconds: Float) {
        rainbowCycleSeconds = seconds
        activeAnimId?.let { prefs.setSceneRainbowCycle(it, seconds) } ?: run { prefs.rainbowCycleSeconds = seconds }
    }
    fun updateTintStrength(v: Float) {
        tintStrength = v
        activeAnimId?.let { prefs.setSceneTintStrength(it, v) } ?: run { prefs.tintStrength = v }
    }
    fun updateSyncThemedIcons(v: Boolean) { prefs.syncThemedIcons = v }

    fun isFavorite(animationId: String): Boolean = favoriteIds.contains(animationId)
    fun toggleFavorite(animationId: String) {
        prefs.setFavorite(animationId, !prefs.isFavorite(animationId))
    }
    fun toggleShuffle() { prefs.shuffleEnabled = !prefs.shuffleEnabled }

    /** Current scene-scoped values for an animation, folded with each spec's default. */
    fun sceneValues(animation: com.arcusfoundry.labs.pixelpilot.render.Animation): Map<String, Any?> {
        val cfg = prefs.sceneConfig(animation.id, animation.settings)
        return animation.settings.associate { spec ->
            spec.key to when (spec) {
                is com.arcusfoundry.labs.pixelpilot.render.SettingSpec.Text -> cfg.string(spec.key, spec.default)
                is com.arcusfoundry.labs.pixelpilot.render.SettingSpec.IntRange -> cfg.int(spec.key, spec.default)
                is com.arcusfoundry.labs.pixelpilot.render.SettingSpec.Color -> cfg.color(spec.key, spec.default)
                is com.arcusfoundry.labs.pixelpilot.render.SettingSpec.Choice -> cfg.choice(spec.key, spec.default)
            }
        }
    }

    fun setSceneValue(animationId: String, key: String, value: Any) {
        prefs.setSceneValue(animationId, key, value)
    }

    fun persistPickedVideo(context: Context, uriString: String) {
        // Take persistable URI permission so the wallpaper service can still
        // read the content URI after app process dies.
        try {
            context.contentResolver.takePersistableUriPermission(
                android.net.Uri.parse(uriString),
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some providers don't grant persistable permissions; source will
            // still work as long as app process is alive.
        }
        selectSource(WallpaperSource.Video(uriString))
    }

    /**
     * Kicks off a YouTube download in the background. Adds an entry to
     * [pendingDownloads] (which the UI renders as a tile in the videos row),
     * fetches the video's title + poster image, then runs the actual stream
     * download with progress callbacks. On success the download tile is
     * removed and the new local file is set as the active wallpaper. On
     * failure the tile shows the error briefly, then disappears.
     */
    fun startBackgroundDownload(url: String) {
        if (url.isBlank()) return
        val id = "${url}_${System.currentTimeMillis()}"
        pendingDownloads = pendingDownloads + PendingDownload(
            id = id,
            url = url,
            title = "Loading…",
            thumbnailUrl = null,
            progress = 0f,
            errorMessage = null
        )

        viewModelScope.launch {
            // Pre-flight metadata (title + poster) so the tile has something
            // to show before bytes start arriving.
            val meta = youtube.extractMetadata(url).getOrNull()
            if (meta != null) {
                updatePendingDownload(id) {
                    it.copy(title = meta.title, thumbnailUrl = meta.thumbnailUrl)
                }
            }

            val result = youtube.downloadToLocal(url) { read, total ->
                val pct = if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else 0f
                updatePendingDownload(id) { it.copy(progress = pct) }
            }

            result.fold(
                onSuccess = { file ->
                    pendingDownloads = pendingDownloads.filter { it.id != id }
                    selectSource(WallpaperSource.LocalFile(file.absolutePath))
                },
                onFailure = { e ->
                    updatePendingDownload(id) {
                        it.copy(errorMessage = e.message ?: "Download failed")
                    }
                    kotlinx.coroutines.delay(5000)
                    pendingDownloads = pendingDownloads.filter { it.id != id }
                }
            )
        }
    }

    private fun updatePendingDownload(id: String, transform: (PendingDownload) -> PendingDownload) {
        pendingDownloads = pendingDownloads.map { if (it.id == id) transform(it) else it }
    }

    /** Re-check whether Pixel Pilot is the active system wallpaper. Call on activity resume. */
    fun refreshActiveWallpaperStatus() {
        isPixelPilotActiveWallpaper = checkIsActive(getApplication())
    }

    private fun checkIsActive(context: Context): Boolean {
        val wm = WallpaperManager.getInstance(context)
        return wm.wallpaperInfo?.component?.packageName == context.packageName
    }

    data class PendingDownload(
        val id: String,
        val url: String,
        val title: String,
        val thumbnailUrl: String?,
        val progress: Float,
        val errorMessage: String?
    )
}
