package com.myxoz.life.storage.api.syncables

import java.time.Instant

data class BankingSidecarSyncable(
    override val id: Long,
    val transactionId: String,
    val name: String,
    val date: Instant,
) : Syncable