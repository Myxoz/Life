package com.myxoz.life.calendar.feed

import android.icu.util.Calendar
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myxoz.life.LocalStorage
import com.myxoz.life.api.SyncedEvent
import com.myxoz.life.calendar.getMonthByCalendarMonth
import com.myxoz.life.dbwrapper.BankingEntity
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.viewmodels.CalendarViewModel
import com.myxoz.life.viewmodels.InspectedEventViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

val dateBarHeight = 50.dp
val fullDayBarHeight = 15.dp
val daySummaryHeight = 15.dp
val sidebarWidth = 40.dp
@Composable
fun CalendarComposable(calendarViewModel: CalendarViewModel, inspectedEventViewModel: InspectedEventViewModel) {
    val db = LocalStorage.current
    val conf = LocalConfiguration.current
    val density = LocalDensity.current
    val today = LocalDate.now()
    val calendar = remember { Calendar.getInstance() }
    var currentYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(getMonthByCalendarMonth(calendar.get(Calendar.MONTH))) }
    val days = calendarViewModel.days
    val listState = rememberLazyListState()
    val isEditing by inspectedEventViewModel.isEditing.collectAsState()
    val displayedDays by calendarViewModel.dayAmount.collectAsState()
    val context = LocalContext.current
    val flingBehavior = remember {
        snapFlingBehavior(
            SnapLayoutInfoProvider(listState, SnapPosition.Start),
            exponentialDecay(3f),
            spring(stiffness = Spring.StiffnessMediumLow)
        )
    }

    val eachDayWidthGoal by remember(displayedDays) {
        mutableFloatStateOf(
            (conf.screenWidthDp-sidebarWidth.value+1)/displayedDays.toFloat()
        )
    }
    val eachDayWidth by animateFloatAsState(eachDayWidthGoal)
    val coroutineScope = rememberCoroutineScope()
    val zone = remember { ZoneId.systemDefault() }
    fun preloadDay(date: LocalDate){
        val startOfDay = date.atStartOfDay(zone).toEpochSecond()*1000L
        val endOfDay = date.plusDays(1).atStartOfDay(zone).toEpochSecond()*1000L
        coroutineScope.launch {
            val dbEvents = db.events.getEventsBetween(startOfDay, endOfDay).mapNotNull { SyncedEvent.from(db, it)}
            calendarViewModel.dayCache[date] = dbEvents

            val entries = BankingEntity.getAllBankingEntriesFor(db, startOfDay, endOfDay, calendarViewModel.futureBankEntries)
            calendarViewModel.bankingEntityCache[date] = entries
        }
    }
    fun onDayScrolled(index: Int) {
        val earliest = days[0]
        val latest = days.last()
        val current = days[index]
        if(index < 10) {
            repeat(10){
                val newDay = earliest.minusDays(it.toLong()+1)
                if(!days.contains(newDay)) {
                    days.add(0, newDay)
                    preloadDay(newDay)
                }
            }
        }
        if(days.size - index - displayedDays < 10) {
            repeat(10){
                val newDay = latest.plusDays(it.toLong()+1)
                if(!days.contains(newDay)) {
                    days.add(newDay)
                    preloadDay(newDay)
                }
            }
        }
        currentMonth = getMonthByCalendarMonth(current.monthValue-1)
        currentYear = current.year
    }

    LaunchedEffect(Unit) {
        calendarViewModel.initialize(context)

        if(days.isEmpty()) {
            val date = LocalDate.ofEpochDay(
                db.prefs.getLong("visible_date", LocalDate.now().toEpochDay())
            )
            days.add(date)
            onDayScrolled(0)
            listState.scrollToItem(days.indexOfFirst { it == date })
        } else {
            onDayScrolled(0)
        }
        if(!calendarViewModel.search.isSearching()) calendarViewModel.search.reset()
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                onDayScrolled(index)
            }
    }

    BackHandler(isEditing) {
        inspectedEventViewModel.setEditing(false)
    }
    val focusManager = LocalFocusManager.current
    BackHandler(calendarViewModel.search.isSearching()) {
        calendarViewModel.search.reset()
        focusManager.clearFocus()
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
                        calendarViewModel.selectDayPopup.value = true
                    }
                ,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(currentMonth, style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM))
                Text(currentYear.toString(), style = TypoStyle(FontColor.SECONDARY, FontSize.SMALL))
            }
            Box(
                Modifier.height(fullDayBarHeight)
            ) {
                var rotation by remember { mutableFloatStateOf(0f) }
                val coroutineScope = rememberCoroutineScope()
                LaunchedEffect(calendarViewModel.dayAmount, today) {
                    snapshotFlow { listState.firstVisibleItemScrollOffset to listState.firstVisibleItemIndex }.collect {
                        val itemProgress = it.first / (eachDayWidth * density.density)
                        rotation = if(days[it.second].isEqual(today)) {
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

                LaunchedEffect(today) {
                    snapshotFlow { listState.firstVisibleItemIndex }.collect {
                        db.prefs.edit {
                            putLong("visible_date", days[it].toEpochDay())
                        }
                    }
                }
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    "Today",
                    Modifier
                        .size(fullDayBarHeight)
                        .background(Colors.SECONDARY, CircleShape)
                        .clip(CircleShape)
                        .rotate(rotation)
                        .rippleClick{
                            val index = days.indexOfFirst { today.isEqual(it) }
                            if(index==-1) {
                                days.clear()
                                days.add(today)
                            } else {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(index)
                                }
                            }
                        }
                    ,
                    Colors.SELECTED
                )
            }
            Spacer(Modifier)
            Box(
                Modifier,
                contentAlignment = Alignment.Center
            ){
                var timelineHeight by remember { mutableStateOf(100.dp) }
                val currentTime by calendarViewModel.minuteFlow.collectAsStateWithLifecycle()
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
                        val within15m = abs((hour * 60) - totalMinutes) < 20
                        val label = "${if (hour > 9) "" else "0"}$hour:00"
                        Text(
                            label,
                            Modifier
                                .weight(1f)
                                .offset(0.dp, -offset)
                            ,
                            style = TypoStyle(if(within15m) FontColor.TERTIARY else FontColor.SECONDARY, FontSize.SMALL)
                        )
                    }
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .offset(
                            y = daySummaryHeight + (timelineHeight * totalHoursFloat)
                        )
                ) {
                    Text(
                        "${if(currentHour<=9) "0" else ""}$currentHour:${if(currentMinute<=9) "0" else ""}$currentMinute",
                        Modifier
                            .fillMaxWidth()
                            .offset(0.dp, -offset),
                        style = TypoStyle(FontColor.SELECTED, FontSize.SMALL)
                            .copy(fontWeight = FontWeight.W900),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        LazyRow(
            state=listState,
            flingBehavior = flingBehavior
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