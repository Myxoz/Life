package com.myxoz.life.calendar.feed.events

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalFocusManager
import com.myxoz.life.calendar.CalendarSuper

@Composable
internal fun CalendarBackHandler(calendarSuper: CalendarSuper) {
    val focusManager = LocalFocusManager.current
    BackHandler(calendarSuper.search.isSearching) {
        calendarSuper.search.reset()
        focusManager.clearFocus()
    }
    val isEditing by calendarSuper.state.collectAsState()
    BackHandler(isEditing is CalendarSuper.CalendarEventState.Editing) {
        calendarSuper.discardChanges()
    }
}
