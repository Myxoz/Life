package com.myxoz.life

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import com.myxoz.life.ui.LocalScreensProvider

val LocalScreens = staticCompositionLocalOf<LocalScreensProvider> {
    error("No ScreenProvider provided")
}
