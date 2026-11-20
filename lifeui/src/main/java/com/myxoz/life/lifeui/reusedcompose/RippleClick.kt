package com.myxoz.life.lifeui.reusedcompose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun Modifier.rippleClick(enabled: Boolean=true, run: ()->Unit): Modifier =
    this.clickable(
        remember { MutableInteractionSource() },
        ripple(),
        onClick = run,
        enabled = enabled
    )
