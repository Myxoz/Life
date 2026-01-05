package com.myxoz.life.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

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