package com.myxoz.life.utils

import android.content.ClipData
import android.content.SharedPreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.myxoz.life.utils.SharedPrefsUtils.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.reflect.KClass

@Composable
fun Modifier.rippleClick(enabled: Boolean=true, run: ()->Unit): Modifier = this.clickable( remember { MutableInteractionSource() }, ripple(), onClick = run, enabled = enabled)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.combinedRippleClick(onHold: (()->Unit)?=null, enabled: Boolean = true, onClick: ()->Unit): Modifier = this.combinedClickable(
    remember { MutableInteractionSource() }, ripple(), onClick = onClick, onLongClick = onHold, enabled = enabled
)

@Composable
fun TextUnit.toDp(): Dp = with(LocalDensity.current) { this@toDp.toDp() }
fun TextUnit.toDp(density: Density): Dp = with(density) { this@toDp.toDp() }

@Composable
fun Dp.toSp() = with(LocalDensity.current) { this@toSp.toPx() / fontScale / density }.sp

fun Dp.toSp(density: Density) = with(density) { this@toSp.toPx() / fontScale / this.density }.sp

@Composable
fun Dp.toPx() = with(LocalDensity.current) {this@toPx.toPx()}

fun Dp.toPx(density: Density) = with(density) {this@toPx.toPx()}
data class Ref<T>(var value: T)

