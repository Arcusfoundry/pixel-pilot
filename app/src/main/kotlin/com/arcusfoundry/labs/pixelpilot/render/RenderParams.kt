package com.arcusfoundry.labs.pixelpilot.render

sealed class TintMode {
    data object None : TintMode()
    data class Static(val color: Int) : TintMode()
    data class Rainbow(val cycleSeconds: Float = 20f) : TintMode()
}

data class RenderParams(
    val speed: Float = 1f,
    val scale: Float = 1f,
    val dim: Float = 0f,
    val tint: TintMode = TintMode.None,
    val tintStrength: Float = 0.6f
) {
    companion object {
        val DEFAULTS = RenderParams()
    }
}
