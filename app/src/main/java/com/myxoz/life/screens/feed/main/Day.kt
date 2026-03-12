package com.myxoz.life.screens.feed.main

import android.icu.util.Calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myxoz.life.LocalScreens
import com.myxoz.life.Theme
import com.myxoz.life.api.Syncable
import com.myxoz.life.api.syncables.ManualTransactionSyncable
import com.myxoz.life.screens.feed.instantevents.InstantEvent
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.utils.atStartAsMillis
import com.myxoz.life.utils.boxShadow
import com.myxoz.life.utils.toDp
import com.myxoz.life.viewmodels.CalendarViewModel
import com.myxoz.life.viewmodels.InspectedEventViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

const val screenTimeGoal = 1000f*3600*4f
const val stepsGoal = 6000f
@Composable
fun DayComposable(
    calendarViewModel: CalendarViewModel,
    inspectedEventViewModel: InspectedEventViewModel,
    date: LocalDate,
    fullWidth: Dp,
    fullHeight: Dp,
    fullHeightPx: Float
) {
    val zone = remember { ZoneId.systemDefault() }
    val startOfDay = remember { date.atStartOfDay(zone).toEpochSecond() * 1000L }
    val endOfDay = remember { date.plusDays(1).atStartOfDay(zone).toEpochSecond() * 1000L }

    // Fuck daylight saving time. This is mostly 24 but can be 23 oder 25, I hate calendars
    val hoursToday = remember { ((endOfDay - startOfDay) / (3600 * 1000)).toInt() }
    val calendar = remember { Calendar.getInstance() }
    val oneHourDp = fullHeight / hoursToday
    val oneHourPx = fullHeightPx / hoursToday
    val width = fullWidth * .97f
    Column(
        Modifier
            .width(fullWidth)
    ) {
        DayTopBar(calendarViewModel, date)
        Box(
            Modifier
                .weight(1f)
                .width(width)
        ) {
            BackgroundTiles(inspectedEventViewModel, hoursToday, startOfDay)
            Box( // Day Content
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
            ) {
                val isEditing by inspectedEventViewModel.isEditing.collectAsState()
                val instantEventSize = InstantEvent.INSTANTEVENTSIZE * oneHourDp

                RenderEvents(
                    calendarViewModel,
                    inspectedEventViewModel,
                    date,
                    isEditing,
                    oneHourDp,
                    instantEventSize,
                    startOfDay,
                    endOfDay,
                    width
                )

                RenderInstantEvents(
                    calendarViewModel,
                    inspectedEventViewModel,
                    date,
                    startOfDay,
                    endOfDay,
                    oneHourDp
                )

                RenderProposedEvents(
                    calendarViewModel,
                    date,
                    oneHourDp,
                    startOfDay,
                    endOfDay
                )

                RenderCurrentTimeCursor(
                    calendarViewModel,
                    calendar,
                    isEditing,
                    oneHourDp,
                    date
                )

                RenderEditing(
                    inspectedEventViewModel,
                    isEditing,
                    startOfDay,
                    endOfDay,
                    oneHourDp,
                    oneHourPx,
                    instantEventSize,
                    calendar
                )
            }
        }
    }
}
@Composable
private fun RenderCurrentTimeCursor(
    calendarViewModel: CalendarViewModel,
    calendar: Calendar,
    isEditing: Boolean,
    oneHourDp: Dp,
    date: LocalDate
) {
    val time by calendarViewModel.minuteFlow.collectAsStateWithLifecycle()
    calendar.timeInMillis = time
    val zone = remember { ZoneId.systemDefault() }
    val start = remember(date) { date.atStartAsMillis(zone) }
    // We render the cursor up to 2 days in each direction to support the cursor to smoothly move from one to another day
    if(abs(start - time) < 1000L * 3600 * 24 * 2) Box(
        Modifier
            .offset(y = (-5).dp + (time - start) / (3600f * 1000f) * oneHourDp)
            .height(10.dp)
            .alpha(if (isEditing) .3f else .7f)
            .fillMaxWidth()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Theme.primary, CircleShape)
                .align(Alignment.CenterStart)
                .height(2.dp)
        )
        Box(
            Modifier
                .background(Theme.primary, CircleShape)
                .align(Alignment.CenterStart)
                .size(10.dp)
        )
    }
}

@Composable
private fun RenderEvents(
    calendarViewModel: CalendarViewModel,
    inspectedEventViewModel: InspectedEventViewModel,
    date: LocalDate,
    isEditing: Boolean,
    oneHourDp: Dp,
    instantEventSize: Dp,
    startOfDay: Long,
    endOfDay: Long,
    width: Dp
) {
    val editedEvent by inspectedEventViewModel.event.collectAsState()
    val segments by calendarViewModel.getSegmentedEvents(date).collectAsState()
    val screens = LocalScreens.current
    for(segmentedEvent in segments) {
        if(isEditing && segmentedEvent.event.id == editedEvent.id) continue
        val haptic = LocalHapticFeedback.current
        segmentedEvent.Render(
            calendarViewModel,
            oneHourDp,
            instantEventSize,
            startOfDay,
            endOfDay,
            width,
            !isEditing,
            {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                inspectedEventViewModel.setEditing(true)
                inspectedEventViewModel.setInspectedEventTo(
                    segmentedEvent.event
                )
            }) {
            inspectedEventViewModel.setInspectedEventTo(segmentedEvent.event)
            screens.gotoEventDetails()
        }
    }
}

