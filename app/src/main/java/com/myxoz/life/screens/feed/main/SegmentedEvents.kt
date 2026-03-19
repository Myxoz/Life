package com.myxoz.life.screens.feed.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.events.ProposedEvent
import com.myxoz.life.events.additionals.DefinedDurationEvent
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.events.additionals.PeopleEvent
import com.myxoz.life.screens.feed.instantevents.InstantEvent
import com.myxoz.life.viewmodels.CalendarViewModel
import java.util.LinkedList


data class PrerenderedEvent(
    val event: SyncedEvent,
    val segments: List<Segment>,
    val nextToPeople: Set<Long>
) {
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Render(calendarViewModel: CalendarViewModel, oneHour: Dp, bankingSizeDp: Dp, startOfDay: Long, endOfDay: Long, width: Dp, isClickEnabled: Boolean, editEvent: ()->Unit, openEventDetails: ()->Unit){
        val lastUpdated by calendarViewModel.search.lastUpdated.collectAsState()
        var isSearched by remember { mutableStateOf(true) }
        LaunchedEffect(lastUpdated) {
            isSearched = calendarViewModel.search.isSearched(calendarViewModel, this@PrerenderedEvent)
        }
        Box(
            Modifier
                .fillMaxSize()
                .alpha(if (isSearched) 1f else 0.3f)
        ) {
            for (segment in segments) {
                Box(
                    Modifier
                        .padding(
                            top = segment.getTopPadding(oneHour, startOfDay),
                            start = if (!segment.isLeft) width - bankingSizeDp else 0.dp
                        )
                        .height(segment.getHeightDp(oneHour, startOfDay, endOfDay))
                        .width(if (segment.isFullWidth) width else if (segment.isLeft) width - bankingSizeDp else bankingSizeDp)
                        .clip(RoundedCornerShape(10.dp))
                        .combinedClickable(
                            remember { MutableInteractionSource() },
                            ripple(),
                            isClickEnabled,
                            onClick = openEventDetails,
                            onLongClick = editEvent
                        )
                        .background(event.proposed.getBackgroundBrush(), RoundedCornerShape(10.dp))
                ) {
                    if(segment.hasContent) {
                        with(event.proposed) {
                            RenderContent(oneHour, startOfDay, endOfDay, !segment.isLeft, segment.getBlockHeight(startOfDay, endOfDay))
                        }
                    }
                }
            }
        }
    }
    companion object {
        fun getPrerenderedEvents(events: List<SyncedEvent>, instantEntries: List<InstantEvent.InstantEventGroup>, instantEventDisplaySize: Long): Map<Long, PrerenderedEvent> {
            val order = arrayOf(
                EventType.Timewaste,
                EventType.DigSoc,
                EventType.Social,
                EventType.Sleep,
                EventType.Spont,
                EventType.Hobby,
                EventType.Learn,
                EventType.Travel,
                EventType.Work,
            )
            // Complexity reasons instead of using modifyable.entries / values
            // All will just  be the first iteration and fill spots will use something different from all
            val all = LinkedList<Pair<SyncedEvent, PrerenderedEventBuilder.SegmentBuilder>>()
            for (type in order) {
                for(event in events.filter { it.proposed.type == type }){
                    val overlapping = all.filter { it.first.proposed.overlaps(event.proposed) }
                    for (prerenderedEvent in overlapping) {
                        prerenderedEvent.second.isFullWidth = false
                        prerenderedEvent.second.isLeft = false
                    }
                    val segment = PrerenderedEventBuilder.SegmentBuilder(
                        overlapping.isEmpty(),
                        true,
                        true,
                        event.proposed.start,
                        event.proposed.end
                    )
                    all.add(event to segment)
                }
            }
            for(event in instantEntries) {
                val instantEventAsDDEvent = DefinedDurationEvent(
                    event.start - instantEventDisplaySize / 2,
                    event.end + instantEventDisplaySize / 2
                )
                all.forEach {
                    if(it.first.proposed.overlaps(instantEventAsDDEvent)) {
                        it.second.isFullWidth = false
                        // Does not set left or right align
                    }
                }
            }
            // Until here the segments are only one element
            // Now iterate the list and try to fill spots
            val segmentedMap = mutableMapOf<Long, PrerenderedEventBuilder>()
            val peopleEvents = mutableListOf<PeopleEvent>()
            for (event in all) {
                if (event.second.isLeft) {
                    segmentedMap[event.first.id] = PrerenderedEventBuilder(
                        event.first,
                        listOf(event.second)
                    )
                } else {
                    // Everything that is just a bar at the right
                    val proposed = event.first.proposed
                    if (proposed is PeopleEvent) {
                        peopleEvents.add(proposed)
                    }
                    val ev = event.second
                    val overlappingEvents =
                        all.mapNotNull { if (it.second.overlaps(ev)) it.second else null }
                    // Including self
                    val overlappList = (overlappingEvents.mapNotNull {
                        if (it.start >= ev.start) it to true else null
                    } + overlappingEvents.mapNotNull {
                        if (it.end <= ev.end) it to false else null
                    }).sortedBy { if (it.second) it.first.start else it.first.end }
                    val parted = mutableListOf<Pair<Long, Long>>()
                    var start = ev.start
                    var overlapAmount = overlappingEvents.filter { it.start < ev.start }.size
                    for (ov in overlappList) {
                        if (overlapAmount == 1) {
                            parted.add(start to if (ov.second) ov.first.start else ov.first.end)
                        }
                        overlapAmount += if (ov.second) 1 else -1
                        if (overlapAmount == 1) {
                            start = if (ov.second) ov.first.start else ov.first.end
                        }
                    }
                    val parts = parted.filter { it.second != it.first }
                    if (parts.isNotEmpty()) {
                        val longest =
                            parts.sortedByDescending { it.second - it.first }[0]  // Cant be null
                        val listBuilding = mutableListOf<PrerenderedEventBuilder.SegmentBuilder>()
                        for (part in parts) {
                            if (part.first < event.second.end) listBuilding.add(
                                PrerenderedEventBuilder.SegmentBuilder(
                                    isFullWidth = true,
                                    isLeft = true,
                                    hasContent = part == longest,
                                    segmentStart = part.first,
                                    segmentEnd = part.second
                                )
                            )
                        }
                        ev.hasContent = false
                        listBuilding.add(
                            ev
                        ) // Right no content full time event add last for it to be lowest
                        segmentedMap[event.first.id] = PrerenderedEventBuilder(
                            event.first,
                            listBuilding
                        )
                    } else {
                        segmentedMap[event.first.id] = PrerenderedEventBuilder(
                            event.first,
                            listOf(ev)
                        )
                    }
                }
            }
            segmentedMap.forEach { (_, ev) ->
                if(ev.event.proposed !is PeopleEvent)
                    peopleEvents
                        .filter { (it as? ProposedEvent)?.overlaps(ev.event.proposed) ?: false }
                        .forEach { ev.nextToPeople.addAll(it.people) }
            }
            return segmentedMap.mapValues {
                it.value.asPrerenderedEvent()
            }
        }
    }
    data class Segment(
        val isFullWidth: Boolean,
        val isLeft: Boolean,
        val hasContent: Boolean,
        val segmentStart: Long,
        val segmentEnd: Long
    ): DefinedDurationEvent(segmentStart, segmentEnd)
    private data class PrerenderedEventBuilder(
        var event: SyncedEvent,
        var segments: List<SegmentBuilder>,
    ) {
        val nextToPeople = mutableSetOf<Long>()
        fun asPrerenderedEvent(): PrerenderedEvent = PrerenderedEvent(
            event,
            segments.map(SegmentBuilder::asSegment),
            nextToPeople
        )
        data class SegmentBuilder(
            var isFullWidth: Boolean,
            var isLeft: Boolean,
            var hasContent: Boolean,
            var segmentStart: Long,
            var segmentEnd: Long
        ): DefinedDurationEvent(segmentStart, segmentEnd) {
            fun asSegment(): Segment = Segment(isFullWidth, isLeft, hasContent, segmentStart, segmentEnd)
        }
    }
}