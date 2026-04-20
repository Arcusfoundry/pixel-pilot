package com.arcusfoundry.labs.pixelpilot.theme

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
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
     * Generates a vertical-gradient bitmap from [color] (slightly darkened at top)
     * and sets it as the system wallpaper. Gradient gives Monet a slightly wider
     * palette to extract, producing a more nuanced theme than a pure solid color.
     */
    fun applyThemeColor(context: Context, color: Int): Result<Unit> = runCatching {
        val bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val darker = darken(color, 0.25f)
        val lighter = lighten(color, 0.1f)
        val paint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, BITMAP_SIZE.toFloat(),
                darker, lighter,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, BITMAP_SIZE.toFloat(), BITMAP_SIZE.toFloat(), paint)

        val wm = WallpaperManager.getInstance(context)
        // FLAG_SYSTEM drives home-screen wallpaper, which is what Monet samples.
        wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
        bitmap.recycle()
        Log.i(TAG, "Applied theme color: #${Integer.toHexString(color)}")
    }.onFailure { Log.e(TAG, "Failed to apply theme color", it) }

    private fun darken(color: Int, amount: Float): Int = mix(color, Color.BLACK, amount)
    private fun lighten(color: Int, amount: Float): Int = mix(color, Color.WHITE, amount)

    private fun mix(a: Int, b: Int, t: Float): Int {
        val ta = t.coerceIn(0f, 1f)
        val r = (Color.red(a) * (1 - ta) + Color.red(b) * ta).toInt()
        val g = (Color.green(a) * (1 - ta) + Color.green(b) * ta).toInt()
        val bl = (Color.blue(a) * (1 - ta) + Color.blue(b) * ta).toInt()
        return Color.argb(255, r, g, bl)
    }
}
