package com.myxoz.life.storage.events

import com.myxoz.life.storage.events.additionals.DigSocPlatform
import com.myxoz.life.storage.events.additionals.EventTime
import com.myxoz.life.storage.events.additionals.TimedTagLikeContainer

data class DigSocEvent(
    override val eventTime: EventTime,
    val digSocEntries: List<TimedTagLikeContainer<DigSocPlatform>>,
    override val title: String,
    override val people: Set<Long>
): RawEvent, TitleEvent, PeopleEvent {
    override fun copyWithTimeline(newEventTime: EventTime) = copy(eventTime = newEventTime)

    internal companion object {
        const val CALENDARID = 8
    }
}