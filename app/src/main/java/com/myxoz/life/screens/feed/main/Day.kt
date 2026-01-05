package com.myxoz.life.screens.feed.main

import android.content.Context.MODE_PRIVATE
import android.icu.util.Calendar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.times
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myxoz.life.LocalAPI
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalSettings
import com.myxoz.life.LocalStorage
import com.myxoz.life.R
import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.dbwrapper.DaysEntity
import com.myxoz.life.events.EmptyEvent
import com.myxoz.life.events.ProposedEvent
import com.myxoz.life.screens.feed.dayoverview.getWeekDayByInt
import com.myxoz.life.screens.options.getUsageDataBetween
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.viewmodels.CalendarViewModel
import com.myxoz.life.viewmodels.InspectedEventViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.min

const val screenTimeGoal = 1000f*3600*4f
const val stepsGoal = 6000f
@Composable
fun DayComposable(
    viewModel: CalendarViewModel,
    inspectedEventViewModel: InspectedEventViewModel,
    isToday: Boolean,
    date: LocalDate,
    fullWidth: Dp,
){
    val navController = LocalNavController.current
    val db = LocalStorage.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val settings = LocalSettings.current
    var lastDayDragEnd by remember { mutableLongStateOf(0L) }
    val zone = remember { ZoneId.systemDefault() }
    val startOfDay = remember { date.atStartOfDay(zone).toEpochSecond()*1000L }
    val endOfDay = remember { date.plusDays(1).atStartOfDay(zone).toEpochSecond()*1000L }
    var dayEntity: DaysEntity? by remember { mutableStateOf(null) }
    var events by remember { mutableStateOf(viewModel.dayCache[date]?.toList() ?: listOf()) }
    val hoursToday = remember { ((endOfDay- startOfDay) / (3600*1000)).toInt() }
    var oneHourDp by remember { mutableStateOf(0.dp) }
    val hourInPx = with(density) { oneHourDp.toPx() }
    val calendar = remember { Calendar.getInstance() }
    var instantEvents by remember { mutableStateOf(viewModel.instantEventCache[date]?.toList() ?: listOf()) }
    var birthdayAmount by remember { mutableIntStateOf(0) }
    val lastUpdateTs by viewModel.lastEventUpdateTs.collectAsState()
    val width = fullWidth*.97f
    LaunchedEffect(Unit) {
        if (!isToday) {
            dayEntity = db.days.getDay(date.toEpochDay().toInt())
        } else {
            val calendar = Calendar.getInstance()
            val end = calendar.timeInMillis
            calendar.set(Calendar.MILLISECONDS_IN_DAY, 0)
            val start = calendar.timeInMillis
            dayEntity = DaysEntity(
                0,
                if(settings.features.screentime.hasAssured()) getUsageDataBetween(context, start, end).toInt() else 0,
                if(settings.features.stepCounting.has.value) context.getSharedPreferences("steps", MODE_PRIVATE).run {
                    getLong("saved_steps", 0L) - getLong("steps_at_midnight", 0L)
                }.toInt()  else 0,
                0,
                0,
                0
            )
        }
        birthdayAmount = db.people.getPeopleWithBirthdayAt(date).size
    }
    LaunchedEffect(lastUpdateTs) {
        withContext(Dispatchers.IO){
            val dbEvents = db.events.getEventsBetween(startOfDay, endOfDay).mapNotNull { SyncedEvent.from(db, it)}
            viewModel.dayCache[date] = dbEvents
            events = dbEvents

            val mix = InstantEvent.getEntriesForDay(db, startOfDay, endOfDay, viewModel)
            viewModel.instantEventCache[date] = mix
            instantEvents = mix
        }
    }
    Column(
        Modifier
            .width(fullWidth)
    ) {
        Column(
            Modifier
                .height(dateBarHeight)
                .fillMaxWidth()
            ,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = TypoStyle(if(isToday) FontColor.SELECTED else FontColor.SECONDARY, FontSize.MLARGE, FontFamily.Display)
                    .run { this.copy(fontWeight = if(isToday) FontWeight.Bold else FontWeight.Normal) }
            )
            Text(
                getWeekDayByInt(date.dayOfWeek.value - 1),
                style = TypoStyle(if(isToday) FontColor.SELECTED else FontColor.SECONDARY, FontSize.SMALLM)
                    .run { this.copy(fontWeight = if(isToday) FontWeight.Bold else FontWeight.Normal) }
            )
        }
        Box(
            Modifier
                .height(fullDayBarHeight)
        )
        Row (
            Modifier
                .height(daySummaryHeight)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .clip(CircleShape)
                .rippleClick{
                    navController.navigate("day/${date.toEpochDay()}/overview")
                },
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(birthdayAmount) {
                Icon(
                    painterResource(R.drawable.birthday),
                    "Birthday",
                    Modifier.fillMaxHeight(),
                    Colors.PRIMARYFONT
                )
            }
            val dayEntity = dayEntity
            val screentime by settings.features.screentime.has.collectAsState()
            val steps by settings.features.stepCounting.has.collectAsState()
            if(dayEntity!=null) {
                if (screentime || dayEntity.screenTimeMs != 0) {
                    DayPill(
                        painterResource(R.drawable.screentime),
                        "${dayEntity.screenTimeMs / 1000 / 3600}h ${dayEntity.screenTimeMs / 1000 / 60 % 60}m",
                        (dayEntity.screenTimeMs / screenTimeGoal).coerceIn(0f, 1f),
                        Colors.SCREENTIME
                    )
                }
                if (steps || dayEntity.steps != 0) {
                    DayPill(
                        painterResource(R.drawable.shoe),
                        "${dayEntity.steps}",
                        (dayEntity.steps / stepsGoal).coerceIn(0f, 1f),
                        Colors.STEPS
                    )
                }
            }
        }
        Box(
            Modifier
                .weight(1f)
                .width(width)
        ) {
            Column(
                Modifier
                    .onGloballyPositioned { coordinates ->
                        with(density) {
                            oneHourDp = (coordinates.size.height.toDp() / hoursToday)
                        }
                    }
                ,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(hoursToday){
                    Box(
                        Modifier
                            .padding(vertical = 2.dp)
                            .background(Colors.CALENDARBG, RoundedCornerShape(25))
                            .fillMaxWidth()
                            .weight(1f)
                            .clickable(null, null) {
                                inspectedEventViewModel.setInspectedEventTo(
                                    if(inspectedEventViewModel.isEditing.value) {
                                        inspectedEventViewModel.event.value.copyWithTimes(
                                            start = startOfDay+it*3600*1000L,
                                            end = startOfDay+it*3600*1000L + inspectedEventViewModel.event.value.proposed.length()
                                        )
                                    } else {
                                        SyncedEvent(-1L, 0L, null, EmptyEvent(
                                            startOfDay + it * 3600 * 1000L,
                                            startOfDay + (it + 1) * 3600 * 1000L,
                                            false, usl = false
                                        )
                                        )
                                    }
                                )
                                inspectedEventViewModel.setEditing(true)
                            }
                    )
                }
            }
            Box( // Day Content
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
            ) {
                val isEditing by inspectedEventViewModel.isEditing.collectAsState()
                val newEvent by inspectedEventViewModel.event.collectAsState()

                val instantEventDisplaySize = 1.5f
                val bankingSizeDp = instantEventDisplaySize*oneHourDp
                val segments = SegmentedEvent.Companion.getSegmentedEvents(
                    events,
                    instantEvents,
                    (instantEventDisplaySize * 3600L).toLong() * 1000L
                )
                for(segmentedEvent in segments) {
                    if(isEditing && segmentedEvent.event.id == newEvent.id) continue
                    val haptic = LocalHapticFeedback.current
                    segmentedEvent.Render(viewModel, events, oneHourDp, bankingSizeDp, startOfDay, endOfDay, width, !isEditing, {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        inspectedEventViewModel.setEditing(true)
                        inspectedEventViewModel.setInspectedEventTo(
                            segmentedEvent.event
                        )
                    }) {
                        inspectedEventViewModel.setInspectedEventTo(segmentedEvent.event)
                        navController.navigate("fullscreen_event")
                    }
                }
                for(instantEvent in instantEvents) {
                    Box(
                        Modifier
                            .padding(top=max(0.dp, ((instantEvent.timestamp - startOfDay)/3_600_000f)*oneHourDp - bankingSizeDp/2))
                            .height(instantEventDisplaySize*oneHourDp)
                            .fillMaxWidth()
                        ,
                        contentAlignment = Alignment.CenterEnd
                    ){
                        Column(
                            Modifier
                                .size(instantEventDisplaySize*oneHourDp)
                                .background(Colors.BACKGROUND.copy(.7f), CircleShape)
                                .clip(CircleShape)
                                .rippleClick{
                                    instantEvent.openDetails(
                                        navController
                                    )
                                }
                            ,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painterResource(instantEvent.icon),
                                "Card",
                                Modifier.height(oneHourDp/(if(instantEvent.icon == R.drawable.gpay) 3f else 2f)),
                                tint = Colors.PRIMARYFONT
                            )
                            Spacer(Modifier.height((if(instantEvent.icon == R.drawable.pay_with_card) 2.dp else 4.dp)))
                            Text(instantEvent.subText, style = TypoStyle(FontColor.PRIMARY, FontSize.XSMALL))
                        }
                        Box(
                            Modifier
                                .padding(end = instantEventDisplaySize*.9f*oneHourDp)
                                .background(Brush.horizontalGradient(listOf(Color.Transparent, Colors.PRIMARYFONT)), CircleShape)
                                .width(oneHourDp)
                                .padding(vertical = 1.dp)
                            ,
                        )
                    }
                }
                for(event in viewModel.proposedEvents) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth().align(Alignment.CenterStart)) {
                        event.render(oneHourDp, startOfDay, endOfDay,{viewModel.removeProposedEvent(event, context)}) {
                            viewModel.refreshEvents()
                        }
                    }
                }
                run {
                    val time by viewModel.minuteFlow.collectAsStateWithLifecycle()
                    calendar.timeInMillis = time
                    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                    val currentMinute = calendar.get(Calendar.MINUTE)
                    val totalHoursFloat = (currentHour * 60 + currentMinute) / 60f
                    if(isToday) Box(
                        Modifier
                            .offset(y= (-5).dp)
                            .offset(y = totalHoursFloat*oneHourDp)
                            .height(10.dp)
                            .alpha(if(isEditing) .3f else .7f)
                            .fillMaxWidth()
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(Colors.SELECTED, CircleShape)
                                .align(Alignment.CenterStart)
                                .height(2.dp)
                        )
                        Box(
                            Modifier
                                .background(Colors.SELECTED, CircleShape)
                                .align(Alignment.CenterStart)
                                .size(10.dp)
                        )
                    }
                }
                if(isEditing && newEvent.proposed.end > startOfDay && newEvent.proposed.start < endOfDay) {
                    val gradientBottom = remember {
                        Brush.radialGradient( listOf( Colors.SELECTED, Color.Transparent ),
                            Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY), 100f )
                    }
                    val gradientTop = remember {
                        Brush.radialGradient( listOf( Colors.SELECTED, Color.Transparent ),
                            Offset(0f, 0f), 100f )
                    }
                    val bottomDragger: @Composable BoxScope.() -> Unit = @Composable {
                        Box(
                            Modifier
                                .fillMaxWidth(0.4f)
                                .height(oneHourDp * 2)
                                .align(Alignment.BottomEnd)
                                .background(
                                    gradientBottom,
                                    RoundedCornerShape(10.dp)
                                )
                                .pointerInput("end", lastDayDragEnd) {
                                    var totalDrag = 0f; var ev = newEvent
                                    this.detectVerticalDragGestures(
                                        onDragStart = { ev = inspectedEventViewModel.event.value; totalDrag = 0f },
                                        onDragEnd = { lastDayDragEnd = System.currentTimeMillis() }
                                    ) { _, it ->
                                        totalDrag += it; val offsetInMs = ((totalDrag / hourInPx * 4).toInt() * 900 * 1000L)
                                        inspectedEventViewModel.setInspectedEventTo(ev.copyWithTimes(
                                            start = ev.proposed.start,
                                            end = (ev.proposed.end + offsetInMs).coerceAtLeast(ev.proposed.start + 900*1000)
                                        ) )
                                    }
                                },
                            Alignment.Center
                        ) {}
                    }
                    val topDragger: @Composable BoxScope.() -> Unit = @Composable {
                        Box(
                            Modifier
                                .fillMaxWidth(0.4f)
                                .height(oneHourDp * 2)
                                .align(Alignment.TopStart)
                                .background( gradientTop, RoundedCornerShape(10.dp) )
                                .pointerInput("start", lastDayDragEnd) {
                                    var totalDrag = 0f; var ev = inspectedEventViewModel.event.value
                                    this.detectVerticalDragGestures(
                                        onDragStart = { ev = inspectedEventViewModel.event.value; totalDrag = 0f },
                                        onDragEnd = { lastDayDragEnd = System.currentTimeMillis() }
                                    ) { _, it ->
                                        totalDrag += it; val offsetInMs =((totalDrag / hourInPx * 4).toInt() * 900 * 1000L)
                                        inspectedEventViewModel.setInspectedEventTo(ev.copyWithTimes(
                                            start = (ev.proposed.start + offsetInMs).coerceAtMost(ev.proposed.end - 900*1000),
                                            end = newEvent.proposed.end
                                        ))
                                    }
                                },
                            Alignment.Center
                        ) {}
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding( top = newEvent.proposed.getTopPadding(oneHourDp, startOfDay) )
                            .clickable(null,null){
                                navController.navigate("fullscreen_event")
                            }
                            .pointerInput("move_newday", lastDayDragEnd) {
                                var totalDrag = 0f; var ev = inspectedEventViewModel.event.value
                                this.detectVerticalDragGestures(
                                    onDragStart = { ev = inspectedEventViewModel.event.value; totalDrag = 0f },
                                    onDragEnd = { lastDayDragEnd = System.currentTimeMillis() }
                                ) { _, it ->
                                    totalDrag += it
                                    val offsetInMs = ((totalDrag / hourInPx * 4).toInt() * 900 * 1000L)
                                    inspectedEventViewModel.setInspectedEventTo(ev.copyWithTimes(
                                        ev.proposed.start + offsetInMs,
                                        ev.proposed.end + offsetInMs
                                    ))
                                }
                            }
                            .height(newEvent.proposed.getHeightDp(oneHourDp, startOfDay, endOfDay))
                            .border(2.dp, Colors.SELECTED, RoundedCornerShape(10.dp))
                            .background(newEvent.proposed.getBackgroundBrush(.5f), RoundedCornerShape(10.dp))
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                        ) {
                            with(newEvent.proposed){
                                RenderContent(oneHourDp, startOfDay, endOfDay, false,
                                    newEvent.proposed.getBlockHeight(startOfDay, endOfDay)
                                )
                            }
                        }
                        if (newEvent.proposed.start >= startOfDay) topDragger()
                        if (newEvent.proposed.end <= endOfDay) bottomDragger()
                    }
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .offset(y= -FontSize.LARGE.size.toDp() + newEvent.proposed.getTopPadding(oneHourDp, startOfDay).run { if(this==1.dp) (-2).dp else this })
                            .clickable(null,null){
                                navController.navigate("fullscreen_event")
                            }
                    ) {
                        calendar.timeInMillis = newEvent.proposed.start
                        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                        val currentMinute = calendar.get(Calendar.MINUTE)
                        Text(
                            "${if(currentHour<=9) "0" else ""}$currentHour:${if(currentMinute<=9) "0" else ""}$currentMinute",
                            Modifier
                                .background(newEvent.proposed.type.color, RoundedCornerShape(50, 50, 0, 0))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                            ,
                            color = newEvent.proposed.type.selectedColor,
                            textAlign = TextAlign.Right
                        )
                    }
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .offset(y= newEvent.proposed.getTopPadding(oneHourDp, startOfDay) + newEvent.proposed.getHeightDp(oneHourDp, startOfDay, endOfDay) - 1.dp)
                            .clickable(null,null){
                                navController.navigate("fullscreen_event")
                            }
                    ) {
                        calendar.timeInMillis = newEvent.proposed.end
                        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                        val currentMinute = calendar.get(Calendar.MINUTE)
                        Text(
                            "${if(currentHour<=9) "0" else ""}$currentHour:${if(currentMinute<=9) "0" else ""}$currentMinute",
                            Modifier
                                .background(newEvent.proposed.type.color, RoundedCornerShape(0, 0, 50, 50))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                            ,
                            color = newEvent.proposed.type.selectedColor,
                            textAlign = TextAlign.Left
                        )
                    }
                }
            }
        }
    }
}

