package com.myxoz.life.ui.feed.instantevents

import android.icu.util.Calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.myxoz.life.LocalScreens
import com.myxoz.life.Theme
import com.myxoz.life.ui.options.SubOption.Companion.RenderSubOptions
import com.myxoz.life.utils.formatDayTime

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
        val instantEvents = instantEventsViewModel.instantEvents
        instantEvents.map {
            _root_ide_package_.com.myxoz.life.ui.options.SubOption(
                it.icon,
                it.subText,
                null,
                it.timestamp.formatDayTime(calendar),
                { it.openDetails(screens) })
        }.RenderSubOptions()
    }
}