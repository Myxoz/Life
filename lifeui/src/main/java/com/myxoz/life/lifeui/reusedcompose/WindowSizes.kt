package com.myxoz.life.lifeui.reusedcompose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize

val WindowInfo.containerDpSize: DpSize
    @Composable
    get() {
        val density = LocalDensity.current
        val conf = this.containerSize
        val (width, height) = with(density) { conf.width.toDp() to conf.height.toDp() }
        return DpSize(width, height)
    }
val WindowInfo.dpWidth: Dp
    @Composable
    get() {
        val density = LocalDensity.current
        val conf = this.containerSize
        return with(density) { conf.width.toDp() }
    }
val WindowInfo.dpHeight: Dp
    @Composable
    get() {
        val density = LocalDensity.current
        val conf = this.containerSize
        return with(density) { conf.height.toDp() }
    }

val windowPadding: PaddingValues
    @Composable get() = WindowInsets.systemBars.asPaddingValues(LocalDensity.current)
