package com.arcusfoundry.labs.pixelpilot.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.arcusfoundry.labs.pixelpilot.render.RenderParams
import com.arcusfoundry.labs.pixelpilot.render.SceneConfig
import com.arcusfoundry.labs.pixelpilot.render.SettingSpec
import com.arcusfoundry.labs.pixelpilot.render.TintMode
import com.arcusfoundry.labs.pixelpilot.source.WallpaperSource

class WallpaperPreferences(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var source: WallpaperSource?
        get() = WallpaperSource.parse(prefs.getString(KEY_SOURCE, null))
        set(value) = prefs.edit { putString(KEY_SOURCE, value?.serialize()) }

    var speed: Float
        get() = prefs.getFloat(KEY_SPEED, 1f)
        set(v) = prefs.edit { putFloat(KEY_SPEED, v) }

    var scale: Float
        get() = prefs.getFloat(KEY_SCALE, 1f)
        set(v) = prefs.edit { putFloat(KEY_SCALE, v) }

    var dim: Float
        get() = prefs.getFloat(KEY_DIM, 0f)
        set(v) = prefs.edit { putFloat(KEY_DIM, v) }

    /** One of: "none", "static", "rainbow". */
    var tintKind: String
        get() = prefs.getString(KEY_TINT_KIND, "none") ?: "none"
        set(v) = prefs.edit { putString(KEY_TINT_KIND, v) }

    var tintColor: Int
        get() = prefs.getInt(KEY_TINT_COLOR, 0xFFC3D95E.toInt())
        set(v) = prefs.edit { putInt(KEY_TINT_COLOR, v) }

    var rainbowCycleSeconds: Float
        get() = prefs.getFloat(KEY_RAINBOW_CYCLE, 20f)
        set(v) = prefs.edit { putFloat(KEY_RAINBOW_CYCLE, v) }

    var tintStrength: Float
        get() = prefs.getFloat(KEY_TINT_STRENGTH, 0.6f)
        set(v) = prefs.edit { putFloat(KEY_TINT_STRENGTH, v) }

    var syncThemedIcons: Boolean
        get() = prefs.getBoolean(KEY_SYNC_THEMED_ICONS, false)
        set(v) = prefs.edit { putBoolean(KEY_SYNC_THEMED_ICONS, v) }

    /**
     * Color the wallpaper engine should declare via onComputeColors(). Null means
     * "no override, sample naturally from frames." Non-null drives Material You
     * Monet extraction directly without replacing the wallpaper.
     */
    var systemSyncColor: Int?
        get() = if (prefs.contains(KEY_SYSTEM_SYNC_COLOR)) prefs.getInt(KEY_SYSTEM_SYNC_COLOR, 0) else null
        set(v) = prefs.edit {
            if (v == null) remove(KEY_SYSTEM_SYNC_COLOR) else putInt(KEY_SYSTEM_SYNC_COLOR, v)
        }

    /** Last video playback error, or null if playback is healthy. */
    var lastVideoError: String?
        get() = prefs.getString(KEY_LAST_VIDEO_ERROR, null)
        set(v) = prefs.edit {
            if (v == null) remove(KEY_LAST_VIDEO_ERROR) else putString(KEY_LAST_VIDEO_ERROR, v)
        }

    /** Current playback state trail ("ATTACH 1234x5678 / IDLE → BUFFERING"). */
    var lastVideoState: String?
        get() = prefs.getString(KEY_LAST_VIDEO_STATE, null)
        set(v) = prefs.edit {
            if (v == null) remove(KEY_LAST_VIDEO_STATE) else putString(KEY_LAST_VIDEO_STATE, v)
        }

    /**
     * MRU list of user-provided video sources, newest first. Each entry is
     * a serialized WallpaperSource ("video:..." or "file:..."). Capped at 5.
     */
    var recents: List<String>
        get() = prefs.getString(KEY_RECENTS, null)
            ?.split('\u001F')
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        set(value) = prefs.edit {
            putString(KEY_RECENTS, value.take(5).joinToString("\u001F"))
        }

    fun pushRecent(serialized: String) {
        // Stable add order: re-selecting an existing entry must not reorder
        // the list. Tiles stay where the user put them. When the cap is
        // exceeded, drop the oldest (leftmost) entry.
        if (recents.contains(serialized)) return
        recents = (recents + serialized).takeLast(5)
    }

    fun removeRecent(serialized: String) {
        recents = recents.filter { it != serialized }
    }

    /**
     * Reads the user's scene-scoped values for an animation (keyed under
     * "scene:<animationId>:<settingKey>"), folding in each spec's default.
     */
    fun sceneConfig(animationId: String, specs: List<SettingSpec>): SceneConfig {
        if (specs.isEmpty()) return SceneConfig.EMPTY
        val map = mutableMapOf<String, Any?>()
        for (spec in specs) {
            val k = sceneKey(animationId, spec.key)
            map[spec.key] = when (spec) {
                is SettingSpec.Text -> prefs.getString(k, spec.default) ?: spec.default
                is SettingSpec.IntRange -> prefs.getInt(k, spec.default)
                is SettingSpec.Color -> prefs.getInt(k, spec.default)
                is SettingSpec.Choice -> prefs.getString(k, spec.default) ?: spec.default
            }
        }
        return SceneConfig(map)
    }

    fun setSceneValue(animationId: String, key: String, value: Any) {
        val k = sceneKey(animationId, key)
        prefs.edit {
            when (value) {
                is String -> putString(k, value)
                is Int -> putInt(k, value)
                is Float -> putFloat(k, value)
                is Boolean -> putBoolean(k, value)
                else -> putString(k, value.toString())
            }
        }
    }

    private fun sceneKey(animationId: String, settingKey: String): String =
        "$SCENE_KEY_PREFIX$animationId:$settingKey"

    /** True if [key] is a scene-scoped key belonging to [animationId]. */
    fun isSceneKeyFor(animationId: String, key: String?): Boolean =
        key != null && key.startsWith("$SCENE_KEY_PREFIX$animationId:")

    fun renderParams(): RenderParams {
        val tintMode = when (tintKind) {
            "static" -> TintMode.Static(tintColor)
            "rainbow" -> TintMode.Rainbow(rainbowCycleSeconds)
            else -> TintMode.None
        }
        return RenderParams(
            speed = speed,
            scale = scale,
            dim = dim,
            tint = tintMode,
            tintStrength = tintStrength
        )
    }

    fun registerChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        const val PREFS_NAME = "pixel_pilot_wallpaper"
        const val KEY_SOURCE = "source"
        const val KEY_SPEED = "speed"
        const val KEY_SCALE = "scale"
        const val KEY_DIM = "dim"
        const val KEY_TINT_KIND = "tint_kind"
        const val KEY_TINT_COLOR = "tint_color"
        const val KEY_RAINBOW_CYCLE = "rainbow_cycle"
        const val KEY_TINT_STRENGTH = "tint_strength"
        const val KEY_SYNC_THEMED_ICONS = "sync_themed_icons"
        const val KEY_SYSTEM_SYNC_COLOR = "system_sync_color"
        const val KEY_LAST_VIDEO_ERROR = "last_video_error"
        const val KEY_LAST_VIDEO_STATE = "last_video_state"
        const val KEY_RECENTS = "recents"
        const val SCENE_KEY_PREFIX = "scene:"

        val ALL_PARAM_KEYS = setOf(
            KEY_SOURCE, KEY_SPEED, KEY_SCALE, KEY_DIM,
            KEY_TINT_KIND, KEY_TINT_COLOR, KEY_RAINBOW_CYCLE, KEY_TINT_STRENGTH
        )
    }
}
