package com.myxoz.life.calendar.feed

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
import com.myxoz.life.calendar.CalendarSuper
import com.myxoz.life.calendar.feed.events.CalendarComposable
import com.myxoz.life.calendar.search.LifeBottomBar
import com.myxoz.life.lifecore.settings.LocalSettings
import com.myxoz.life.lifeui.providers.Theme
import com.myxoz.life.lifeui.reusedcompose.windowPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeComposable(
    calendarFeedApplicationState: CalendarFeedApplicationState,
    calendarSuper: CalendarSuper
) {
    LaunchedEffect(Unit) {
        calendarSuper.runRecurringHomeTasks()
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
                CalendarComposable(calendarSuper, calendarFeedApplicationState)
            }
            LifeBottomBar(calendarRepo, calendarFeedApplicationState)
        }
        DaySummaryPopUp(calendarRepo)
        BackupIssuesPopup(calendarRepo)
        MainFeedDatePicker(calendarFeedApplicationState)
    }
}