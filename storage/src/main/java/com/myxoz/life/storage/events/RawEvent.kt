package com.myxoz.life.storage.events

import com.myxoz.life.storage.events.additionals.EventTime

sealed interface RawEvent{
    val eventTime: EventTime
    fun copyWithTimeline(newEventTime: EventTime): RawEvent
}