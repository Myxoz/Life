package com.myxoz.life.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.utils.combinedRippleClick
import com.myxoz.life.utils.rippleClick

@Composable
fun ActionBar(smallActionClick: (()->Unit)?, smallContent: (@Composable ()->Unit)?, color: Color, onLargeClick: ()->Unit, largeContent: @Composable ()->Unit){
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val gradientEnd by animateColorAsState(color, tween(2000, easing = LinearEasing))
        if(smallContent!=null){
            Box(
                Modifier
                    .size(75.dp)
                    .background(Colors.SECONDARY, RoundedCornerShape(25))
                    .clip(RoundedCornerShape(25))
                    .rippleClick{
                        smallActionClick?.invoke()
                    }
                    .padding(22.5.dp)
                ,
                contentAlignment = Alignment.Center
            ) {
                smallContent()
            }
        }
        Box(
            Modifier
                .padding(bottom = 10.dp)
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Colors.SECONDARY, gradientEnd),
                        startX = 250f
                    ),
                    RoundedCornerShape(25)
                )
                .clip(RoundedCornerShape(25))
                .rippleClick{
                    onLargeClick()
                }
                .height(75.dp)
            ,
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ){
                largeContent()
            }
        }
    }
}
enum class ArrowDirection {
    Up, Right, Down, Left
}
fun Modifier.drawArrowBehind(direction: ArrowDirection, arrowWidth: Float, color: Color) = this.drawWithCache{
    val width = this.size.width
    val height = this.size.height
    val pointerSize = arrowWidth/1.5f // TODO Tweak later
    val strokeWidth = arrowWidth/3f // TODO Tweak later
    val components = when(direction){
        ArrowDirection.Up -> arrayOf(width/2 to height-strokeWidth, width/2 to strokeWidth, -pointerSize to pointerSize)
        ArrowDirection.Right -> arrayOf(strokeWidth to height/2, width-strokeWidth to height/2, -pointerSize to -pointerSize)
        ArrowDirection.Down -> arrayOf(width/2 to strokeWidth, width/2 to height-strokeWidth, pointerSize to -pointerSize)
        ArrowDirection.Left -> arrayOf(strokeWidth-width to height/2, strokeWidth to height/2, pointerSize to pointerSize)
    }
    onDrawBehind {
        drawLine(
            color,
            Offset(components[0].first, components[0].second),
            Offset(components[1].first, components[1].second),
            strokeWidth,
            StrokeCap.Round
        )
        drawLine(
            color,
            Offset(components[1].first+components[2].first, components[1].second+components[2].second),
            Offset(components[1].first, components[1].second),
            strokeWidth,
            StrokeCap.Round
        )
        drawLine(
            color,
            Offset(
                components[1].first+(if(components[2].first == components[2].second) components[2].first else -components[2].first),
                components[1].second+(if(components[2].first == components[2].second) -components[2].second else components[2].second)
            ),
            Offset(components[1].first, components[1].second),
            strokeWidth,
            StrokeCap.Round
        )
    }
}
@Composable
fun Chip(onClick: (()->Unit)?=null, onHold: (()->Unit)?=null, spacing: Dp =0.dp, color: Color=Colors.SECONDARY, content: @Composable RowScope.()->Unit){
    Row(
        Modifier
            .background(color, CircleShape)
            .clip(CircleShape)
            .combinedRippleClick({
                onHold?.invoke()
            }, onClick!=null || onHold!=null){
                onClick?.invoke()
            }
            .padding(horizontal = 10.dp, vertical = 5.dp)
        ,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}
