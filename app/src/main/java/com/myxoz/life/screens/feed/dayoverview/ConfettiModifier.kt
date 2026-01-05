package com.myxoz.life.screens.feed.dayoverview

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

// Fully Vibe-Coded refer to https://chatgpt.com/c/695adcd8-53c4-8332-82fc-75dae846bbcd
// Nvm, ChatGPTs-Version sucked so I rewrote most of it

private data class ConfettiParticle(
    val startX: Float,          // 0..1 (relative, scaled each frame)
    val startY: Float,          // negative, above screen
    val size: Float,
    val fallSpeed: Float,       // px per second multiplier
    val swayAmplitude: Float,   // px
    val swayFrequency: Float,   // radians
    val rotationSpeed: Float,
    val color: Color,
    val timeOffset: Float       // seconds
)

fun Modifier.confetti(
    enabled: Boolean = true,
    particleCount: Int = 40
): Modifier = if (!enabled) this else composed {

    val colors = listOf(
        Color(0xFFE57373),
        Color(0xFFBA68C8),
        Color(0xFF64B5F6),
        Color(0xFF4DB6AC),
        Color(0xFFFFD54F)
    )

    val density = LocalDensity.current

    val particles = remember {
        List(particleCount) {
            ConfettiParticle(
                startX = Random.nextFloat(),
                startY = 0f,
                size = Random.nextFloat() * 6f + 6f,
                fallSpeed = Random.nextFloat() * 0.2f + 0.2f,
                swayAmplitude = Random.nextFloat() * 25f + 10f,
                swayFrequency = Random.nextFloat() * 2f + 1f,
                rotationSpeed = Random.nextFloat() * 120f - 60f,
                color = colors.random(),
                timeOffset = Random.nextFloat() * 3f - 3f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "confetti")
    val timeSeconds by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000 * 1000, easing = LinearEasing)
        ),
        label = "confettiTime"
    )

    drawWithContent {
        drawContent()

        val width = size.width
        val height = size.height

        particles.forEach { p ->
            val resetMargin = p.size
            val t = timeSeconds + p.timeOffset

            val baseX = p.startX * width
            val y = p.startY + t * p.fallSpeed
            val wrappedY = ((((if(y > 0) y else -0.1f) % 1f) * height) * (height + resetMargin * 4) / height) - resetMargin * 2

            val sway = sin(t * p.swayFrequency) * p.swayAmplitude
            val x = baseX + sway

            rotate(
                degrees = t * p.rotationSpeed,
                pivot = Offset(x, wrappedY)
            ) {
                drawRect(
                    color = p.color,
                    topLeft = Offset(x, wrappedY),
                    size = Size(
                        with(density) { p.size.dp.toPx() },
                        with(density) { (p.size * 0.6f).dp.toPx() }
                    )
                )
            }
        }
    }
}
