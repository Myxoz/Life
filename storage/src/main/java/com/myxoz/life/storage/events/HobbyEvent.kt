package com.myxoz.life.storage.events

import com.myxoz.life.storage.events.additionals.EventTag
import com.myxoz.life.storage.events.additionals.EventTime

data class HobbyEvent(
    override val eventTime: EventTime,
    override val eventTags: Set<EventTag>,
    override val title: String,
    override val details: String?
): RawEvent, TagEvent, TitleEvent, DetailsEvent {
    override fun copyWithTimeline(newEventTime: EventTime) = copy(eventTime = newEventTime)
    internal companion object {
        const val CALENDARID = 4
    }
}