@Composable
private fun BackgroundTiles(
    inspectedEventViewModel: InspectedEventViewModel,
    hoursToday: Int,
    startOfDay: Long,
) {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(hoursToday){
            Box(
                Modifier
                    .padding(vertical = 2.dp)
                    .background(Theme.background, RoundedCornerShape(25))
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(null, null) {
                        inspectedEventViewModel.updateStartTs(startOfDay + it * 3600 * 1000L)
                    }
            )
        }
    }
}

@Composable
private fun RenderInstantEvents(
    calendarViewModel: CalendarViewModel,
    inspectedEventViewModel: InspectedEventViewModel,
    date: LocalDate,
    startOfDay: Long,
    endOfDay: Long,
    oneHourDp: Dp
){
    val syncable by inspectedEventViewModel.editedSyncable.collectAsState()
    val collectedInstantEvents by calendarViewModel.getInstantEventsForDay(syncable, date)
    for (group in collectedInstantEvents) {
        group.Render(startOfDay, endOfDay, oneHourDp)
    }
}

@Composable
private fun BoxScope.RenderProposedEvents(
    calendarViewModel: CalendarViewModel,
    date: LocalDate,
    oneHourDp: Dp,
    startOfDay: Long,
    endOfDay: Long,
) {
    val collectedProposedEvents by calendarViewModel.getProposedEventsAt(date).collectAsState()
    val coroutineScope = rememberCoroutineScope()
    for(event in collectedProposedEvents) {
        Box(Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .align(Alignment.CenterStart)) {
            event.Render(oneHourDp, startOfDay, endOfDay,{
                coroutineScope.launch {
                    calendarViewModel.removeProposedEvent(event)
                }
            }) {
                calendarViewModel.saveProposedEvent(event)
            }
        }
    }
}

@Composable
private fun BoxScope.RenderEditing(
    inspectedEventViewModel: InspectedEventViewModel,
    isEditing: Boolean,
    startOfDay: Long,
    endOfDay: Long,
    oneHourDp: Dp,
    oneHourPx: Float,
    instantEventSize: Dp,
    calendar: Calendar,
){
    val editedEvent by inspectedEventViewModel.event.collectAsState()
    val syncable by inspectedEventViewModel.editedSyncable.collectAsState()
    if (isEditing) {
        if (syncable == null && editedEvent.proposed.end > startOfDay && editedEvent.proposed.start < endOfDay) {
            RenderEventEditing(
                inspectedEventViewModel,
                startOfDay,
                endOfDay,
                oneHourDp,
                oneHourPx,
                calendar
            )
        } else {
            RenderEditSyncable(
                inspectedEventViewModel,
                syncable,
                startOfDay,
                endOfDay,
                instantEventSize,
                oneHourDp,
                oneHourPx
            )
        }
    }
}

