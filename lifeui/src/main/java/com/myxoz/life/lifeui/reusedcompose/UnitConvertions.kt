package com.myxoz.life.lifeui.reusedcompose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun TextUnit.toDp(): Dp = with(LocalDensity.current) { this@toDp.toDp() }
fun TextUnit.toDp(density: Density): Dp = with(density) { this@toDp.toDp() }

@Composable
fun Dp.toSp() = with(LocalDensity.current) { this@toSp.toPx() / fontScale / density }.sp

fun Dp.toSp(density: Density) = with(density) { this@toSp.toPx() / fontScale / this.density }.sp

@Composable
fun Dp.toPx() = with(LocalDensity.current) {this@toPx.toPx()}

fun Dp.toPx(density: Density) = with(density) {this@toPx.toPx()}
