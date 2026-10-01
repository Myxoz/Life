package com.myxoz.life.storage.api.syncables

sealed interface Syncable: ServerSyncable {
    val id: Long
}