package com.myxoz.life.ui

import android.annotation.SuppressLint
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.times
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.ui.feed.fullscreenevent.InputField
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.utils.MaterialShapes
import com.myxoz.life.utils.collectAsMutableState
import com.myxoz.life.utils.combinedRippleClick
import com.myxoz.life.utils.copy
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.utils.toShape
import com.myxoz.life.utils.transformed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
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

class ThreeStateBottomSheetState(
    val initialState: SheetValue,
) {
    var state by mutableStateOf(initialState)
        private set
    var height by mutableFloatStateOf(0f)
        private set
    var snapHeightPx by mutableFloatStateOf(0f)
        private set
    var partialToFullProgress by mutableFloatStateOf(if(initialState == SheetValue.Expanded) 1f else 0f)
        private set
    var partialToCollapsed by mutableFloatStateOf(if(initialState == SheetValue.Collapsed) 1f else 0f)
        private set
    private var lastVelocity by mutableFloatStateOf(0f)
    private var screenHeightPx by mutableFloatStateOf(0f)
    private var collapsedSheetHeight by mutableFloatStateOf(0f)
    suspend fun expandTo(stateLevel: SheetValue){
        animateTo(stateLevel)
    }
    private var isInitial = true
    private fun updateLayout(screenHeightPx: Float, collapsedSheetHeight: Float) {
        val screenHeightHasUpdated = screenHeightPx != this.screenHeightPx
        this.screenHeightPx = screenHeightPx
        this.snapHeightPx = screenHeightPx * .7f
        this.collapsedSheetHeight = collapsedSheetHeight
        if(isInitial) {
            applyNewHeight(initialState.heightPx())
            isInitial = false
        } else if(screenHeightHasUpdated) {
            applyNewHeight(state.heightPx())
        }
    }
    private fun getSnapTarget(): SheetValue {
        if(lastVelocity > 0f  && height < snapHeightPx) return SheetValue.Collapsed
        if((lastVelocity > 0f && height > snapHeightPx && height <= screenHeightPx) || (lastVelocity < 0f && height < snapHeightPx)) return SheetValue.Partial
        return SheetValue.Expanded
    }
    private suspend fun animateTo(target: SheetValue) {
        animate(
            height,
            target.heightPx(),
            lastVelocity,
        ){ animationValue, v ->
            applyNewHeight(animationValue)
            lastVelocity = v
        }
    }
    private fun applyNewHeight(newHeight: Float) {
        val partial = SheetValue.Partial.heightPx()
        val expanded = SheetValue.Expanded.heightPx()
        val collapsed = SheetValue.Collapsed.heightPx()
        height = newHeight
        state = when {
            newHeight <= collapsed -> SheetValue.Collapsed
            newHeight >= expanded -> SheetValue.Expanded
            else -> SheetValue.Partial
        }
        partialToFullProgress = ((height - partial) / (expanded - partial)).coerceIn(0f, 1f)
        partialToCollapsed = ((height - collapsed) / (partial - collapsed)).coerceIn(0f, 1f)
    }
    private fun SheetValue.heightPx(): Float{
        return when(this){
            SheetValue.Collapsed -> collapsedSheetHeight
            SheetValue.Partial -> snapHeightPx
            SheetValue.Expanded -> screenHeightPx
        }
    }
    private suspend fun handleDrop() {
        animateTo(getSnapTarget())
    }
    private suspend fun previewPredictiveBack(progress: Flow<BackEventCompat>) {
        val ititialOffset = height
        try {
            progress.collect { backEvent ->
                height = ititialOffset * (1-backEvent.progress.pow(0.5f) * .25f)
            }
            expandTo(SheetValue.Collapsed)
        } catch (_: CancellationException) {
            // This block is executed if the gesture is cancelled.
            height = ititialOffset
        }
    }
    enum class SheetValue {
        Collapsed,
        Partial,
        Expanded
    }
    companion object {
        @SuppressLint("StateFlowValueCalledInComposition")
        @OptIn(ExperimentalFoundationApi::class)
        @Composable
        fun ThreeStateBottomSheet(
            state: ThreeStateBottomSheetState,
            collapsedSheetHeight: Dp,
            color: Color,
            innerPadding: PaddingValues,
            content: @Composable () -> Unit,
        ) {
            BoxWithConstraints(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                val density = LocalDensity.current
                val screenHeightPx = with(density) { maxHeight.toPx()  }
                SideEffect {
                    state.updateLayout(screenHeightPx, with(density) { collapsedSheetHeight.toPx() })
                }

                val shape = remember {
                    RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                }
                PredictiveBackHandler(state.state != SheetValue.Collapsed, state::previewPredictiveBack)
                val heightDp by remember {
                    derivedStateOf {
                        with(density) { state.height.toDp() }
                    }
                }
                val coroutineScope = rememberCoroutineScope()
                Box(
                    Modifier
                        .height(heightDp)
                        .background(color, shape)
                        .clip(shape)
                        .pointerInput(Unit){
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    coroutineScope.launch {
                                        state.handleDrop()
                                    }
                                },
                            ){ _, d ->
                                state.lastVelocity = d
                                state.applyNewHeight(state.height - d)
                            }
                        }
                        .rippleClick(state.state == SheetValue.Collapsed) {
                            coroutineScope.launch {
                                state.expandTo(SheetValue.Partial)
                            }
                        }
                        .padding(
                            top = state.partialToFullProgress * innerPadding.calculateTopPadding()
                        )
                ) {
                    MeasuredSheetContent(
                        {}
                    ) {
                        content()
                    }
                }
            }
        }

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
fun BottomSearchBar(bg: Color, bottomPadding: Dp, onChange: (String)->Unit, icon: Painter? = null, onHold: (() -> Unit)? = null, defaltValue: String? = null, onClick: (() -> Unit)? = null) {
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
                        defaltValue,
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
@Composable
fun getMaxTabletScreenWidth(): Dp = min(LocalWindowInfo.current.containerDpSize.width * .95f, SCREENMAXWIDTH)
fun Modifier.setMaxTabletWidth() =
    this
        .widthIn(max = SCREENMAXWIDTH)
        .fillMaxWidth(.95f)

@Composable
fun LifeProgressIndicator(modifier: Modifier, color: Color){
    Box(
        modifier
            .aspectRatio(1f)
    ) {
        val infiniteAnimation = rememberInfiniteTransition()
        val rotation by infiniteAnimation.animateFloat(
            0f,
            360f,
            infiniteRepeatable(
                tween(2000, easing = LinearEasing))
            )
        Icon(
            painterResource(R.drawable.life_icon_outer_trimmed),
            "Loading",
            Modifier
                .fillMaxSize()
                .rotate(rotation)
            ,
            color
        )
        Icon(
            painterResource(R.drawable.life_icon_center_trimmed),
            null,
            Modifier
                .fillMaxSize()
            ,
            color
        )
    }
}

@Composable
fun Modifier.holdToCopy(text: String?, coroutineScope: CoroutineScope? = null, click: ()->Unit = {}): Modifier {
    val clipboard = LocalClipboard.current
    val coroutineScope = coroutineScope ?: rememberCoroutineScope()
    return this.combinedClickable(null, null, onLongClick = {
        if(text == null) return@combinedClickable
        coroutineScope.launch {
            clipboard.copy(text)
        }
    }, onClick = click)
}

@Composable
fun EditToTickAndDiscard(
    isEditing: Boolean,
    iconSize: Dp,
    editIcon: ()->Int = {R.drawable.edit},
    padding: Dp = 10.dp,
    save: () -> Unit,
    discard: () -> Unit,
    edit: () -> Unit
){
    val editingAnimationProgress by animateFloatAsState(if(isEditing) 1f else 0f)
    Box(
        Modifier.width(iconSize+padding + editingAnimationProgress*(iconSize+padding)),
        contentAlignment = Alignment.CenterEnd
    ) {
        val discardShape = remember {
            MaterialShapes.Ghostish.transformed(Matrix().apply { rotateZ(-90f) })
        }.toShape()
        val morph = remember {
            Morph(
                MaterialShapes.Cookie12Sided,
                MaterialShapes.Ghostish.transformed(Matrix().apply { rotateZ(90f) })
            )
        }
        val path = remember(editingAnimationProgress) {
            morph.toPath(editingAnimationProgress).asComposePath()
        }
        val shape = path.toShape()
        Box(
            Modifier
                .offset(x = -editingAnimationProgress*(iconSize+padding))
                .alpha(editingAnimationProgress)
                .size(iconSize+padding)
                .background(Theme.secondaryContainer, discardShape)
                .clip(discardShape)
                .rippleClick{
                    discard()
                }
                .padding(padding)
        ) {
            Icon(painterResource(R.drawable.close), "Discard", Modifier.fillMaxSize(), Theme.onSecondaryContainer)
        }
        Box(
            Modifier
                .size(iconSize+padding)
                .background(Theme.primaryContainer, shape)
                .clip(shape)
                .rippleClick{
                    if(!isEditing) edit() else save()
                }
                .padding(padding)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                !isEditing,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                Icon(painterResource(editIcon()), "Edit", Modifier.fillMaxSize(), Theme.onSecondaryContainer)
            }
            androidx.compose.animation.AnimatedVisibility(
                isEditing,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                Icon(painterResource(R.drawable.tick), "Save", Modifier.fillMaxSize(), Theme.onSecondaryContainer)
            }
        }
    }
    BackHandler(isEditing){
        discard()
    }
}