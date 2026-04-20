package com.arcusfoundry.labs.pixelpilot.render

import android.graphics.Canvas

interface Animation {
    val id: String
    val displayName: String
    val category: String
    val defaultBackground: Int
    val legacy: Boolean
        get() = false

    fun initialize(width: Int, height: Int, scale: Float): Any

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
