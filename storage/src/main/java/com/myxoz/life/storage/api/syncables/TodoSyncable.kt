package com.myxoz.life.storage.api.syncables

import java.time.Instant

data class TodoSyncable(
    override val id: Long,
    val short: String,
    val details: String?,
    val done: Boolean,
    val timestamp: Instant,
): Syncable