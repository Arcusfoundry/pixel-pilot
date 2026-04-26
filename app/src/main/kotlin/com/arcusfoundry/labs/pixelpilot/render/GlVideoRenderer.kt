package com.arcusfoundry.labs.pixelpilot.render

import android.content.Context
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.net.Uri
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.arcusfoundry.labs.pixelpilot.prefs.WallpaperPreferences
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Video wallpaper renderer via GL intermediary.
 *
 * ExoPlayer writes frames to a SurfaceTexture-backed Surface. On each
 * frame-available callback, we bind the SurfaceTexture's external OES
 * texture and draw it to an EGLSurface targeting the wallpaper Surface.
 * Effects (dim / tint / rainbow) are applied in the fragment shader.
 *
 * Direct ExoPlayer.setVideoSurface(wallpaperSurface) does not work reliably
 * on WallpaperService SurfaceHolders — frames decode but never paint. The
 * SurfaceTexture → EGL → blit pattern is the one Android's own live
 * wallpaper samples use.
 */
@OptIn(UnstableApi::class)
class GlVideoRenderer(
    private val context: Context,
    private val sourceUri: String
) : WallpaperRenderer {

    private var thread: HandlerThread? = null
    private var handler: Handler? = null

    @Volatile private var params: RenderParams = RenderParams.DEFAULTS
    @Volatile private var visible: Boolean = false
    @Volatile private var videoWidth: Int = 0
    @Volatile private var videoHeight: Int = 0
    private var surfaceWidth: Int = 0
    private var surfaceHeight: Int = 0
    private val startUptimeMillis = SystemClock.uptimeMillis()

    // GL state, all touched only on the GL thread.
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var program: Int = 0
    private var aPositionLoc: Int = -1
    private var aTexCoordLoc: Int = -1
    private var uTexMatrixLoc: Int = -1
    private var uTintLoc: Int = -1
    private var uTintStrengthLoc: Int = -1
    private var uDimLoc: Int = -1
    private var oesTextureId: Int = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var videoInputSurface: Surface? = null
    private var player: ExoPlayer? = null

    private val positionBuffer: FloatBuffer = floatBuffer(
        floatArrayOf(
            -1f, -1f,
             1f, -1f,
            -1f,  1f,
             1f,  1f
        )
    )
    private val texCoordBuffer: FloatBuffer = floatBuffer(
        floatArrayOf(
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f
        )
    )
    private val stMatrix = FloatArray(16)
    private val cropMatrix = FloatArray(16)
    private val combinedMatrix = FloatArray(16)

    override fun attach(surface: Surface, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height

        val t = HandlerThread("pixelpilot-gl-video").apply { start() }
        thread = t
        val h = Handler(t.looper)
        handler = h

        h.post {
            try {
                initEgl(surface)
                initGl()
                initVideoPipeline()
            } catch (e: Throwable) {
                Log.e(TAG, "init failed", e)
                WallpaperPreferences(context).lastVideoError =
                    "GL_INIT_FAIL: ${e.javaClass.simpleName}: ${e.message}"
            }
        }
    }

    override fun setVisible(visible: Boolean) {
        this.visible = visible
        handler?.post {
            player?.playWhenReady = visible
        }
    }

    override fun updateParams(params: RenderParams) {
        this.params = params
    }

    override fun detach() {
        val h = handler ?: return
        val t = thread ?: return
        handler = null
        thread = null
        h.post {
            releaseVideoPipeline()
            releaseGl()
            releaseEgl()
            t.quitSafely()
        }
    }

    override fun release() { detach() }

    // ---------- GL thread only below ----------

    private fun initEgl(outputSurface: Surface) {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(eglDisplay != EGL14.EGL_NO_DISPLAY) { "eglGetDisplay failed" }

        val version = IntArray(2)
        check(EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) { "eglInitialize failed" }

        // EGL_SURFACE_TYPE=EGL_WINDOW_BIT is required to get a config that
        // supports eglCreateWindowSurface. Omitting it lets EGL pick any
        // compatible config — sometimes one that supports window surfaces,
        // sometimes not. That's the source of the "video worked sometimes,
        // then died" reports.
        val configAttribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)
            || numConfigs[0] <= 0
        ) {
            error("eglChooseConfig failed: 0x${EGL14.eglGetError().toString(16)}")
        }

        val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(
            eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0
        )
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            error("eglCreateContext failed: 0x${EGL14.eglGetError().toString(16)}")
        }

        eglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay, configs[0], outputSurface, intArrayOf(EGL14.EGL_NONE), 0
        )
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            error("eglCreateWindowSurface failed: 0x${EGL14.eglGetError().toString(16)}")
        }

        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            error("eglMakeCurrent failed: 0x${EGL14.eglGetError().toString(16)}")
        }
    }

    private fun initGl() {
        program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        aPositionLoc = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoordLoc = GLES20.glGetAttribLocation(program, "aTexCoord")
        uTexMatrixLoc = GLES20.glGetUniformLocation(program, "uTexMatrix")
        uTintLoc = GLES20.glGetUniformLocation(program, "uTint")
        uTintStrengthLoc = GLES20.glGetUniformLocation(program, "uTintStrength")
        uDimLoc = GLES20.glGetUniformLocation(program, "uDim")

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        oesTextureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
    }

    private fun initVideoPipeline() {
        val st = SurfaceTexture(oesTextureId).apply {
            setDefaultBufferSize(
                if (surfaceWidth > 0) surfaceWidth else 1280,
                if (surfaceHeight > 0) surfaceHeight else 720
            )
            setOnFrameAvailableListener({
                handler?.post { drawFrame() }
            }, handler)
        }
        surfaceTexture = st
        val inputSurface = Surface(st)
        videoInputSurface = inputSurface

        val dsFactory = DefaultDataSource.Factory(context)
        val msFactory = DefaultMediaSourceFactory(dsFactory)
        val p = ExoPlayer.Builder(context)
            .setMediaSourceFactory(msFactory)
            .build().apply {
                setVideoSurface(inputSurface)
                volume = 0f
                repeatMode = Player.REPEAT_MODE_ALL
                setMediaItem(MediaItem.fromUri(toPlayableUri(sourceUri)))
                addListener(PlayerEvents())
                prepare()
                playWhenReady = visible
            }
        player = p
    }

    private fun drawFrame() {
        if (eglDisplay == EGL14.EGL_NO_DISPLAY || eglSurface == EGL14.EGL_NO_SURFACE) return
        val st = surfaceTexture ?: return

        try {
            st.updateTexImage()
            st.getTransformMatrix(stMatrix)
        } catch (e: Throwable) {
            Log.w(TAG, "updateTexImage failed: ${e.message}")
            return
        }

        computeCropMatrix(cropMatrix)
        Matrix.multiplyMM(combinedMatrix, 0, cropMatrix, 0, stMatrix, 0)

        val tintColor = effectiveTintColor()
        val tintR = tintColor?.let { Color.red(it) / 255f } ?: 0f
        val tintG = tintColor?.let { Color.green(it) / 255f } ?: 0f
        val tintB = tintColor?.let { Color.blue(it) / 255f } ?: 0f
        val tintStrength = if (tintColor != null) params.tintStrength.coerceIn(0f, 1f) else 0f
        val dim = params.dim.coerceIn(0f, 1f)

        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, combinedMatrix, 0)
        GLES20.glUniform3f(uTintLoc, tintR, tintG, tintB)
        GLES20.glUniform1f(uTintStrengthLoc, tintStrength)
        GLES20.glUniform1f(uDimLoc, dim)

        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, positionBuffer)
        GLES20.glEnableVertexAttribArray(aTexCoordLoc)
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPositionLoc)
        GLES20.glDisableVertexAttribArray(aTexCoordLoc)

        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    /**
     * Builds a 4x4 matrix that transforms 0..1 tex coords into the right
     * crop/fit region. Operates in "texture space": coords are translated
     * and scaled around the 0.5 center point.
     *
     * params.scale >= 1 : center-crop (matches SCALE_TO_FIT_WITH_CROPPING)
     * params.scale <  1 : fit (matches SCALE_TO_FIT, letterboxes)
     */
    private fun computeCropMatrix(out: FloatArray) {
        Matrix.setIdentityM(out, 0)
        val vw = videoWidth
        val vh = videoHeight
        if (vw <= 0 || vh <= 0 || surfaceWidth <= 0 || surfaceHeight <= 0) return

        val surfaceAspect = surfaceWidth.toFloat() / surfaceHeight.toFloat()
        val videoAspect = vw.toFloat() / vh.toFloat()
        val crop = params.scale >= 1f

        var scaleX = 1f
        var scaleY = 1f
        if (crop) {
            // Center-crop: scale UVs inward so more of the frame fills each axis.
            if (videoAspect > surfaceAspect) {
                // Video wider than surface → crop left/right (shrink U range).
                scaleX = surfaceAspect / videoAspect
            } else {
                // Video taller than surface → crop top/bottom (shrink V range).
                scaleY = videoAspect / surfaceAspect
            }
        } else {
            // Fit: scale UVs outward so entire frame fits (extra area rendered as
            // transparent black since we clear to black).
            if (videoAspect > surfaceAspect) {
                scaleY = videoAspect / surfaceAspect
            } else {
                scaleX = surfaceAspect / videoAspect
            }
        }

        // Build: translate to origin, scale, translate back. Matrix column-major.
        Matrix.translateM(out, 0, 0.5f, 0.5f, 0f)
        Matrix.scaleM(out, 0, scaleX, scaleY, 1f)
        Matrix.translateM(out, 0, -0.5f, -0.5f, 0f)
    }

    private fun effectiveTintColor(): Int? = when (val m = params.tint) {
        TintMode.None -> null
        is TintMode.Static -> m.color
        is TintMode.Rainbow -> {
            val cycleMs = (m.cycleSeconds * 1000f).coerceAtLeast(100f)
            val phase = ((SystemClock.uptimeMillis() - startUptimeMillis) % cycleMs.toLong()) / cycleMs
            ColorUtil.hsl(phase * 360f, 0.7f, 0.55f)
        }
    }

    private fun releaseVideoPipeline() {
        try { player?.release() } catch (_: Throwable) {}
        player = null
        try { videoInputSurface?.release() } catch (_: Throwable) {}
        videoInputSurface = null
        try { surfaceTexture?.release() } catch (_: Throwable) {}
        surfaceTexture = null
    }

    private fun releaseGl() {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
        if (oesTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(oesTextureId), 0)
            oesTextureId = 0
        }
    }

    private fun releaseEgl() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(
                eglDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
            if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(eglDisplay, eglSurface)
            if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(eglDisplay, eglContext)
            // Don't eglTerminate — the display is process-shared with other EGL
            // clients. eglMakeCurrent + destroy is sufficient cleanup for our
            // resources.
        }
        eglSurface = EGL14.EGL_NO_SURFACE
        eglContext = EGL14.EGL_NO_CONTEXT
        eglDisplay = EGL14.EGL_NO_DISPLAY
    }

    private inner class PlayerEvents : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            val msg = "${error.errorCodeName} (${error.errorCode}): ${error.message}"
            Log.e(TAG, "PlaybackError $msg", error)
            WallpaperPreferences(context).lastVideoError = msg
        }

        override fun onPlaybackStateChanged(state: Int) {
            val name = when (state) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "state_$state"
            }
            val now = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date())
            WallpaperPreferences(context).lastVideoState = "$now $name"
            if (state == Player.STATE_READY) {
                WallpaperPreferences(context).lastVideoError = null
            }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            videoWidth = videoSize.width
            videoHeight = videoSize.height
            handler?.post {
                surfaceTexture?.setDefaultBufferSize(videoWidth, videoHeight)
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            val summary = if (tracks.groups.isEmpty()) "NO_TRACKS"
            else tracks.groups.joinToString(",") { g ->
                "${g.type}(len=${g.length}, sel=${g.isSelected})"
            }
            val now = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date())
            WallpaperPreferences(context).lastVideoState = "$now TRACKS $summary"
        }
    }

    companion object {
        private const val TAG = "GlVideoRenderer"

        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec4 aTexCoord;
            uniform mat4 uTexMatrix;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = (uTexMatrix * aTexCoord).xy;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES uTexture;
            uniform vec3 uTint;
            uniform float uTintStrength;
            uniform float uDim;
            varying vec2 vTexCoord;
            void main() {
                vec4 color = texture2D(uTexture, vTexCoord);
                float lum = dot(color.rgb, vec3(0.299, 0.587, 0.114));
                vec3 tinted = mix(color.rgb, lum * uTint, uTintStrength);
                tinted *= (1.0 - uDim);
                gl_FragColor = vec4(tinted, color.a);
            }
        """

        private fun buildProgram(vertexSrc: String, fragmentSrc: String): Int {
            val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSrc)
            val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc)
            val prog = GLES20.glCreateProgram()
            check(prog != 0) { "glCreateProgram failed" }
            GLES20.glAttachShader(prog, vs)
            GLES20.glAttachShader(prog, fs)
            GLES20.glLinkProgram(prog)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES20.GL_TRUE) {
                val log = GLES20.glGetProgramInfoLog(prog)
                GLES20.glDeleteProgram(prog)
                error("program link failed: $log")
            }
            GLES20.glDeleteShader(vs)
            GLES20.glDeleteShader(fs)
            return prog
        }

        private fun compileShader(type: Int, src: String): Int {
            val shader = GLES20.glCreateShader(type)
            check(shader != 0) { "glCreateShader failed" }
            GLES20.glShaderSource(shader, src)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] != GLES20.GL_TRUE) {
                val log = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                error("shader compile failed: $log")
            }
            return shader
        }

        private fun floatBuffer(data: FloatArray): FloatBuffer {
            val bb = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder())
            val fb = bb.asFloatBuffer()
            fb.put(data).position(0)
            return fb
        }

        private fun toPlayableUri(source: String): Uri {
            val parsed = Uri.parse(source)
            return if (parsed.scheme.isNullOrEmpty()) Uri.fromFile(File(source)) else parsed
        }
    }
}
