package com.arcusfoundry.labs.pixelpilot.theme

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import com.arcusfoundry.labs.pixelpilot.prefs.WallpaperPreferences

/**
 * Two-pronged Material You sync:
 *
 * 1. If Pixel Pilot is the current wallpaper, writing to prefs.systemSyncColor
 *    fires the wallpaper engine's prefs listener, which calls
 *    Engine.notifyColorsChanged(). Android then re-invokes onComputeColors()
 *    on the engine, which returns WallpaperColors(syncColor). Monet extracts
 *    from that directly. No wallpaper replacement, no re-apply required.
 *
 * 2. If some other wallpaper is active (or as a fallback), also setBitmap()
 *    a solid-color image so Monet has something to sample even without our
 *    engine running.
 *
 * Running both is cheap and gives the best chance of working across devices
 * and launcher behaviors.
 */
object SystemThemeApplier {

    private const val TAG = "SystemThemeApplier"
    private const val BITMAP_SIZE = 512

    fun applyThemeColor(context: Context, color: Int): Result<Unit> = runCatching {
        // Path 1: write to prefs so our wallpaper engine re-publishes colors.
        WallpaperPreferences(context).systemSyncColor = color

        // Path 2: also set a solid bitmap so if Pixel Pilot isn't the active
        // wallpaper, Monet still has a sample to extract from.
        val bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888).apply {
            setHasAlpha(false)
        }
        Canvas(bitmap).drawColor(color)

        val wm = WallpaperManager.getInstance(context)
        val flags = WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        wm.setBitmap(bitmap, null, true, flags)
        bitmap.recycle()
        Log.i(TAG, "Applied theme color: #${Integer.toHexString(color)}")
        Unit
    }.onFailure { Log.e(TAG, "Failed to apply theme color", it) }
}
