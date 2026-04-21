package com.arcusfoundry.labs.pixelpilot.render

import android.graphics.Canvas

interface Animation {
    val id: String
    val displayName: String
    val category: String
    val defaultBackground: Int
    val legacy: Boolean
        get() = false

    /** Per-animation config schema. Empty means no scene settings, no gear icon. */
    val settings: List<SettingSpec>
        get() = emptyList()

    /** Existing entry point — animations without scene config override this one. */
    fun initialize(width: Int, height: Int, scale: Float): Any

    /**
     * Scene-config-aware entry point. Default delegates to the plain overload
     * so existing animations need no changes. Animations with scene settings
     * override this and read [config] values.
     */
    fun initialize(
        width: Int,
        height: Int,
        scale: Float,
        config: SceneConfig
    ): Any = initialize(width, height, scale)

    fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        state: Any,
        timeMs: Long,
        speed: Float,
        scale: Float,
        tintColor: Int?
    )
}
