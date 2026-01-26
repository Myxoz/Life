package com.myxoz.life.ui

import android.annotation.SuppressLint
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.myxoz.life.Theme
import com.myxoz.life.screens.feed.fullscreenevent.InputField
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.utils.MaterialShapes
import com.myxoz.life.utils.collectAsMutableState
import com.myxoz.life.utils.combinedRippleClick
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.utils.toShape
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt

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
                    .background(Theme.surfaceContainerHigh, RoundedCornerShape(25))
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
                        listOf(Theme.surfaceContainerHigh, gradientEnd),
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
    val pointerSize = arrowWidth/1.5f
    val strokeWidth = arrowWidth/3f
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
fun Chip(onClick: (()->Unit)?=null, onHold: (()->Unit)?=null, spacing: Dp =0.dp, color: Color=Theme.secondaryContainer, content: @Composable RowScope.()->Unit){
    Row(
        Modifier
            .clip(CircleShape)
            .background(color)
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
@Composable
fun RowChip(onClick: (()->Unit)?=null, onHold: (()->Unit)?=null, spacing: Dp = 0.dp, color: Color=Theme.secondaryContainer, isFirst: Boolean, isLast: Boolean, content: @Composable RowScope.()->Unit){
    Row(
        Modifier
            .clip(rememberAsymmetricalHorrizontalCornerRadius(isFirst, isLast, 50))
            .background(color)
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
@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThreeStateBottomSheet(
    state: ThreeStateBottomSheetState,
    minHeight: Dp,
    color: Color,
    innerPadding: PaddingValues,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val density = LocalDensity.current
        val offset by state.offset.collectAsState()
        val minHeightPx = with(density) { minHeight.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx()  } - minHeightPx
        val heightDp = with(density) {minHeight + offset.toDp()}.coerceIn(minHeight, maxHeight)
        val coroutineScope = rememberCoroutineScope()
        val lastVelocity by state.lastVelocity.collectAsState()
        val snapHeightPx = 0.7f * screenHeightPx
        fun getSnapTarget(): Float{
            if(lastVelocity > 0f  && offset < snapHeightPx) return 0f
            if((lastVelocity > 0f && offset > snapHeightPx && offset <= screenHeightPx) || (lastVelocity < 0f && offset < snapHeightPx)) return snapHeightPx
            return screenHeightPx
        }
        val snapHeightDp = with(density) { snapHeightPx.toDp() }
        state.snapHeight.value = minHeight + snapHeightDp
        state.height.value = heightDp
        state.progress.value = (offset / snapHeightPx).coerceIn(0f, 1f)
        val shape = remember {
            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        }
        PredictiveBackHandler(offset!=0f) {
            val ititialOffset = offset
            it.collect { prog ->
                state.offset.value = ititialOffset * (1-prog.progress.pow(0.5f) * .25f)
            }
            state.expandTo(0, screenHeightPx.toInt(), minHeightPx)
        }
        Box(
            Modifier
                .height(heightDp)
                .background(color, shape)
                .clip(shape)
                .pointerInput(Unit){
                    detectVerticalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                animate(
                                    offset,
                                    getSnapTarget(),
                                    lastVelocity,
                                ){ a, _ ->
                                    state.offset.value = a
                                }
                            }
                        },
                    ){ _, d ->
                        state.lastVelocity.value = d
                        state.offset.value += -d
                    }
                }
                .rippleClick(offset == 0f){
                    coroutineScope.launch {
                        animate(0f, snapHeightPx){ a, _ -> state.offset.value = a }
                    }
                }
                .padding(
                    bottom = (1f-(offset/snapHeightPx).coerceIn(0f, 1f))*innerPadding.calculateBottomPadding(),
                    top = (((offset-snapHeightPx)/(screenHeightPx-snapHeightPx)).coerceIn(0f, 1f))*innerPadding.calculateTopPadding()
                )
        ) {
            MeasuredSheetContent(
                {}
            ){
                content()
            }
        }
    }
}
class ThreeStateBottomSheetState{
    val progress = MutableStateFlow(0f)
    val offset = MutableStateFlow(0f)
    val height = MutableStateFlow(0.dp)
    val snapHeight = MutableStateFlow(0.dp)
    val lastVelocity = MutableStateFlow(0f)
    suspend fun expandTo(stateLevel: Int, screenHeightPx: Int, minHeightPx: Float){
        val snapHeightPx = 0.7f * (screenHeightPx-minHeightPx)
        val snapTarget = when (stateLevel) {
            0 -> 0f
            1 -> snapHeightPx
            else -> screenHeightPx.toFloat()
        }
        animate(
            offset.value,
            snapTarget,
            lastVelocity.value,
        ){ a, v ->
            offset.value = a
            lastVelocity.value = v
            progress.value = (offset.value / snapHeightPx).coerceIn(0f, 1f)
        }
        offset.value = snapTarget
        lastVelocity.value = 0f
        progress.value = (snapTarget / snapHeightPx).coerceIn(0f, 1f)
    }
}

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
fun MeasuredSheetContent(
    onHeightChanged: (Dp) -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    Layout(
        content = {
            Column(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    val heightPx = coordinates.size.height
                    val heightDp = with(density) { heightPx.toDp() }
                    onHeightChanged(heightDp)
                }
            ) {
                content()
            }
        }
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(
            constraints.copy(maxHeight = Constraints.Infinity)
        )

        layout(width = constraints.maxWidth, height = constraints.maxHeight) {
            placeable.placeRelative(0, 0)
        }
    }
}

