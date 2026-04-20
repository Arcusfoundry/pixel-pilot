package com.arcusfoundry.labs.pixelpilot.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.arcusfoundry.labs.pixelpilot.render.RenderParams
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
        val next = (listOf(serialized) + recents.filter { it != serialized }).take(5)
        recents = next
    }

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
        const val KEY_RECENTS = "recents"

        val ALL_PARAM_KEYS = setOf(
            KEY_SOURCE, KEY_SPEED, KEY_SCALE, KEY_DIM,
            KEY_TINT_KIND, KEY_TINT_COLOR, KEY_RAINBOW_CYCLE, KEY_TINT_STRENGTH
        )
    }
}
