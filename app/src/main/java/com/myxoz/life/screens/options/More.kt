package com.myxoz.life.screens.options

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.screens.NavPath
import com.myxoz.life.screens.options.SubOption.Companion.RenderSubOptions

@Composable
fun MoreComposable(){
    Box(
        Modifier
            .background(Theme.background)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
        ,
        Alignment.BottomCenter
    ) {
        listOf(
            SubOption(R.drawable.settings, "Einstellungen", NavPath.Menu.More.SETTINGS),
            SubOption(R.drawable.debug, "Debug", NavPath.Menu.More.DEBUG),
            SubOption(R.drawable.info, "Informationen", NavPath.Menu.More.INFORMATION),
            SubOption(R.drawable.contacts, "Ich", NavPath.Menu.Contacts.DISPLAY_PERSON.with(ME_ID)),
            SubOption(R.drawable.sparkles, "KI", NavPath.Menu.More.AI),
        ).RenderSubOptions()
    }
}
