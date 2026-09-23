package com.myxoz.life.ui.socialgraph

import androidx.compose.animation.core.Animatable
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.util.lerp

class SocialGraphVisual {
    class NodeState {
        var progress = Animatable(0.5f)
        fun nodeColor(theme: ColorScheme) = when(val progress = progress.value){
            in 0.5f..1.0f -> lerp(theme.secondary, theme.primary, progress * 2 - 1)
            else -> lerp(theme.secondaryContainer, theme.secondary, progress * 2)
        }
        fun fontColor(theme: ColorScheme) =  when(val progress = progress.value){
            in 0.5f..1.0f -> lerp(theme.secondary, theme.primary, progress * 2 - 1)
            else -> lerp(theme.secondary.copy(0.0f), theme.secondary, progress * 2)
        }
        fun fontSize() = lerp(30f, 50f, progress.value)
        fun radius() = lerp(30f, 50f, progress.value)
        fun shouldDrawNodeName() = progress.value > 0f
        companion object {
            const val IRRELEVANT = 0.0f
            const val DEFAULT = 0.5f
            const val CONNECTEDTOSELECTED = 0.5f
            const val SELECTED = 1.0f
            const val INITIALFONTSIZE = 40f
        }
    }
    class EdgeState{
        var progress = Animatable(0.5f)
        fun shouldDrawTextEdge() = progress.value > 0.5f
        fun fontColor(theme: ColorScheme): Color = when(val progress = progress.value){
            in 0.5f..1f -> lerp(theme.secondary.copy(0.0f), theme.secondary, progress * 2 - 1f)
            else -> Color.Transparent
        }
        fun edgeColor(theme: ColorScheme): Color = when(val progress = progress.value){
            in 0.5f..1f -> theme.outline
            else -> lerp(theme.outlineVariant, theme.outline, progress * 2)
        }

        val radius: Float
            get() = lerp(40f, 50f, progress.value)

        companion object {
            const val IRRELEVANT = 0.0f
            const val DEFAULT = 0.5f
            const val CONNECTEDTOSELECTED = 1.0f
        }
    }
}