package com.myxoz.life

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import com.myxoz.life.screens.LocalScreensProvider
import com.myxoz.life.viewmodels.Settings

val LocalNavController = staticCompositionLocalOf<NavController> {
    error("No NavController provided")
}
val LocalSettings = staticCompositionLocalOf<Settings> {
    error("No Settings provided")
}
val LocalScreens = staticCompositionLocalOf<LocalScreensProvider> {
    error("No ScreenProvider provided")
}
val LocalColors = staticCompositionLocalOf<ColorScheme> {
    error("No color scheme provided")
}
val Theme: ColorScheme
    @Composable
    inline get() = LocalColors.current
