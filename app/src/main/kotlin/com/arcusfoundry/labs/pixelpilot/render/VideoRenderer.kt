package com.arcusfoundry.labs.pixelpilot.render

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.Surface
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.RgbMatrix
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

/**
 * Renders MP4 (or any ExoPlayer-supported format) wallpapers. Applies dim, tint, and
 * rainbow cycling via RgbMatrix video effects. Scale is applied via VideoScalingMode.
 */
@OptIn(UnstableApi::class)
class VideoRenderer(private val context: Context, private val sourceUri: String) : WallpaperRenderer {

    private var player: ExoPlayer? = null
    private var params = RenderParams.DEFAULTS
    private var visible = false

    override fun attach(surface: Surface, width: Int, height: Int) {
        val mediaUri = toPlayableUri(sourceUri)
        val newPlayer = ExoPlayer.Builder(context).build().apply {
            setVideoSurface(surface)
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ALL
            setMediaItem(MediaItem.fromUri(mediaUri))
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "video playback error", error)
                }
            })
            prepare()
            playWhenReady = visible
        }
        player = newPlayer
        applyParams(newPlayer)
    }

    override fun setVisible(visible: Boolean) {
        this.visible = visible
        player?.playWhenReady = visible
    }

    override fun updateParams(params: RenderParams) {
        this.params = params
        player?.let { applyParams(it) }
    }

    override fun detach() {
        player?.release()
        player = null
    }

    override fun release() { detach() }

    private fun applyParams(p: ExoPlayer) {
        p.playbackParameters = PlaybackParameters(params.speed.coerceIn(0.25f, 3f))
        p.setVideoScalingMode(
            if (params.scale >= 1f) C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            else C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        )
        // Only touch the effects pipeline when there's an actual effect to apply.
        // Passing an empty list disrupts frame output on some Media3 paths,
        // leaving the wallpaper surface blank.
        val effects = buildEffects()
        if (effects.isNotEmpty()) {
            try {
                p.setVideoEffects(effects)
            } catch (e: Throwable) {
                Log.w(TAG, "setVideoEffects unavailable: ${e.message}")
            }
        }
    }

    private fun buildEffects(): List<androidx.media3.common.Effect> {
        val effects = mutableListOf<androidx.media3.common.Effect>()
        val tintColor: Int? = when (val mode = params.tint) {
            TintMode.None -> null
            is TintMode.Static -> mode.color
            is TintMode.Rainbow -> {
                val ms = (System.currentTimeMillis() % (mode.cycleSeconds * 1000f).toLong()).toFloat()
                ColorUtil.hsl((ms / (mode.cycleSeconds * 1000f)) * 360f, 0.7f, 0.55f)
            }
        }
        val dim = params.dim.coerceIn(0f, 1f)
        val strength = params.tintStrength.coerceIn(0f, 1f)
        if (dim > 0f || tintColor != null) {
            effects.add(ColorAdjust(tintColor, strength, dim))
        }
        return effects
    }

    /**
     * Combines tint-toward-color (via desaturate-then-multiply) with dim (overall brightness
     * reduction) as a single 4x4 RgbMatrix applied per frame.
     */
    @OptIn(UnstableApi::class)
    private class ColorAdjust(
        private val tint: Int?,
        private val tintStrength: Float,
        private val dim: Float
    ) : RgbMatrix {
        override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray {
            val tr = tint?.let { Color.red(it) / 255f } ?: 1f
            val tg = tint?.let { Color.green(it) / 255f } ?: 1f
            val tb = tint?.let { Color.blue(it) / 255f } ?: 1f
            val s = if (tint != null) tintStrength else 0f
            val lr = 0.299f; val lg = 0.587f; val lb = 0.114f
            val brightness = 1f - dim
            val m = FloatArray(16)
            // Column-major 4x4 for RgbMatrix. Channel mix = (1-s)*I + s*(luminance-to-tint)
            // Identity-weighted diagonal with off-diagonal from luminance mix, all scaled by brightness.
            m[0]  = ((1f - s) * 1f + s * lr * tr) * brightness
            m[1]  = (s * lr * tg) * brightness
            m[2]  = (s * lr * tb) * brightness
            m[3]  = 0f

            m[4]  = (s * lg * tr) * brightness
            m[5]  = ((1f - s) * 1f + s * lg * tg) * brightness
            m[6]  = (s * lg * tb) * brightness
            m[7]  = 0f

            m[8]  = (s * lb * tr) * brightness
            m[9]  = (s * lb * tg) * brightness
            m[10] = ((1f - s) * 1f + s * lb * tb) * brightness
            m[11] = 0f

            m[12] = 0f
            m[13] = 0f
            m[14] = 0f
            m[15] = 1f
            return m
        }
    }

    companion object {
        private const val TAG = "VideoRenderer"

        /**
         * Normalizes a source string into a URI ExoPlayer can open:
         * - content:// URIs (SAF picks) pass through unchanged
         * - file:// URIs pass through
         * - http(s):// URIs pass through
         * - bare absolute paths are wrapped as Uri.fromFile to get a proper
         *   file:// URI. Bare paths go unrecognized by ExoPlayer's default
         *   data sources, which is why local files downloaded from YouTube
         *   weren't playing.
         */
        private fun toPlayableUri(source: String): Uri {
            val parsed = Uri.parse(source)
            return if (parsed.scheme.isNullOrEmpty()) {
                Uri.fromFile(File(source))
            } else {
                parsed
            }
        }
    }
}
