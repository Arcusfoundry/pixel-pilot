package com.arcusfoundry.labs.pixelpilot.render.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.ColorUtil
import kotlin.math.max
import kotlin.random.Random

object StarfieldAnimation : Animation {
    override val id = "starfield"
    override val displayName = "Starfield"
    override val category = "Nature"
    override val defaultBackground = Color.rgb(0, 0, 5)

    private class Star(var x: Float, var y: Float, var z: Float)
    private class State(val stars: Array<Star>, val bg: Paint, val star: Paint)

    override fun initialize(width: Int, height: Int, scale: Float): Any {
        val stars = Array(200) {
            Star(
                Random.nextFloat() * width - width / 2f,
                Random.nextFloat() * height - height / 2f,
                Random.nextFloat() * 1000f
            )
        }
        val bg = Paint().apply { color = defaultBackground }
        val star = Paint().apply { isAntiAlias = true }
        return State(stars, bg, star)
    }

    override fun draw(
        canvas: Canvas,
        width: Int,
        height: Int,
        state: Any,
        timeMs: Long,
        speed: Float,
        scale: Float,
        tintColor: Int?
    ) {
        val s = state as State
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), s.bg)
        val base = tintColor ?: Color.WHITE
        for (st in s.stars) {
            st.z -= 0.5f * speed
            if (st.z <= 0f) {
                st.z = 1000f
                st.x = Random.nextFloat() * width - width / 2f
                st.y = Random.nextFloat() * height - height / 2f
            }
            val sx = (st.x / st.z) * 300f + width / 2f
            val sy = (st.y / st.z) * 300f + height / 2f
            val r = max(0f, (1f - st.z / 1000f) * 2f * scale)
            s.star.color = ColorUtil.withAlpha(base, 1f - st.z / 1000f)
            canvas.drawCircle(sx, sy, r, s.star)
        }
    }
}
