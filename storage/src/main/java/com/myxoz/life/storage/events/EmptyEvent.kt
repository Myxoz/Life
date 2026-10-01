package com.myxoz.life.storage.events

import com.myxoz.life.storage.events.additionals.EventTime

data class EmptyEvent(override val eventTime: EventTime): RawEvent {
    override fun copyWithTimeline(newEventTime: EventTime) = copy(eventTime = newEventTime)
    internal companion object {
        const val CALENDARID = 1
    }
}