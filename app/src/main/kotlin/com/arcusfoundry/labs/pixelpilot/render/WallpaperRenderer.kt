package com.arcusfoundry.labs.pixelpilot.render

import android.view.Surface

interface WallpaperRenderer {
    fun attach(surface: Surface, width: Int, height: Int)
    fun setVisible(visible: Boolean)
    fun updateParams(params: RenderParams)
    fun detach()
    fun release()
}
