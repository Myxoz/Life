package com.myxoz.life.utils.diagrams

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class PieChart(){
    val components = MutableStateFlow<Map<String, PieChartPart>>(mapOf())
    fun update(newComponents: Map<String, PieChartPart>) {
        val mutComponents = components.value.mapValues { PieChartPart(it.value.color, 0.0) }.toMutableMap()
        newComponents.forEach { mutComponents[it.key] = it.value }
        components.value = mutComponents
    }
    @Composable
    fun Render(){
        val currentComponents by components.collectAsState()
        val animatedAngles = remember { mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>() }
        val total = currentComponents.values.sumOf { it.value }
        LaunchedEffect(currentComponents) {
            currentComponents.keys.forEach { id ->
                if (!animatedAngles.containsKey(id)) {
                    animatedAngles[id] = Animatable(0f)
                }
            }

            // Animate all parts to their target values
            currentComponents.forEach { (id, part) ->
                val targetAngle = if(total > 0.0) ((part.value / total) * 360f).toFloat() else 0f
                launch {
                    animatedAngles[id]?.animateTo(
                        targetValue = targetAngle,
                        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
                    )
                }
            }
        }

        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val canvasSize = size.minDimension
                val radius = canvasSize / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                var startAngle = -90f // Start from top

                currentComponents.forEach { part ->
                    val sweepAngle = animatedAngles[part.key]?.value ?: 0f

                    if (sweepAngle > 0f) {
                        drawArc(
                            color = part.value.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            topLeft = Offset(
                                center.x - radius,
                                center.y - radius
                            ),
                            size = Size(radius * 2, radius * 2)
                        )
                        startAngle += sweepAngle
                    }
                }
            }
        }
    }

    companion object {
        data class PieChartPart(val color: Color, val value: Double)
        fun build(builder: PieChartBuilder.()->Unit): Map<String, PieChartPart>{
            val pieChartBuilder = PieChartBuilder()
            pieChartBuilder.builder()
            return pieChartBuilder.parts
        }
        class PieChartBuilder {
            internal val parts = mutableMapOf<String, PieChartPart>()
            fun add(color: Color, value: Double, id: String){
                parts[id] = PieChartPart(color, value)
            }
        }
    }
}