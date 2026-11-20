package com.myxoz.life.calendar.feed.events

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.myxoz.life.calendar.CalendarSuper
import com.myxoz.life.calendar.feed.CalendarFeedApplicationState
import com.myxoz.life.calendar.feed.DayComposable
import com.myxoz.life.lifeui.reusedcompose.dpHeight
import com.myxoz.life.lifeui.reusedcompose.dpWidth
import com.myxoz.life.lifeui.reusedcompose.toDp
import com.myxoz.life.lifeui.reusedcompose.windowPadding
import com.myxoz.life.lifeui.theme.FontSize
import com.myxoz.life.lifeui.time.updatingToday

internal val dateBarHeight = 50.dp
internal val paddingBetweenSummaryAndDate = 15.dp
internal val daySummaryHeight = 15.dp
internal val sidebarWidth = 40.dp

@Composable
fun CalendarComposable(
    calendarSuper: CalendarSuper,
    calendarFeedApplicationState: CalendarFeedApplicationState
) {
    val screenWidthDp = LocalWindowInfo.current.dpWidth
    val today by updatingToday()
    val displayedDays by calendarFeedApplicationState.dayAmount.collectAsState()
    val eachDayWidthGoal = (screenWidthDp.value - sidebarWidth.value + 1) / displayedDays.toFloat()
    val eachDayWidth by animateFloatAsState(eachDayWidthGoal)
    calendarFeedApplicationState.ObserveLazyListState()
    CalendarBackHandler(calendarSuper)
    Row(
        Modifier
            .fillMaxSize()
    ) {
        CalendarTimeline(calendarFeedApplicationState, today, eachDayWidth, displayedDays)

        val days by calendarFeedApplicationState.days.collectAsState()
        val insets = windowPadding
        val screenHeight = LocalWindowInfo.current.dpHeight
        val density = LocalDensity.current
        val fullDayDp = screenHeight -
                insets.calculateTopPadding() -
                insets.calculateBottomPadding() -
                (FontSize.LARGE.size.toDp() + 34.dp) - // 20.dp + 2 * 7.dp bottom bar
                (daySummaryHeight + dateBarHeight + paddingBetweenSummaryAndDate)
        val fullDayHeightPx = with(density) { fullDayDp.toPx() }
        LazyRow(
            state = calendarFeedApplicationState.lazyListState,
            flingBehavior = calendarFeedApplicationState.snapFlingBehavior
        ) {
            items(days, { it }) { day ->
                DayComposable(
                    calendarSuper,
                    calendarFeedApplicationState,
                    day,
                    eachDayWidth.dp,
                    fullDayDp,
                    fullDayHeightPx
                )
            }
        }
    }
}
