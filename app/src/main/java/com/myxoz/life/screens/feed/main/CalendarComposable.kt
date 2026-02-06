package com.myxoz.life.screens.feed.main

import android.icu.util.Calendar
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.collectAsMutableState
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.viewmodels.CalendarViewModel
import com.myxoz.life.viewmodels.InspectedEventViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

val dateBarHeight = 50.dp
val fullDayBarHeight = 15.dp
val daySummaryHeight = 15.dp
val sidebarWidth = 40.dp
@Composable
fun CalendarComposable(calendarViewModel: CalendarViewModel, inspectedEventViewModel: InspectedEventViewModel, showDaySelector: ()->Unit) {
    val conf = LocalConfiguration.current
    val density = LocalDensity.current
    val today = LocalDate.now()
    val currentYear by calendarViewModel.currentYear.collectAsState()
    val currentMonth by calendarViewModel.currentMonth.collectAsState()
    val days by calendarViewModel.days.collectAsMutableState()
    val listState = calendarViewModel.lazyListState
    val isEditing by inspectedEventViewModel.isEditing.collectAsState()
    val displayedDays by calendarViewModel.dayAmount.collectAsState()
    val eachDayWidthGoal by remember(displayedDays) {
        mutableFloatStateOf(
            (conf.screenWidthDp-sidebarWidth.value+1)/displayedDays.toFloat()
        )
    }
    val eachDayWidth by animateFloatAsState(eachDayWidthGoal)
    val focusManager = LocalFocusManager.current
    val isSearching by calendarViewModel.search.isSearching.collectAsState()
    BackHandler(isSearching) {
        calendarViewModel.search.reset()
        focusManager.clearFocus()
    }
    BackHandler(isEditing) {
        inspectedEventViewModel.setEditing(false)
    }
    Row(
        Modifier
            .fillMaxSize()
    ) {
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
                        showDaySelector()
                    }
                ,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(currentMonth, style = TypoStyle(Theme.primary, FontSize.MEDIUM))
                Text(currentYear.toString(), style = TypoStyle(Theme.primary, FontSize.SMALL))
            }
            Box(
                Modifier.height(fullDayBarHeight)
            ) {
                var rotation by remember { mutableFloatStateOf(0f) }
                val coroutineScope = rememberCoroutineScope()
                LaunchedEffect(calendarViewModel.dayAmount, today) {
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
                        .size(fullDayBarHeight)
                        .background(Theme.primaryContainer, CircleShape)
                        .clip(CircleShape)
                        .rotate(rotation)
                        .rippleClick{
                            val index = days.indexOfFirst { today.isEqual(it) }
                            if(index==-1) {
                                calendarViewModel.days.value = listOf(today)
                            } else {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(index)
                                }
                            }
                        }
                    ,
                    Theme.onPrimaryContainer
                )
            }
            Spacer(Modifier)
            Box(
                Modifier,
                contentAlignment = Alignment.Center
            ){
                var timelineHeight by remember { mutableStateOf(100.dp) }
                val currentTime by calendarViewModel.minuteFlow.collectAsStateWithLifecycle()
                val calendar = remember { Calendar.getInstance() }
                calendar.timeInMillis = currentTime
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(Calendar.MINUTE)
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
                        "${if(currentHour<=9) "0" else ""}$currentHour:${if(currentMinute<=9) "0" else ""}$currentMinute",
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
        LazyRow(
            state=listState,
            flingBehavior = calendarViewModel.snapFlingBehavior
        ) {
            items(days, {it.toEpochDay()}) { day ->
                DayComposable(
                    calendarViewModel,
                    inspectedEventViewModel,
                    day == today,
                    day,
                    eachDayWidth.dp,
                )
            }
        }
    }
}