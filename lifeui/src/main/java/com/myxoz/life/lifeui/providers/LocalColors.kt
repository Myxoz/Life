package com.myxoz.life.lifeui.providers

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalColors = staticCompositionLocalOf<ColorScheme> {
    error("No color scheme provided")
}
inline val Theme: ColorScheme
    @Composable
    inline get() = LocalColors.current
