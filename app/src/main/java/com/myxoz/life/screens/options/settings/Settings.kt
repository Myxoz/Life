package com.myxoz.life.screens.options.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.screens.NavPath
import com.myxoz.life.screens.options.SubOption
import com.myxoz.life.screens.options.SubOption.Companion.RenderSubOptions

const val ME_ID = 1L
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsComposable(){
    Box(
        Modifier
            .background(Theme.background)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
        ,
        Alignment.BottomCenter
    ) {
        listOf(
            SubOption(
                R.drawable.settings,
                "Berechtigungen",
                NavPath.Menu.More.Settings.PERMISSIONS
            ),
            SubOption(R.drawable.settings, "Preferences", NavPath.Menu.More.Settings.PREFERENCES),
        ).RenderSubOptions()
    }
}