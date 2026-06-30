package com.myxoz.life.ui.options.settings

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
import com.myxoz.life.ui.NavPath
import com.myxoz.life.ui.options.SubOption.Companion.RenderSubOptions

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
            _root_ide_package_.com.myxoz.life.ui.options.SubOption(
                R.drawable.settings,
                "Berechtigungen",
                NavPath.Menu.More.Settings.PERMISSIONS
            ),
            _root_ide_package_.com.myxoz.life.ui.options.SubOption(
                R.drawable.settings,
                "Preferences",
                NavPath.Menu.More.Settings.PREFERENCES
            ),
        ).RenderSubOptions()
    }
}