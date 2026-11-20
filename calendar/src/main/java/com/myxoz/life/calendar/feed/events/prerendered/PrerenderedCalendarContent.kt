package com.myxoz.life.calendar.feed.events.prerendered

import com.myxoz.life.calendar.feed.events.prerendered.PrerenderedEvent.Segment
import com.myxoz.life.calendar.feed.instantevents.InstantEventGroup
import com.myxoz.life.core.minus
import com.myxoz.life.core.plus
import com.myxoz.life.storage.api.syncables.SyncedEvent
import com.myxoz.life.storage.events.DigSocEvent
import com.myxoz.life.storage.events.EmptyEvent
import com.myxoz.life.storage.events.HobbyEvent
import com.myxoz.life.storage.events.LearnEvent
import com.myxoz.life.storage.events.PeopleEvent
import com.myxoz.life.storage.events.RawEvent
import com.myxoz.life.storage.events.SleepEvent
import com.myxoz.life.storage.events.SocialEvent
import com.myxoz.life.storage.events.SpontEvent
import com.myxoz.life.storage.events.TimewasteEvent
import com.myxoz.life.storage.events.TravelEvent
import com.myxoz.life.storage.events.WorkEvent
import com.myxoz.life.storage.events.additionals.EventTime
import java.time.Instant
import java.util.LinkedList
import kotlin.time.Duration

internal data class PrerenderedCalendarContent(
    val events: List<PrerenderedEvent>,
    val instantEvents: List<InstantEventGroup>
) {
    companion object {
        private fun priority(event: SyncedEvent) = when(event.raw) {
            is DigSocEvent -> 1
            is SocialEvent -> 2
            is TimewasteEvent -> 3
            is SleepEvent -> 4
            is SpontEvent -> 5
            is HobbyEvent -> 6
            is LearnEvent -> 7
            is TravelEvent -> 8
            is WorkEvent -> 9
            is EmptyEvent -> Long.MAX_VALUE
        }
        fun getPrerenderedCalendarContent(
            events: List<SyncedEvent>,
            instantEntries: List<InstantEventGroup>,
            instantEventDisplaySize: Duration
        ): PrerenderedCalendarContent {
            // Complexity reasons instead of using modifyable.entries / values
            // All will just be the first iteration and fill spots will use something different from all
            val all = LinkedList<Pair<SyncedEvent, PrerenderedEventBuilder.SegmentBuilder>>()
            val sorted = events.sortedBy(this::priority)
            for(event in sorted) {
                val overlapping = all.filter { it.first.raw.eventTime.overlaps(event.raw.eventTime) }
                for (prerenderedEvent in overlapping) {
                    prerenderedEvent.second.isFullWidth = false
                    prerenderedEvent.second.isLeft = false
                }
                val segment = PrerenderedEventBuilder.SegmentBuilder(
                    overlapping.isEmpty(),
                    true,
                    true,
                    event.raw.eventTime.start,
                    event.raw.eventTime.end
                )
                all.add(event to segment)
            }
            for(event in instantEntries) {
                val instantEventsAsEventTime = EventTime(
                    event.start - instantEventDisplaySize / 2,
                    event.end + instantEventDisplaySize / 2,
                    unspecifiedStart = false,
                    unspecifiedLength = false
                )
                all.forEach {
                    if(it.first.raw.eventTime.overlaps(instantEventsAsEventTime)) {
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
                    val proposed = event.first.raw
                    if (proposed is PeopleEvent) {
                        peopleEvents.add(proposed)
                    }
                    val ev = event.second
                    val overlappingEvents =
                        all.mapNotNull { if (ev.overlaps(it.second)) it.second else null }
                    // Including self
                    val overlappList = (overlappingEvents.mapNotNull {
                        if (it.segmentStart >= ev.segmentStart) it to true else null
                    } + overlappingEvents.mapNotNull {
                        if (it.segmentEnd <= ev.segmentEnd) it to false else null
                    }).sortedBy { if (it.second) it.first.segmentStart else it.first.segmentEnd }
                    val parted = mutableListOf<Pair<Instant, Instant>>()
                    var start = ev.segmentStart
                    var overlapAmount = overlappingEvents.filter { it.segmentStart < ev.segmentStart }.size
                    for (ov in overlappList) {
                        if (overlapAmount == 1) {
                            parted.add(start to if (ov.second) ov.first.segmentStart else ov.first.segmentEnd)
                        }
                        overlapAmount += if (ov.second) 1 else -1
                        if (overlapAmount == 1) {
                            start = if (ov.second) ov.first.segmentStart else ov.first.segmentEnd
                        }
                    }
                    val parts = parted.filter { it.second != it.first }
                    if (parts.isNotEmpty()) {
                        val longest =
                            parts.sortedByDescending { it.second - it.first }[0]  // Cant be null
                        val listBuilding = mutableListOf<PrerenderedEventBuilder.SegmentBuilder>()
                        for (part in parts) {
                            if (part.first < event.second.segmentEnd) listBuilding.add(
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
                        listBuilding.add(0,
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
                if(ev.event.raw !is PeopleEvent)
                    peopleEvents
                        .filter { (it as? RawEvent)?.eventTime?.overlaps(ev.event.raw.eventTime) ?: false }
                        .forEach { ev.nextToPeople.addAll(it.people) }
            }
            val segmentedEvents = segmentedMap.map { it.value.asPrerenderedEvent() }
            return PrerenderedCalendarContent(segmentedEvents, instantEntries)
        }
    }

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
            var segmentStart: Instant,
            var segmentEnd: Instant
        ) {
            fun overlaps(raw: RawEvent) =
                raw.eventTime.overlaps(segmentStart, segmentEnd)

            fun overlaps(other: SegmentBuilder) =
                other.segmentStart < segmentEnd && other.segmentEnd > segmentStart

            fun asSegment(): Segment = Segment(isFullWidth, isLeft, hasContent, segmentStart, segmentEnd)
        }
    }
}