@Composable
fun rememberAsymmetricalVerticalCornerRadius(isFirst: Boolean, isLast: Boolean, scaling: Int = 24, reverse: Boolean = false) = remember(isFirst, isLast) { RoundedCornerShape(
    if((!reverse && isFirst) || (reverse && isLast)) scaling else scaling/2,
    if((!reverse && isFirst) || (reverse && isLast)) scaling else scaling/2,
    if((reverse && isFirst) || (!reverse && isLast)) scaling else scaling/2,
    if((reverse && isFirst) || (!reverse && isLast)) scaling else scaling/2,
) }

@Composable
fun rememberAsymmetricalHorrizontalCornerRadius(isFirst: Boolean, isLast: Boolean, scaling: Int = 24, reverse: Boolean = false) = remember(isFirst, isLast) { RoundedCornerShape(
    if((!reverse && isFirst) || (reverse && isLast)) scaling else scaling/2,
    if((reverse && isFirst) || (!reverse && isLast)) scaling else scaling/2,
    if((reverse && isFirst) || (!reverse && isLast)) scaling else scaling/2,
    if((!reverse && isFirst) || (reverse && isLast)) scaling else scaling/2,
) }

val BOTTOMSEARCHBARHEIGHT = 56.dp + 5.dp * 2 + 15.dp
@Composable
fun BottomSearchBar(bg: Color, bottomPadding: Dp, onChange: (String)->Unit, icon: Painter? = null, onHold: (() -> Unit)? = null, onClick: (() -> Unit)? = null){
    Column(
        Modifier
            .fillMaxWidth()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(15.dp)
                .background(Brush.verticalGradient(
                    listOf(Color.Transparent, bg.copy(alpha = .5f)),
                ))
        )
        Box(
            Modifier
                .fillMaxWidth()
                .background(bg.copy(alpha = .5f))
                .padding(vertical = 5.dp)
            ,
            Alignment.BottomCenter
        ) {
            Row(
                Modifier
                    .fillMaxWidth(.95f)
                    .padding(bottom = bottomPadding)
                ,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .border(1.dp, Theme.primary, CircleShape)
                        .background(Theme.surfaceContainerHighest, CircleShape)
                ) {
                    InputField(
                        null,
                        "Suchen",
                        background = Color.Transparent,
                        onChange = onChange
                    )
                }
                icon?.let { icon ->
                    val lineHeight = FontSize.LARGE.size.toDp() + 16.dp  * 2 /* TextFieldPadding * 2 */
                    val shape = MaterialShapes.Cookie12Sided.toShape()
                    Box(
                        Modifier
                            .size(lineHeight)
                            .clip(shape)
                            .background(Theme.primary)
                            .combinedRippleClick(
                                onHold,
                                onClick != null && onHold != null
                            ) {
                                onClick?.invoke()
                            }
                    ) {
                        Icon(icon, "New", Modifier.fillMaxSize(), Theme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun ToggleIconButton(active: MutableStateFlow<Boolean>, icon: Painter, onClick: ((Boolean) -> Unit)? = null){
    ToggleButton(active, onClick) {
        Icon(
            icon,
            null,
            Modifier
                .size(30.dp)
            ,
            tint = Theme.onPrimaryContainer
        )
    }
}
@Composable
fun ToggleButton(active: MutableStateFlow<Boolean>, onClick: ((Boolean) -> Unit)? = null, content: @Composable ()->Unit){
    val isActive by active.collectAsMutableState()
    val progress by animateFloatAsState(if(isActive) 1f else 0f) // , tween(easing = LinearEasing)
    val colors by animateColorAsState(if(isActive) Theme.primaryContainer else Theme.surfaceContainerHighest)
    Box(
        Modifier
            .clip(RoundedCornerShape((progress * 20 + 30).roundToInt()))
            .background(colors)
            .rippleClick{
                active.value = !active.value
                onClick?.invoke(active.value)
            }
            .padding(10.dp)
    ) {
        content()
    }
}
val SCREENMAXWIDTH = 700.dp
fun Modifier.setMaxTabletWidth() =
    this
        .widthIn(max = SCREENMAXWIDTH)
        .fillMaxWidth(.95f)
