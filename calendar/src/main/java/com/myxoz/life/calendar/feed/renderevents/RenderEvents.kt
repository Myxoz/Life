package com.myxoz.life.calendar.feed.renderevents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import com.myxoz.life.calendar.feed.CalendarFeedApplicationState
import com.myxoz.life.calendar.feed.events.prerendered.PrerenderedEvent
import com.myxoz.life.calendar.search.SearchField
import com.myxoz.life.lifeui.providers.LocalNavController
import java.time.LocalDate

@Composable
internal fun RenderEvents(
    events: List<PrerenderedEvent>,
    search: SearchField,
    calendarFeedApplicationState: CalendarFeedApplicationState,
    date: LocalDate,
    isEditing: Boolean,
    oneHourDp: Dp,
    instantEventSize: Dp,
    startOfDay: Long,
    endOfDay: Long,
    width: Dp
) {
    val editedEvent by calendarFeedApplicationState.event.collectAsState()
    val segments by calendarFeedApplicationState.getSegmentedEvents(date)
    val nav = LocalNavController.current
    for(segmentedEvent in segments) {
        if(isEditing && segmentedEvent.key == editedEvent.id) continue
        val haptic = LocalHapticFeedback.current
        segmentedEvent.value.Render(
            calendarFeedApplicationState,
            oneHourDp,
            instantEventSize,
            startOfDay,
            endOfDay,
            width,
            !isEditing,
            {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                calendarFeedApplicationState.
                calendarSuper.setInspectedEventTo()
                calendarFeedApplicationState.editExistingEvent(segmentedEvent.value.event)
            }
        ) {
            calendarFeedApplicationState.setInspectedEventTo(segmentedEvent.value.event)
            screens.gotoEventDetails()
        }
    }
}
