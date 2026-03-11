package com.myxoz.life.screens.feed.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.myxoz.life.LocalSettings
import com.myxoz.life.Theme
import com.myxoz.life.screens.feed.search.LifeBottomBar
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.CalendarViewModel
import com.myxoz.life.viewmodels.InspectedEventViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeComposable(calendarViewModel: CalendarViewModel, inspectedEventViewModel: InspectedEventViewModel){
    val settings = LocalSettings.current
    LaunchedEffect(Unit) {
        calendarViewModel.resync()
        calendarViewModel.refetchAlarmClockTs()
        withContext(Dispatchers.IO) {
            calendarViewModel.requestAutoDetectedEventStart(settings)
        }

    }
    Box(
        Modifier
            .background(Theme.surfaceContainer)
            .padding(windowPadding)
    ) {
        Column(
            Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.weight(1f)) {
                CalendarComposable(calendarViewModel, inspectedEventViewModel)
            }
            LifeBottomBar(calendarViewModel)
        }
        DaySummaryPopUp(calendarViewModel, inspectedEventViewModel)
        MainFeedDatePicker(calendarViewModel)
    }
}