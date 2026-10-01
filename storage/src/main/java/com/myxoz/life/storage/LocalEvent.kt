package com.myxoz.life.storage

import com.myxoz.life.storage.events.RawEvent

data class LocalEvent(
    val localId: Long,
    val raw: RawEvent
)