@Composable
private fun BoxScope.RenderEventEditing(
    inspectedEventViewModel: InspectedEventViewModel,
    startOfDay: Long,
    endOfDay: Long,
    oneHourDp: Dp,
    hourInPx: Float,
    calendar: Calendar
){
    val screens = LocalScreens.current
    val editedEvent by inspectedEventViewModel.event.collectAsState()
    val colors = editedEvent.proposed.type.colors.bg
    val gradientBottom = remember(colors) {
        Brush.radialGradient(
            listOf(colors, Color.Transparent),
            Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY), 100f
        )
    }
    val gradientTop = remember {
        Brush.radialGradient(
            listOf(colors, Color.Transparent),
            Offset(0f, 0f), 100f
        )
    }

    suspend fun PointerInputScope.dragEvent(start: Boolean, end: Boolean) {
        var totalDrag = 0f
        var ev = inspectedEventViewModel.event.value
        this.detectVerticalDragGestures(
            onDragStart = {
                ev = inspectedEventViewModel.event.value; totalDrag = 0f
            }
        ) { _, it ->
            totalDrag += it;
            val offsetInMs =
                ((totalDrag / hourInPx * 4).toInt() * 900 * 1000L)
            inspectedEventViewModel.setInspectedEventTo(
                when {
                    !start && end -> {
                        ev.copyWithTimes(
                            start = ev.proposed.start,
                            end = (ev.proposed.end + offsetInMs).coerceAtLeast(
                                ev.proposed.start + 900 * 1000
                            )
                        )
                    }

                    start && !end -> {
                        ev.copyWithTimes(
                            start = (ev.proposed.start + offsetInMs).coerceAtMost(
                                ev.proposed.end - 900 * 1000
                            ),
                            end = editedEvent.proposed.end
                        )
                    }

                    else -> {
                        ev.copyWithTimes(
                            ev.proposed.start + offsetInMs,
                            ev.proposed.end + offsetInMs
                        )
                    }
                }
            )
        }
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
                .pointerInput("end") {
                    dragEvent(false, end = true)
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
                .background(gradientTop, RoundedCornerShape(10.dp))
                .pointerInput("start") {
                    dragEvent(true, end = false)
                },
            Alignment.Center
        ) {}
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(
                top = editedEvent.proposed.getTopPadding(
                    oneHourDp,
                    startOfDay
                )
            )
            .clickable(null, null) {
                screens.gotoEventDetails()
            }
            .pointerInput("move_newday") {
                dragEvent(true, end = true)
            }
            .height(
                editedEvent.proposed.getHeightDp(
                    oneHourDp,
                    startOfDay,
                    endOfDay
                )
            )
            .border(
                2.dp,
                editedEvent.proposed.type.colors.bg,
                RoundedCornerShape(10.dp)
            )
            .boxShadow(
                alpha = .25f,
                blur = 10.dp,
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        if (editedEvent.proposed.start >= startOfDay) topDragger()
        if (editedEvent.proposed.end <= endOfDay) bottomDragger()
        Box(
            Modifier
                .fillMaxSize()
                .alpha(.7f)
                .background(
                    editedEvent.proposed.getBackgroundBrush(6 / 7f), // Just no
                    RoundedCornerShape(10.dp)
                )
        ) {
            with(editedEvent.proposed) {
                RenderContent(
                    oneHourDp, startOfDay, endOfDay, false,
                    getBlockHeight(startOfDay, endOfDay)
                )
            }
        }
    }
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .offset(
                y = -FontSize.LARGE.size.toDp() + editedEvent.proposed.getTopPadding(
                    oneHourDp,
                    startOfDay
                ).run { if (this == 1.dp) (-2).dp else this })
            .clickable(null, null) {
                screens.gotoEventDetails()
            }
            .pointerInput("top_bar") {
                dragEvent(true, end = false)
            }
    ) {
        calendar.timeInMillis = editedEvent.proposed.start
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        Text(
            "${if (currentHour <= 9) "0" else ""}$currentHour:${if (currentMinute <= 9) "0" else ""}$currentMinute",
            Modifier
                .background(
                    editedEvent.proposed.type.colors.bg,
                    RoundedCornerShape(50, 50, 0, 0)
                )
                .padding(horizontal = 5.dp, vertical = 2.dp),
            color = editedEvent.proposed.type.selectedColor,
            textAlign = TextAlign.Right
        )
    }
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .offset(
                y = editedEvent.proposed.getTopPadding(
                    oneHourDp,
                    startOfDay
                ) + editedEvent.proposed.getHeightDp(
                    oneHourDp,
                    startOfDay,
                    endOfDay
                ) - 1.dp
            )
            .clickable(null, null) {
                screens.gotoEventDetails()
            }
            .pointerInput("top_bar") {
                dragEvent(false, end = true)
            }
    ) {
        calendar.timeInMillis = editedEvent.proposed.end
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        Text(
            "${if (currentHour <= 9) "0" else ""}$currentHour:${if (currentMinute <= 9) "0" else ""}$currentMinute",
            Modifier
                .background(
                    editedEvent.proposed.type.colors.bg,
                    RoundedCornerShape(0, 0, 50, 50)
                )
                .padding(horizontal = 5.dp, vertical = 2.dp),
            color = editedEvent.proposed.type.selectedColor,
            textAlign = TextAlign.Left
        )
    }
}

@Composable
private fun RenderEditSyncable(
    inspectedEventViewModel: InspectedEventViewModel,
    syncable: Syncable.DatedSyncable?,
    startOfDay: Long,
    endOfDay: Long,
    instantEventSize: Dp,
    oneHourDp: Dp,
    oneHourPx: Float
){
    val screens = LocalScreens.current
    if (syncable is ManualTransactionSyncable && syncable.timestamp <= endOfDay && syncable.timestamp >= startOfDay) {
        val instant = syncable.asInstantEvent() ?: return
        Box(
            Modifier
                .offset(y = ((instant.timestamp - startOfDay) / 3_600_000f) * oneHourDp - instantEventSize / 2)
                .height(instantEventSize)
                .fillMaxWidth()
                .pointerInput("start") {
                    var totalDrag = 0f
                    var ev: ManualTransactionSyncable =
                        inspectedEventViewModel.editedSyncable.value as? ManualTransactionSyncable
                            ?: return@pointerInput
                    this.detectVerticalDragGestures(
                        onDragStart = {
                            ev =
                                inspectedEventViewModel.editedSyncable.value as? ManualTransactionSyncable
                                    ?: return@detectVerticalDragGestures
                            totalDrag = 0f
                        }
                    ) { _, it ->
                        totalDrag += it
                        val offsetInMs =
                            ((totalDrag / oneHourPx * 3600L * 1000L)).toLong()
                        inspectedEventViewModel.updateStartTs(ev.timestamp + offsetInMs)
                    }
                },
            contentAlignment = Alignment.CenterEnd
        ) {
            instant.RenderContent(oneHourDp, true) {
                screens.gotoEventDetails()
            }
        }
    }
}