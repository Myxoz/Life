package com.myxoz.life.calendar.feed.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myxoz.life.calendar.feed.CalendarFeedApplicationState
import com.myxoz.life.lifeui.providers.Theme
import com.myxoz.life.lifeui.reusedcompose.rippleClick
import com.myxoz.life.lifeui.reusedcompose.toDp
import com.myxoz.life.lifeui.theme.FontSize
import com.myxoz.life.lifeui.theme.TypoStyle
import com.myxoz.life.presentation.R
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

@Composable
internal fun CalendarTimeline(
    calendarFeedApplicationState: CalendarFeedApplicationState,
    today: LocalDate,
    eachDayWidth: Float,
    displayedDays: Int
) {
    val listState = calendarFeedApplicationState.lazyListState
    val density = LocalDensity.current
    val currentYear by calendarFeedApplicationState.currentYear.collectAsState()
    val currentMonth by calendarFeedApplicationState.currentShortMonth.collectAsState()
    Column(
        Modifier
            .width(sidebarWidth)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier
                .height(dateBarHeight)
                .clickable(null, null){
                    calendarFeedApplicationState.isSelectDayVisible.value = true
                }
            ,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(currentMonth, style = TypoStyle(Theme.primary, FontSize.MEDIUM))
            Text(currentYear.toString(), style = TypoStyle(Theme.primary, FontSize.SMALL))
        }
        Box(
            Modifier.height(paddingBetweenSummaryAndDate)
        ) {
            var rotation by remember { mutableFloatStateOf(0f) }
            val coroutineScope = rememberCoroutineScope()
            val days by calendarFeedApplicationState.days.collectAsState()
            LaunchedEffect(calendarFeedApplicationState.dayAmount, today) {
                snapshotFlow { listState.firstVisibleItemScrollOffset to listState.firstVisibleItemIndex }.collect {
                    val itemProgress = it.first / (eachDayWidth * density.density)
                    val day = days.getOrNull(it.second)?:return@collect
                    rotation = if(day.isEqual(today)) {
                        90f * itemProgress
                    } else if(days[it.second].isAfter(today)) {
                        90f
                    } else if(days.getOrNull(it.second+displayedDays)?.isEqual(today)==true){
                        -90f * (1 - itemProgress)
                    } else if(days.getOrNull(it.second+displayedDays)?.isBefore(today)==true){
                        -90f
                    } else {
                        0f
                    }
                }
            }
            Icon(
                painterResource(R.drawable.drop_down),
                "Today",
                Modifier
                    .size(paddingBetweenSummaryAndDate)
                    .background(Theme.primaryContainer, CircleShape)
                    .clip(CircleShape)
                    .rotate(rotation)
                    .rippleClick {
                        val index = days.indexOfFirst { today.isEqual(it) }
                        if (index == -1) {
                            calendarFeedApplicationState.days.value = listOf(today)
                        } else {
                            coroutineScope.launch {
                                listState.scrollToItem(index)
                            }
                        }
                    },
                Theme.onPrimaryContainer
            )
        }
        Spacer(Modifier)
        Box(
            Modifier,
            contentAlignment = Alignment.Center
        ){
            var timelineHeight by remember { mutableStateOf(100.dp) }
            val currentTime by calendarFeedApplicationState.minuteFlow.collectAsStateWithLifecycle(Instant.now())
            val zoned by remember { derivedStateOf { currentTime.atZone(ZoneId.systemDefault()) } }
            val currentHour = zoned.hour
            val currentMinute = zoned.minute
            val totalMinutes = currentHour * 60 + currentMinute
            val totalHoursFloat = totalMinutes / 60f
            val offset = FontSize.SMALL.size.toDp() / 2f
            Column(
                Modifier
                    .onGloballyPositioned{ coordinates ->
                        with(density) {
                            timelineHeight = ((coordinates.size.height.toDp() - daySummaryHeight) / 24)
                        }
                    }
                ,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(daySummaryHeight))
                repeat(24) { hour ->
                    val within30m = abs((hour * 60) - totalMinutes) < 30
                    val label = "${if (hour > 9) "" else "0"}$hour:00"
                    Text(
                        label,
                        Modifier
                            .weight(1f)
                            .offset(0.dp, -offset)
                        ,
                        style = TypoStyle(if(within30m) Theme.tertiary.copy(.5f) else Theme.secondary, FontSize.SMALL)
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .offset(
                        y = daySummaryHeight + (timelineHeight * totalHoursFloat)
                    )
                ,
            ) {
                Text(
                    currentHour.toString().padStart(2, '0')+":"+
                    currentMinute.toString().padStart(2, '0'),
                    Modifier
                        .fillMaxWidth()
                        .offset(0.dp, -offset - 2.dp)
                        .padding(horizontal = 4.dp)
                        .background(Theme.primary, CircleShape)
                        .padding(vertical = 2.dp)
                    ,
                    style = TypoStyle(Theme.onPrimary, FontSize.SMALL)
                        .copy(fontWeight = FontWeight.W900),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}