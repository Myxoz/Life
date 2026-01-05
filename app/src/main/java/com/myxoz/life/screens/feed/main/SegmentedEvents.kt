package com.myxoz.life.screens.feed.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalStorage
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.viewmodels.CalendarViewModel


data class SegmentedEvent(val event: SyncedEvent, val isFullWidth: Boolean, val isLeft: Boolean, val hasContent: Boolean, val segmentStart: Long, val segmentEnd: Long): DefinedDurationEvent(segmentStart, segmentEnd) {
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Render(calendarViewModel: CalendarViewModel, allEvents: List<SyncedEvent>, oneHour: Dp, bankingSizeDp: Dp, startOfDay: Long, endOfDay: Long, width: Dp, isClickEnabled: Boolean, editEvent: ()->Unit, openEventDetails: ()->Unit){
        val lastUpdated by calendarViewModel.search.lastUpdated.collectAsState()
        var isSearched by remember { mutableStateOf(true) }
        val db = LocalStorage.current
        LaunchedEffect(lastUpdated) {
            isSearched = calendarViewModel.search.isSearched(db, this@SegmentedEvent.event.proposed, allEvents)
        }
        Box(
            Modifier
                .padding(
                    top = getTopPadding(oneHour, startOfDay),
                    start = if(!isLeft) width-bankingSizeDp else 0.dp
                )
                .height(getHeightDp(oneHour, startOfDay, endOfDay))
                .width(if(isFullWidth) width else if(isLeft) width-bankingSizeDp else bankingSizeDp)
                .alpha(if(isSearched) 1f else 0.3f)
                .clip(RoundedCornerShape(10.dp))
                .combinedClickable(remember { MutableInteractionSource() }, ripple(), isClickEnabled, onClick = openEventDetails, onLongClick = editEvent)
                .background(event.proposed.getBackgroundBrush(), RoundedCornerShape(10.dp))
        ) {
            if(hasContent)
                with(event.proposed) {
                    RenderContent(oneHour, startOfDay, endOfDay, !isLeft, this@SegmentedEvent.getBlockHeight(startOfDay, endOfDay))
                }
        }
    }
    companion object {
        fun getSegmentedEvents(events: List<SyncedEvent>, instantEntries: List<InstantEvent>, instantEventDisplaySize: Long): List<SegmentedEvent> {
            val order = arrayOf(
                EventType.DigSoc,
                EventType.Social,
                EventType.Sleep,
                EventType.Spont,
                EventType.Hobby,
                EventType.Learn,
                EventType.Travel,
            )
            val modifyable = mutableListOf<SegmentedEvent>()
            for (type in order) {
                for(event in events.filter { it.proposed.type == type }){
                    var canBeFullWidth = true
                    modifyable.forEachIndexed { i, it ->
                        if(event.proposed.overlaps(it.event.proposed)) {
                            canBeFullWidth = false
                            modifyable[i] = it.copy(isFullWidth = false, isLeft = false)
                        }
                    }
                    modifyable.add(SegmentedEvent(event, canBeFullWidth, true, hasContent = true, event.proposed.start, event.proposed.end))
                }
            }
            for(event in instantEntries) {
                modifyable.forEachIndexed { i, it ->
                    if(it.event.proposed.overlaps(
                            DefinedDurationEvent(
                                event.timestamp - instantEventDisplaySize / 2,
                                event.timestamp + instantEventDisplaySize / 2
                            )
                        )) {
                        modifyable[i] = it.copy(isFullWidth = false)  // Does not set left or right align
                    }
                }
            }
            // Now iterate the list and try to fill spots
            for(seg in modifyable.filter { !it.isLeft }){
                val ev = seg.event.proposed
                val overlappingEvents = modifyable.filter { it.event.proposed.overlaps(ev) }.toMutableList()
                // Including self
                val overlappList = (overlappingEvents.mapNotNull {
                    if(it.start >= ev.start) it to true else null
                } + overlappingEvents.mapNotNull {
                    if(it.end <= ev.end) it to false else null
                }).sortedBy { if(it.second) it.first.start else it.first.end }
                val parted = mutableListOf<Pair<Long, Long>>()
                var start = ev.start
                var overlapAmount = overlappingEvents.filter { it.start < ev.start }.size
                for(ov in overlappList) {
                    if(overlapAmount == 1) {
                        parted.add(start to if(ov.second) ov.first.start else ov.first.end)
                    }
                    overlapAmount += if(ov.second) 1 else -1
                    if(overlapAmount == 1) {
                        start = if(ov.second) ov.first.start else ov.first.end
                    }
                }
                val parts = parted.filter { it.second != it.first }.toMutableList()
                if(parts.size > 1){
                    modifyable.remove(seg)
                    val longest = parts.sortedByDescending { it.second - it.first }[0]  // Cant be null
                    parts.remove(longest)
                    modifyable.add(
                        seg.copy(isFullWidth = true,  isLeft = true, hasContent = true, segmentStart = longest.first, segmentEnd = longest.second)
                    )
                    for(part in parts){
                        if(part.first < seg.event.proposed.end) modifyable.add(
                            seg.copy(isFullWidth = true,  isLeft = true, hasContent = false, segmentStart = part.first, segmentEnd = part.second)
                        )
                    }
                    modifyable.add(seg.copy(
                        hasContent = false,
                        isLeft = false,
                        isFullWidth = false
                    )) // Right no content full time event add last for it to be lowest
                }
            }
            return modifyable.reversed() // So higher prio events are rendered above lower ones
        }
    }
}