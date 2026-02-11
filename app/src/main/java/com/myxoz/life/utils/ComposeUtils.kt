package com.myxoz.life.utils

import android.content.ClipData
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow

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


val windowPadding: PaddingValues
    @Composable get() = WindowInsets.systemBars.asPaddingValues(LocalDensity.current)

suspend fun Clipboard.copy(text: String) {
    setClipEntry(ClipEntry(ClipData.newPlainText(text, text)))
}