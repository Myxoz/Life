package com.myxoz.life.storage.api.syncables

import java.time.Instant

data class ManualTransactionSyncable(
    override val id: Long,
    val digital: Boolean,
    val cashless: Boolean,
    val amountCents: Int,
    val name: String,
    val timestamp: Instant,
    val purpose: String?
): Syncable