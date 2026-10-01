package com.myxoz.life.storage.api.syncables

import com.myxoz.life.storage.events.RawEvent
import java.time.Instant

@ConsistentCopyVisibility
data class SyncedEvent internal constructor(
    override val id: Long,
    val created: Instant,
    val edited: Instant?,
    val raw: RawEvent,
) : Syncable