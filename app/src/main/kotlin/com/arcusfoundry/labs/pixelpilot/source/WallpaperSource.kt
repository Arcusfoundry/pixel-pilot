package com.arcusfoundry.labs.pixelpilot.source

sealed class WallpaperSource {
    data class Procedural(val animationId: String) : WallpaperSource()
    data class Video(val uri: String) : WallpaperSource()
    data class LocalFile(val path: String) : WallpaperSource()

    fun serialize(): String = when (this) {
        is Procedural -> "proc:$animationId"
        is Video -> "video:$uri"
        is LocalFile -> "file:$path"
    }

    companion object {
        fun parse(raw: String?): WallpaperSource? {
            if (raw.isNullOrBlank()) return null
            val idx = raw.indexOf(':')
            if (idx < 0) return null
            val kind = raw.substring(0, idx)
            val value = raw.substring(idx + 1)
            return when (kind) {
                "proc" -> Procedural(value)
                "video" -> Video(value)
                "file" -> LocalFile(value)
                else -> null
            }
        }
    }
}
