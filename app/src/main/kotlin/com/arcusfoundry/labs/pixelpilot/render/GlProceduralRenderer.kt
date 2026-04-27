package com.arcusfoundry.labs.pixelpilot.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import com.arcusfoundry.labs.pixelpilot.prefs.WallpaperPreferences
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Procedural wallpaper renderer via GL.
 *
 * Animation.draw() still produces frames into a back-buffer Bitmap (the
 * preserved-contents semantics that animations like Matrix Rain rely on).
 * Each frame, the back-buffer is uploaded to a GL_TEXTURE_2D and drawn
 * to an EGL surface bound to the wallpaper Surface. Effects (dim) apply
 * in the fragment shader.
 *
 * Why GL instead of the simpler lockCanvas/unlockCanvasAndPost path:
 * lockCanvas binds a CPU producer to the wallpaper Surface's BufferQueue,
 * which cannot be cleanly handed off to MediaCodec/EGL when the user
 * switches from a procedural scene to a video. Going through EGL here
 * makes the procedural→video transition an EGL→EGL handoff, which the
 * BufferQueue tolerates.
 */
class GlProceduralRenderer(
    private val animation: Animation,
    private val prefs: WallpaperPreferences? = null
) : WallpaperRenderer {

    private var thread: HandlerThread? = null
    private var handler: Handler? = null

    @Volatile private var params: RenderParams = RenderParams.DEFAULTS
    @Volatile private var visible: Boolean = false
    private var width: Int = 0
    private var height: Int = 0
    private val startUptimeMillis = SystemClock.uptimeMillis()
    private var lastInitScale: Float = 1f

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var program: Int = 0
    private var aPositionLoc: Int = -1
    private var aTexCoordLoc: Int = -1
    private var uTintLoc: Int = -1
    private var uTintStrengthLoc: Int = -1
    private var uDimLoc: Int = -1
    private var bitmapTextureId: Int = 0

    private var backBuffer: Bitmap? = null
    private var backCanvas: Canvas? = null
    private var animationState: Any? = null
    private var firstUploadDone: Boolean = false

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
            // Y-flipped: GL textures are bottom-up, our Bitmap is top-down.
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
        )
    )

    override fun attach(surface: Surface, width: Int, height: Int) {
        this.width = width
        this.height = height
        this.lastInitScale = params.scale

        val t = HandlerThread("pixelpilot-gl-procedural").apply { start() }
        thread = t
        val h = Handler(t.looper)
        handler = h

        h.post {
            try {
                initEgl(surface)
                initGl()
                initBackBuffer()
                animationState = animation.initialize(width, height, params.scale, currentSceneConfig())
                scheduleNextFrame()
            } catch (e: Throwable) {
                Log.e(TAG, "init failed", e)
            }
        }
    }

    override fun setVisible(visible: Boolean) {
        this.visible = visible
        if (visible) handler?.post { scheduleNextFrame() }
    }

    override fun updateParams(params: RenderParams) {
        this.params = params
        if (kotlin.math.abs(params.scale - lastInitScale) > 0.15f) {
            lastInitScale = params.scale
            handler?.post {
                animationState = animation.initialize(width, height, params.scale, currentSceneConfig())
                backCanvas?.drawColor(animation.defaultBackground)
            }
        }
    }

    fun reinitializeWithSceneConfig() {
        handler?.post {
            animationState = animation.initialize(width, height, params.scale, currentSceneConfig())
            backCanvas?.drawColor(animation.defaultBackground)
        }
    }

    override fun detach() {
        val h = handler ?: return
        val t = thread ?: return
        handler = null
        thread = null
        val latch = java.util.concurrent.CountDownLatch(1)
        val ok = h.post {
            try {
                releaseGl()
                releaseEgl()
                backBuffer?.recycle()
                backBuffer = null
                backCanvas = null
                animationState = null
            } finally {
                latch.countDown()
            }
        }
        if (ok) {
            try {
                latch.await(1, java.util.concurrent.TimeUnit.SECONDS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        t.quitSafely()
    }

    override fun release() { detach() }

    // ---------- GL thread only below ----------

    private fun scheduleNextFrame() {
        val h = handler ?: return
        if (!visible) return
        h.post { renderFrame() }
    }

    private fun renderFrame() {
        if (!visible || handler == null) return
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) return
        val bb = backBuffer ?: return
        val bc = backCanvas ?: return
        val state = animationState ?: return

        try {
            val t = SystemClock.uptimeMillis() - startUptimeMillis
            val tint = effectiveTintColor()
            animation.draw(bc, width, height, state, t, params.speed, params.scale, tint)
        } catch (e: Throwable) {
            Log.w(TAG, "animation.draw failed: ${e.message}")
        }

        try {
            GLES20.glViewport(0, 0, width, height)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(program)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bitmapTextureId)
            if (firstUploadDone) {
                GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bb)
            } else {
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bb, 0)
                firstUploadDone = true
            }

            val tintColor = effectiveTintColor()
            val tintR = tintColor?.let { android.graphics.Color.red(it) / 255f } ?: 0f
            val tintG = tintColor?.let { android.graphics.Color.green(it) / 255f } ?: 0f
            val tintB = tintColor?.let { android.graphics.Color.blue(it) / 255f } ?: 0f
            val tintStrength = if (tintColor != null) params.tintStrength.coerceIn(0f, 1f) else 0f
            val dim = params.dim.coerceIn(0f, 1f)

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
        } catch (e: Throwable) {
            Log.e(TAG, "draw failed", e)
        }

        // Schedule next frame at ~60fps cadence by posting back to handler.
        handler?.postDelayed({ renderFrame() }, 16L)
    }

    private fun effectiveTintColor(): Int? = when (val mode = params.tint) {
        TintMode.None -> null
        is TintMode.Static -> mode.color
        is TintMode.Rainbow -> {
            val cycleMs = (mode.cycleSeconds * 1000f).coerceAtLeast(100f)
            val phase = ((SystemClock.uptimeMillis() - startUptimeMillis) % cycleMs.toLong()) / cycleMs
            ColorUtil.hsl(phase * 360f, 0.7f, 0.55f)
        }
    }

    private fun currentSceneConfig() =
        prefs?.sceneConfig(animation.id, animation.settings) ?: SceneConfig.EMPTY

    private fun initEgl(outputSurface: Surface) {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(eglDisplay != EGL14.EGL_NO_DISPLAY) { "eglGetDisplay failed" }
        val v = IntArray(2)
        check(EGL14.eglInitialize(eglDisplay, v, 0, v, 1)) { "eglInitialize failed" }

        val config = chooseConfig(true) ?: chooseConfig(false)
            ?: error("eglChooseConfig: no compatible config")

        val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            error("eglCreateContext failed: 0x${EGL14.eglGetError().toString(16)}")
        }

        eglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay, config, outputSurface, intArrayOf(EGL14.EGL_NONE), 0
        )
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            val firstErr = EGL14.eglGetError()
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            eglContext = EGL14.EGL_NO_CONTEXT
            val fallback = chooseConfig(false)
                ?: error("eglCreateWindowSurface failed: 0x${firstErr.toString(16)} (no fallback)")
            eglContext = EGL14.eglCreateContext(eglDisplay, fallback, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
            check(eglContext != EGL14.EGL_NO_CONTEXT) { "eglCreateContext (fallback)" }
            eglSurface = EGL14.eglCreateWindowSurface(
                eglDisplay, fallback, outputSurface, intArrayOf(EGL14.EGL_NONE), 0
            )
            if (eglSurface == EGL14.EGL_NO_SURFACE) {
                error("eglCreateWindowSurface failed (both configs): 0x${EGL14.eglGetError().toString(16)}")
            }
        }

        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            error("eglMakeCurrent failed: 0x${EGL14.eglGetError().toString(16)}")
        }
    }

    private fun chooseConfig(withAlpha: Boolean): EGLConfig? {
        val attribs = if (withAlpha) intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_NONE
        ) else intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        return if (EGL14.eglChooseConfig(eglDisplay, attribs, 0, configs, 0, 1, numConfigs, 0)
            && numConfigs[0] > 0
        ) configs[0] else null
    }

    private fun initGl() {
        program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        aPositionLoc = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoordLoc = GLES20.glGetAttribLocation(program, "aTexCoord")
        uTintLoc = GLES20.glGetUniformLocation(program, "uTint")
        uTintStrengthLoc = GLES20.glGetUniformLocation(program, "uTintStrength")
        uDimLoc = GLES20.glGetUniformLocation(program, "uDim")

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        bitmapTextureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bitmapTextureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
    }

    private fun initBackBuffer() {
        val bb = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val bc = Canvas(bb)
        bc.drawColor(animation.defaultBackground)
        backBuffer = bb
        backCanvas = bc
        firstUploadDone = false
    }

    private fun releaseGl() {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
        if (bitmapTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(bitmapTextureId), 0)
            bitmapTextureId = 0
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
        }
        eglSurface = EGL14.EGL_NO_SURFACE
        eglContext = EGL14.EGL_NO_CONTEXT
        eglDisplay = EGL14.EGL_NO_DISPLAY
    }

    companion object {
        private const val TAG = "GlProceduralRenderer"

        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTexture;
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
    }
}