@Composable
fun <T> MutableStateFlow<T>.collectAsMutableState(): MutableState<T> {
    val state = this.collectAsState()
    return remember(this) {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(value) {
                    this@collectAsMutableState.value = value
                }

            override fun component1(): T = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}
@Composable
fun <T> MutableStateFlow<T?>.collectAsMutableNonNullState(defValue: T): MutableState<T> {
    val state = this.collectAsState()

    return remember(this) {
        object : MutableState<T> {
            override var value: T
                get() = state.value ?: defValue
                set(value) {
                    this@collectAsMutableNonNullState.value = value
                }

            override fun component1(): T = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

@OptIn(FlowPreview::class)
fun <T: Any> Flow<T>.syncToPrefs(
    scope: CoroutineScope,
    prefs: SharedPreferences,
    key: String,
    type: KClass<T>,
    debounceMs: Long = 200
) {
    scope.launch(Dispatchers.IO) {
        this@syncToPrefs
            .debounce(debounceMs)
            .collect { value ->
                prefs.edit{
                    put(key, value, type)
                }
            }
    }
}

@OptIn(FlowPreview::class)
fun <T: Any> Flow<T?>.syncNullableToPrefs(
    scope: CoroutineScope,
    prefs: SharedPreferences,
    key: String,
    type: KClass<T>,
    debounceMs: Long = 200
) {
    scope.launch(Dispatchers.IO) {
        this@syncNullableToPrefs
            .debounce(debounceMs)
            .collect { value ->
                value ?: return@collect
                prefs.edit{
                    put(key, value, type)
                }
            }
    }
}


val windowPadding: PaddingValues
    @Composable get() = WindowInsets.systemBars.asPaddingValues(LocalDensity.current)

suspend fun Clipboard.copy(text: String) {
    setClipEntry(ClipEntry(ClipData.newPlainText(text, text)))
}


fun Modifier.boxShadow(
    color: Color = Color.Black,
    alpha: Float = 1f,
    blur: Dp = 0.dp,
    spread: Dp = 0.dp,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    shape: Shape
): Modifier = this
    .graphicsLayer { clip = false } // essential for shadow blur
    .drawBehind {
        val blurPx = blur.toPx()
        val spreadPx = spread.toPx()
        val offsetXPx = offsetX.toPx()
        val offsetYPx = offsetY.toPx()

        // Create outline from shape with spread applied (scale the outline)
        val spreadSize = Size(
            size.width + spreadPx * 2,
            size.height + spreadPx * 2
        )
        val spreadOutline = if (spreadPx != 0f) {
            // Scale the original shape to the spread size
            shape.createOutline(spreadSize, layoutDirection, this)
        } else {
            shape.createOutline(size, layoutDirection, this)
        }

        val paint = Paint().apply {
            asFrameworkPaint().apply {
                // Transparent fill – only the shadow will be visible
                this.color = Color.Transparent.toArgb()
                setShadowLayer(blurPx, offsetXPx, offsetYPx, color.copy(alpha = alpha).toArgb())
            }
        }

        drawIntoCanvas { canvas ->
            canvas.save()
            if (spreadPx != 0f) {
                // Translate to keep the spread shape centered
                canvas.translate(-spreadPx, -spreadPx)
            }
            when (spreadOutline) {
                is Outline.Rectangle -> canvas.drawRect(spreadOutline.rect, paint)
                is Outline.Rounded ->  {
                    val r = spreadOutline.roundRect
                    canvas.drawRoundRect(
                        left = r.left,
                        top = r.top,
                        right = r.right,
                        bottom = r.bottom,
                        radiusX = r.topLeftCornerRadius.x,
                        radiusY = r.topLeftCornerRadius.y,
                        paint = paint
                    )
                }
                is Outline.Generic -> canvas.drawPath(spreadOutline.path, paint)
            }
            canvas.restore()
        }
    }
// Fuck AI we use StackOverflow here:
// https://stackoverflow.com/questions/68218714/angled-gradient-background-in-jetpack-compose#68219962
fun Modifier.angledGradientBackground(colors: List<Color>, degrees: Float) = this.drawBehind {
        val (x, y) = size
        val gamma = atan2(y, x)
        if (gamma == 0f || gamma == (PI / 2).toFloat()) {
            return@drawBehind
        }
        val degreesNormalised = (degrees % 360).let { if (it < 0) it + 360 else it }
        val alpha = (degreesNormalised * PI / 180).toFloat()
        val gradientLength = when (alpha) {
            in 0f..gamma, in (2*PI - gamma)..2*PI -> { x / cos(alpha) }
            in gamma..(PI - gamma).toFloat() -> { y / sin(alpha) }
            in (PI - gamma)..(PI + gamma) -> { x / -cos(alpha) }
            in (PI + gamma)..(2*PI - gamma) -> { y / -sin(alpha) }
            else -> hypot(x, y)
        }

        val centerOffsetX = cos(alpha) * gradientLength / 2
        val centerOffsetY = sin(alpha) * gradientLength / 2

        drawRect(
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset(center.x - centerOffsetX,center.y - centerOffsetY),
                end = Offset(center.x + centerOffsetX, center.y + centerOffsetY)
            ),
            size = size
        )
    }


// Overwrite default height to 0.dp so while messuring this appears as 0.dp and gets streched later. Remember this trick.
fun Modifier.matchInstrinsicHeight() = this.fillMaxHeight().height(0.dp)
fun Modifier.matchInstrinsicHeight(fraction: Float) = this.fillMaxHeight(fraction).height(0.dp)

// Fully Vibe-Coded... Famous last words. Man, can ChatGPT not just do its job?
@Composable
fun LayeredCircularProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color,
    trackColor: Color,
    strokeWidth: Dp,
    trackStrokeWidth: Dp,
) {
    val coercedProgress = progress.coerceIn(0f, 1f)

    val density = LocalDensity.current
    val stroke = with(density) {
        Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
    }
    val trackStroke = with(density) {
        Stroke(width = trackStrokeWidth.toPx(), cap = StrokeCap.Round)
    }

    Canvas(modifier) {
        val startAngle = 270f
        val sweep = coercedProgress * 360f
        val maxRadius = max(stroke.width, trackStroke.width)

        drawCircularArc(
            maxRadius - trackStroke.width,
            0f,
            360f,
            trackColor,
            trackStroke
        )

        drawCircularArc(
            maxRadius - stroke.width,
            startAngle,
            sweep,
            color,
            stroke
        )
    }
}

private fun DrawScope.drawCircularArc(
    paddingToBounds: Float,
    startAngle: Float,
    sweep: Float,
    color: Color,
    stroke: Stroke,
) {
    val diameterOffset = stroke.width / 2
    val arcSize = size.minDimension - 2 * diameterOffset

    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(diameterOffset + paddingToBounds / 2, diameterOffset + paddingToBounds / 2),
        size = Size(arcSize - paddingToBounds, arcSize - paddingToBounds),
        style = stroke
    )
}