package com.myxoz.life.screens.feed.instantevents

import android.icu.util.Calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.myxoz.life.LocalScreens
import com.myxoz.life.Theme
import com.myxoz.life.screens.options.SubOption
import com.myxoz.life.screens.options.SubOption.Companion.RenderSubOptions
import com.myxoz.life.utils.formatDayTime
import com.myxoz.life.viewmodels.InstantEventsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InstantEventsScreen(instantEventsViewModel: InstantEventsViewModel){
    Box(
        Modifier
            .background(Theme.background)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
        ,
        Alignment.BottomCenter
    ) {
        val calendar = Calendar.getInstance()
        val screens = LocalScreens.current
        val instantEvents by instantEventsViewModel.lookedAtInstantEvents.collectAsState()
        instantEvents.map { SubOption(it.icon, it.subText, null, it.timestamp.formatDayTime(calendar), {it.openDetails(screens)}) }.RenderSubOptions()
    }
}