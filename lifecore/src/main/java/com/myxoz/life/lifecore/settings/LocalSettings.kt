package com.myxoz.life.lifecore.settings

import androidx.compose.runtime.staticCompositionLocalOf

val LocalSettings = staticCompositionLocalOf<CompositionSettings> {
    error("No Settings provided")
}