package com.myxoz.life.storage.events

import com.myxoz.life.storage.events.additionals.EventTag
import com.myxoz.life.storage.events.additionals.EventTime
import com.myxoz.life.storage.events.additionals.PeopleEvent
import com.myxoz.life.storage.events.additionals.TagEvent
import com.myxoz.life.storage.events.additionals.TitleEvent

data class SocialEvent(
    override val eventTime: EventTime,
    override val eventTags: List<EventTag>,
    override val title: String,
    override val people: List<Long>,
    val more: Boolean
): RawEvent, TagEvent, TitleEvent, PeopleEvent {
    override fun copyWithTimeline(newEventTime: EventTime) = copy(eventTime = newEventTime)
    internal companion object {
        const val CALENDARID = 6
    }
}