package com.myxoz.life

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import com.myxoz.life.api.API
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.viewmodels.Settings

val LocalAPI = staticCompositionLocalOf<API> {
    error("No API provided. Make sure to wrap your app in a provider.")
}
val LocalNavController = staticCompositionLocalOf<NavController> {
    error("No NavController provided")
}
val LocalStorage = staticCompositionLocalOf<StorageManager> {
    error("No AppDatabase provided")
}

val LocalColors = staticCompositionLocalOf<Colors> {
    error("No Colors provided")
}
val LocalSettings = staticCompositionLocalOf<Settings> {
    error("No Settings provided")
}
