package com.myxoz.life.subscreens.wrapped

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

// Full Vibecode
@Composable
fun ReactiveScreensaverBackground(
    pressPos: Offset,
    pressHeat: Float,
    backgroundAccentColor: Color,

    phaseOffset: Float = 0f,      // [-1.0f..1.0f]  Phase shift (1.0 = +1 full cycle).
    driftScale: Float = 1f,       // [0.0f..2.5f]   0 = no drift, >1 = stronger drift.
    driftHarmonics: Float = 0.35f,// [0.0f..1.0f]   0 = simple orbit, 1 = more “complex” orbit.

    ribbonAmp: Float = 2f,        // [0.0f..3.0f]   0 = fully flat ribbons.
    ribbonDetail: Float = 0.35f,  // [0.0f..1.0f]   Blend low/high harmonics (still loops).
    ribbonSpike: Float = 0f       // [-4.0f..4.0f]  <0 rounder, 0 sine, >0 spikier.
) {
    val inf = rememberInfiniteTransition(label = "bg")
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.92f)
    ) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        val tau = 2f * PI.toFloat()
        val ph = (phase + phaseOffset).let { it - floor(it) } // keep it in [0,1)
        val a1 = tau * ph
        val a2 = 2f * a1
        val a = tau * phase

        val ds = driftScale.coerceIn(0f, 2.5f)
        val dh = driftHarmonics.coerceIn(0f, 1f)

        val drift = Offset(
            x = (lerp(cos(a1), cos(a2), dh)) * w * 0.12f * ds,
            y = (lerp(sin(a1), sin(a2), dh)) * h * 0.10f * ds
        )

        val hot = Color(0x80FFFFFF).compositeOver(backgroundAccentColor).copy(alpha = 0.5f + 0.10f * pressHeat)
        val cold = backgroundAccentColor.copy(alpha = 0.30f)
        val dark = backgroundAccentColor.copy(alpha = 0.1f)

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(hot, cold, dark),
                center = center + drift,
                radius = min(w, h) * 0.80f
            ),
            size = Size(w, h)
        )

        val ribbons = 15
        val points = 120
        val ampBase = min(w, h) * (0.05f + 0.03f * pressHeat)

        repeat(ribbons) { rIndex ->
            val k = rIndex / (ribbons - 1f)
            val yBase = lerp(h * 0.18f, h * 0.82f, k)

            val path = Path()
            for (i in 0..points) {
                val t = i / points.toFloat()
                val x = t * w

                val rd = ribbonDetail.coerceIn(0f, 1f)
                val rs = ribbonSpike.coerceIn(-4f, 4f)
                val ra = ribbonAmp.coerceIn(0f, 3f)

                val base1 = shapedSin(tau * (t * 1.2f) + a1 + rIndex * 0.7f, rs)
                val base2 = shapedSin(tau * (t * 1.2f) + a2 + rIndex * 0.7f, rs) // 2x harmonic

                val hi1 = shapedSin(tau * (t * 2.3f) + 2f * a1 + rIndex * 1.9f, rs)
                val hi2 = shapedSin(tau * (t * 2.3f) + 2f * a2 + rIndex * 1.9f, rs)

                val wave = (lerp(base1, base2, rd) + 0.6f * lerp(hi1, hi2, rd)) / 1.6f

                val warp =
                    if (pressPos != Offset.Unspecified) {
                        val dx = (x - pressPos.x) / w
                        val dy = (yBase - pressPos.y) / h
                        val d2 = dx * dx + dy * dy
                        (0.9f * pressHeat) * expFalloff(d2 * 6.5f) * sin(tau * (2f * phase))
                    } else 0f

                val y = yBase + (wave + warp) * (ampBase * ra) * (0.55f + 0.65f * k)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            val c = Color(
                red = lerp(0.30f, 0.75f, k),
                green = lerp(0.45f, 0.90f, 1f - k),
                blue = 1f,
                alpha = 0.10f + 0.05f * (1f - k)
            )
            drawPath(path = path, color = c, style = Stroke(width = lerp(2f, 2.4f, 1f - k)))
        }
    }
}
private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
private fun shapedSin(angle: Float, spike: Float): Float {
    val s = sin(angle)
    val p = 2.0.pow(spike.toDouble()).toFloat().coerceIn(0.05f, 32f) // spike [-4..4] -> p ~ [1/16..16]
    val mag = abs(s).toDouble().pow(p.toDouble()).toFloat()
    return sign(s) * mag
}

// Smooth falloff without importing anything heavy.
private fun expFalloff(x: Float): Float {
    // Approx e^(-x) using a cheap rational-ish curve (good enough for visuals).
    return 1f / (1f + x + 0.48f * x * x)
}
