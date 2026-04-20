package com.arcusfoundry.labs.pixelpilot.theme

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log

/**
 * Drives Material You color extraction by setting a small solid-color bitmap
 * as the static system wallpaper. Android's Monet engine extracts the dominant
 * color and rebuilds the system theme (icons, accents, settings surfaces) around it.
 *
 * After calling applyThemeColor(), the live wallpaper is replaced with the primed
 * bitmap. Users must manually re-apply the live wallpaper to get their animation
 * back. This is an unavoidable consequence of how Monet sampling works — it runs
 * against whatever the current wallpaper is.
 */
object SystemThemeApplier {

    private const val TAG = "SystemThemeApplier"
    private const val BITMAP_SIZE = 512

    /**
     * Sets a solid-color bitmap as the home-screen wallpaper. Android's Monet
     * engine then samples and extracts the palette, rebuilding the system theme
     * (including launcher icons when Themed Icons is enabled) around that color.
     *
     * Solid color gives Monet the strongest, most predictable signal — a gradient
     * caused the extracted palette to drift based on which region Monet weighted.
     * Also applies FLAG_SYSTEM | FLAG_LOCK so lock and home screens match and
     * Monet re-samples even if the user's current lock wallpaper was the source.
     */
    fun applyThemeColor(context: Context, color: Int): Result<Unit> = runCatching {
        val bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(color)

        val wm = WallpaperManager.getInstance(context)
        val flags = WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        wm.setBitmap(bitmap, null, true, flags)
        bitmap.recycle()
        Log.i(TAG, "Applied theme color: #${Integer.toHexString(color)}")
        Unit
    }.onFailure { Log.e(TAG, "Failed to apply theme color", it) }
}
