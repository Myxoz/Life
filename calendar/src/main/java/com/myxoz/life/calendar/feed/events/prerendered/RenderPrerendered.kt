package com.myxoz.life.calendar.feed.events.prerendered

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import com.myxoz.life.calendar.feed.CalendarFeedApplicationState
import com.myxoz.life.calendar.feed.instantevents.RenderInstantEvents
import com.myxoz.life.calendar.feed.renderevents.RenderEvents
import com.myxoz.life.core.atStartAsInstant
import com.myxoz.life.lifecore.instantevents.InstantEventOpener
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun RenderPrerenderable(
    date: LocalDate,
    calendarFeedApplicationState: CalendarFeedApplicationState,
    instantEventOpener: InstantEventOpener,
    oneHourDp: Dp,
) {
    val prerenderedCalendarContent by calendarFeedApplicationState.getPrerenderedContent(date).collectAsState()
    RenderEvents(prerenderedCalendarContent.events)
    RenderInstantEvents(
        prerenderedCalendarContent?.instantEvents ?: listOf(),
        instantEventOpener,
        oneHourDp,
        date.atStartAsInstant(ZoneId.systemDefault())
    )
}