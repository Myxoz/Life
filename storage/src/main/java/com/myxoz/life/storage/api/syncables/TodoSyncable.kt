package com.myxoz.life.storage.api.syncables

data class TodoSyncable(
    override val id: Long,
    val short: String,
    val details: String?,
    val done: Boolean,
    val timestamp: Long,
): Syncable