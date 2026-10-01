package com.myxoz.life.storage.events

import com.myxoz.life.storage.events.additionals.EventTime
import com.myxoz.life.storage.events.additionals.TimedTagLikeContainer
import com.myxoz.life.storage.events.additionals.Vehicle

data class TravelEvent(
    override val eventTime: EventTime,
    val from: Long,
    val to: Long,
    val vehicles: List<TimedTagLikeContainer<Vehicle>>
): RawEvent {
    override fun copyWithTimeline(newEventTime: EventTime) = copy(eventTime = newEventTime)
    internal companion object {
        const val CALENDARID = 7
    }
}