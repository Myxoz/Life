package com.myxoz.life.storage.events

import com.myxoz.life.storage.events.additionals.EventTime
import com.myxoz.life.storage.events.additionals.TimedTagLikeContainer
import com.myxoz.life.storage.events.additionals.TimewastePlatform
import com.myxoz.life.storage.events.additionals.TitleEvent

data class TimewasteEvent(
    override val eventTime: EventTime,
    val timewastePlatforms: List<TimedTagLikeContainer<TimewastePlatform>>,
    override val title: String,
): RawEvent, TitleEvent {
    override fun copyWithTimeline(newEventTime: EventTime) = copy(eventTime = newEventTime)
    internal companion object {
        const val CALENDARID = 10
    }
}