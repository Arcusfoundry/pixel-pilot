package com.arcusfoundry.labs.pixelpilot.render

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Global access to app Resources for animations that need bitmap assets.
 * Initialized by VideoWallpaperService.onCreate() and MainActivity.onCreate().
 *
 * Using a singleton rather than plumbing Context through the Animation interface
 * keeps the interface clean for the ~95% of animations that are pure procedural.
 */
object AssetLoader {

    @Volatile private var resources: Resources? = null
    private val bitmapCache = mutableMapOf<Int, Bitmap>()

    fun initialize(context: Context) {
        resources = context.applicationContext.resources
    }

    fun loadBitmap(resourceId: Int): Bitmap? {
        bitmapCache[resourceId]?.let { return it }
        val res = resources ?: return null
        val opts = BitmapFactory.Options().apply {
            inScaled = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeResource(res, resourceId, opts)?.also {
            bitmapCache[resourceId] = it
        }
    }
}
