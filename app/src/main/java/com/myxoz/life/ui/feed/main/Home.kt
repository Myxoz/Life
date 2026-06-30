package com.myxoz.life.ui.feed.main

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
import com.myxoz.life.ui.feed.CalendarRepo
import com.myxoz.life.ui.feed.search.LifeBottomBar
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeComposable(calendarRepo: CalendarRepo, calendarViewModel: CalendarViewModel) {
    val settings = LocalSettings.current
    LaunchedEffect(Unit) {
        if(settings.hasAssured(Settings.Feature.SyncWithServer)) calendarRepo.resync()
        calendarRepo.refetchAlarmClockTs()
        withContext(Dispatchers.IO) {
            calendarRepo.requestAutoDetectedEventStart(settings)
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
                CalendarComposable(calendarRepo, calendarViewModel)
            }
            LifeBottomBar(calendarRepo, calendarViewModel)
        }
        DaySummaryPopUp(calendarRepo)
        BackupIssuesPopup(calendarRepo)
        MainFeedDatePicker(calendarViewModel)
    }
}