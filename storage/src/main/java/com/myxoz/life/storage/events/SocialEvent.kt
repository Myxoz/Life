package com.myxoz.life.storage.events

import com.myxoz.life.storage.events.additionals.EventTag
import com.myxoz.life.storage.events.additionals.EventTime

data class SocialEvent(
    override val eventTime: EventTime,
    override val eventTags: Set<EventTag>,
    override val title: String,
    override val people: Set<Long>,
    val more: Boolean
): RawEvent, TagEvent, TitleEvent, PeopleEvent {
    override fun copyWithTimeline(newEventTime: EventTime) = copy(eventTime = newEventTime)
    internal companion object {
        const val CALENDARID = 6
    }
}