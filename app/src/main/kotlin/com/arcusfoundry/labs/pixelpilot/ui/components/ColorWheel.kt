package com.arcusfoundry.labs.pixelpilot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Circular hue/saturation picker. Hue along the wheel's angle, saturation along
 * the radius. Lightness is fixed at 0.55 (good visibility on dark and light).
 * Emits ARGB color via onColorChange.
 */
@Composable
fun ColorWheel(
    initialColor: Int,
    modifier: Modifier = Modifier,
    onColorChange: (Int) -> Unit
) {
    val (iH, iS) = remember(initialColor) { argbToHueSat(initialColor) }
    var hue by remember { mutableStateOf(iH) }
    var saturation by remember { mutableStateOf(iS) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val (h, s) = offsetToHueSat(offset, size.width.toFloat(), size.height.toFloat())
                        if (h != null && s != null) {
                            hue = h; saturation = s
                            onColorChange(hslToArgb(h, s, 0.55f))
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val (h, s) = offsetToHueSat(change.position, size.width.toFloat(), size.height.toFloat())
                        if (h != null && s != null) {
                            hue = h; saturation = s
                            onColorChange(hslToArgb(h, s, 0.55f))
                        }
                    }
                }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = min(size.width, size.height) / 2f - 8f

            // Hue sweep ring.
            val hueColors = (0..12).map { Color.hsv((it * 30f) % 360f, 1f, 1f) }
            drawCircle(
                brush = Brush.sweepGradient(hueColors, Offset(cx, cy)),
                radius = radius,
                center = Offset(cx, cy)
            )

            // Saturation falloff (white center → transparent edge).
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx, cy)
            )

            // Current selection marker.
            val markerR = radius * saturation
            val hueRad = Math.toRadians(hue.toDouble()).toFloat()
            val mx = cx + cos(hueRad) * markerR
            val my = cy + sin(hueRad) * markerR
            drawCircle(color = Color.Black, radius = 10f, center = Offset(mx, my), style = Stroke(2f))
            drawCircle(color = Color.White, radius = 7f, center = Offset(mx, my), style = Stroke(2f))
        }
    }
}

private fun offsetToHueSat(offset: Offset, w: Float, h: Float): Pair<Float?, Float?> {
    val cx = w / 2f
    val cy = h / 2f
    val radius = min(w, h) / 2f - 8f
    val dx = offset.x - cx
    val dy = offset.y - cy
    val dist = sqrt(dx * dx + dy * dy)
    if (dist > radius) return null to null
    val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    val hue = ((angle % 360f) + 360f) % 360f
    val saturation = (dist / radius).coerceIn(0f, 1f)
    return hue to saturation
}

private fun argbToHueSat(argb: Int): Pair<Float, Float> {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val hue = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    val hueNorm = ((hue % 360f) + 360f) % 360f
    val lightness = (max + min) / 2f
    val sat = if (delta == 0f) 0f else delta / (1f - kotlin.math.abs(2f * lightness - 1f))
    return hueNorm to sat.coerceIn(0f, 1f)
}

private fun hslToArgb(h: Float, s: Float, l: Float): Int {
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val hPrime = h / 60f
    val x = c * (1f - kotlin.math.abs(hPrime % 2f - 1f))
    val (r1, g1, b1) = when {
        hPrime < 1f -> Triple(c, x, 0f)
        hPrime < 2f -> Triple(x, c, 0f)
        hPrime < 3f -> Triple(0f, c, x)
        hPrime < 4f -> Triple(0f, x, c)
        hPrime < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = l - c / 2f
    val r = ((r1 + m) * 255f).toInt().coerceIn(0, 255)
    val g = ((g1 + m) * 255f).toInt().coerceIn(0, 255)
    val b = ((b1 + m) * 255f).toInt().coerceIn(0, 255)
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}
