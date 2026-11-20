package com.myxoz.life.lifeui.reusedcompose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min

val SCREENMAXWIDTH = 700.dp
@Composable
fun getMaxTabletScreenWidth(): Dp = min(LocalWindowInfo.current.dpWidth * .95f, SCREENMAXWIDTH)
fun Modifier.setMaxTabletWidth() =
    this
        .widthIn(max = SCREENMAXWIDTH)
        .fillMaxWidth(.95f)
