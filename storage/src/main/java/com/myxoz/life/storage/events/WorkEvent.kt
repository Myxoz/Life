package com.myxoz.life.storage.events

import com.myxoz.life.storage.events.additionals.DetailsEvent
import com.myxoz.life.storage.events.additionals.EventTag
import com.myxoz.life.storage.events.additionals.EventTime
import com.myxoz.life.storage.events.additionals.TagEvent
import com.myxoz.life.storage.events.additionals.TitleEvent

data class WorkEvent(
    override val eventTime: EventTime,
    override val eventTags: List<EventTag>,
    override val title: String,
    override val details: String?
): RawEvent, TagEvent, TitleEvent, DetailsEvent {
    override fun copyWithTimeline(newEventTime: EventTime) = copy(eventTime = newEventTime)
    internal companion object {
        const val CALENDARID = 9
    }
}