fun Int.msToDisplay(ignoreSeconds: Boolean=false): String {
    val t = this/1000
    val h = (t/3600)
    val m = (t/60)%60
    val s = t%60
    return "" +
            (if(h!=0) "${h}h" else "") +
            "${if(h!=0 && m!=0) if(m<=9) " 0" else " " else ""}${if(m!=0) "${m}m" else ""}" +
            if(!ignoreSeconds) "${if((h!=0 || m!=0) && s<=9) " 0" else " "}${s}s" else ""
}

@Composable
fun DayPill(imageVector: Painter, text: String, progress: Float, color: Color){
    Box(
        Modifier
            .height(daySummaryHeight)
            .background(Colors.DAYPILLBG, CircleShape)
            .widthIn(40.dp)
            .clip(CircleShape)
    ) {
        Box(
            Modifier
                .matchParentSize()
        ) {
            Box(
                Modifier
                    .background(color, CircleShape)
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
            )
        }
        Row(
            Modifier
                .fillMaxSize()
                .widthIn(40.dp)
                .padding(horizontal = 2.dp)
                .padding(end = 5.dp)
        ,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(imageVector, null,
                Modifier
                    .size(daySummaryHeight)
                    .padding(2.dp),
                Colors.PRIMARYFONT
            )
            Spacer(Modifier.width(5.dp))
            Text(text, style = TypoStyle(FontColor.PRIMARY, FontSize.SMALL).copy(fontWeight = FontWeight.Bold))
        }
    }
}
open class DefinedDurationEvent(val start: Long, val end: Long) {
    fun length() = (end - start).coerceAtLeast(15*1000L*60)
    fun getBlockHeight(startOfDay: Long, endOfDay: Long): Int = (min(length(), min(endOfDay - start, end - startOfDay))/(900*1000)).toInt()
    fun getBlockLength() = (length() / (900*1000)).toInt()
    fun getTopPadding(oneHour: Dp, startOfDay: Long) = ((this.start - startOfDay).coerceAtLeast(0L) / (3600 * 1000L).toFloat()) * oneHour + 1.dp
    fun getHeightDp(oneHour: Dp, startOfDay: Long, endOfDay: Long) = oneHour*(getBlockHeight(startOfDay, endOfDay)/4f) - 1.dp
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProposedEvent.render(oneHour: Dp, startOfDay: Long, endOfDay: Long, removeProposedEvent: ()->Unit, rerenderDay: () -> Unit){
    var visible by remember { mutableStateOf(true) }
    val db = LocalStorage.current
    val api = LocalAPI.current
    AnimatedVisibility(visible, exit = fadeOut(tween(500))) {
        Box(
            Modifier
                .padding(top = getTopPadding(oneHour, startOfDay))
                .height(getHeightDp(oneHour, startOfDay, endOfDay))
                .background(type.color.copy(.5f), RoundedCornerShape(10.dp))
                .fillMaxWidth()
        ) {
            Box(
                Modifier.fillMaxSize().alpha(.5f)
            ) {
                RenderContent(oneHour, startOfDay, endOfDay, false,
                    getBlockHeight(startOfDay, endOfDay)
                )
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(10.dp)).fillMaxSize(),
            ) {
                val coroutine = rememberCoroutineScope()
                Box(
                    Modifier.fillMaxHeight().weight(1f).background(Colors.ACCEPT).clip(RoundedCornerShape(10.dp)).rippleClick{
                        coroutine.launch {
                            if (this@render.insertAndSyncEvent(
                                    db,
                                    api,
                                    API.generateId(),
                                    System.currentTimeMillis(),
                                    null
                                )
                            ) {
                                removeProposedEvent()
                                rerenderDay()
                            }
                        }
                    }
                )
                Box(
                    Modifier.fillMaxHeight().weight(1f).background(Colors.DECLINE).clip(RoundedCornerShape(10.dp)).combinedClickable(onLongClick = {
                        removeProposedEvent(); visible = false
                    }){}
                )
            }
        }
    }
}