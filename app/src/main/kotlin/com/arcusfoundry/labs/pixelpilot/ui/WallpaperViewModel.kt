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
import com.arcusfoundry.labs.pixelpilot.source.youtube.YouTubeDownloadService
import kotlinx.coroutines.launch

class WallpaperViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = WallpaperPreferences(app)
    private val youtube = YouTubeDownloadService(app)

    var source by mutableStateOf<WallpaperSource?>(prefs.source)
        private set
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
    var syncThemedIcons by mutableStateOf(prefs.syncThemedIcons)
        private set
    var recents by mutableStateOf(prefs.recents)
        private set
    var downloadState by mutableStateOf<DownloadState>(DownloadState.Idle)
        private set
    var lastVideoError by mutableStateOf(prefs.lastVideoError)
        private set
    var lastVideoState by mutableStateOf(prefs.lastVideoState)
        private set
    var isPixelPilotActiveWallpaper by mutableStateOf(checkIsActive(app))
        private set

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            WallpaperPreferences.KEY_SOURCE -> source = prefs.source
            WallpaperPreferences.KEY_SPEED -> speed = prefs.speed
            WallpaperPreferences.KEY_SCALE -> scale = prefs.scale
            WallpaperPreferences.KEY_DIM -> dim = prefs.dim
            WallpaperPreferences.KEY_TINT_KIND -> tintKind = prefs.tintKind
            WallpaperPreferences.KEY_TINT_COLOR -> tintColor = prefs.tintColor
            WallpaperPreferences.KEY_RAINBOW_CYCLE -> rainbowCycleSeconds = prefs.rainbowCycleSeconds
            WallpaperPreferences.KEY_TINT_STRENGTH -> tintStrength = prefs.tintStrength
            WallpaperPreferences.KEY_SYNC_THEMED_ICONS -> syncThemedIcons = prefs.syncThemedIcons
            WallpaperPreferences.KEY_RECENTS -> recents = prefs.recents
            WallpaperPreferences.KEY_LAST_VIDEO_ERROR -> lastVideoError = prefs.lastVideoError
            WallpaperPreferences.KEY_LAST_VIDEO_STATE -> lastVideoState = prefs.lastVideoState
        }
    }

    init {
        prefs.registerChangeListener(prefsListener)
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

    fun updateSpeed(v: Float) { prefs.speed = v }
    fun updateScale(v: Float) { prefs.scale = v }
    fun updateDim(v: Float) { prefs.dim = v }
    fun updateTintKind(kind: String) { prefs.tintKind = kind }
    fun updateTintColor(color: Int) { prefs.tintColor = color }
    fun updateRainbowCycle(seconds: Float) { prefs.rainbowCycleSeconds = seconds }
    fun updateTintStrength(v: Float) { prefs.tintStrength = v }
    fun updateSyncThemedIcons(v: Boolean) { prefs.syncThemedIcons = v }

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

    fun downloadYouTube(url: String) {
        if (url.isBlank()) return
        downloadState = DownloadState.Running(0f, url)
        viewModelScope.launch {
            val result = youtube.downloadToLocal(url) { read, total ->
                val pct = if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else 0f
                downloadState = DownloadState.Running(pct, url)
            }
            downloadState = result.fold(
                onSuccess = { file ->
                    selectSource(WallpaperSource.LocalFile(file.absolutePath))
                    DownloadState.Done(file.absolutePath)
                },
                onFailure = { DownloadState.Failed(it.message ?: "Download failed") }
            )
        }
    }

    fun clearDownloadState() { downloadState = DownloadState.Idle }

    /** Re-check whether Pixel Pilot is the active system wallpaper. Call on activity resume. */
    fun refreshActiveWallpaperStatus() {
        isPixelPilotActiveWallpaper = checkIsActive(getApplication())
    }

    private fun checkIsActive(context: Context): Boolean {
        val wm = WallpaperManager.getInstance(context)
        return wm.wallpaperInfo?.component?.packageName == context.packageName
    }

    sealed class DownloadState {
        data object Idle : DownloadState()
        data class Running(val progress: Float, val url: String) : DownloadState()
        data class Done(val path: String) : DownloadState()
        data class Failed(val reason: String) : DownloadState()
    }
}
