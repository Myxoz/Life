package com.myxoz.life.storage.events

import com.myxoz.life.storage.events.additionals.EventTag

sealed interface TagEvent {
    val eventTags: Set<EventTag>
}
