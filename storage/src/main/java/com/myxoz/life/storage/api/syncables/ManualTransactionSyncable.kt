package com.myxoz.life.storage.api.syncables

data class ManualTransactionSyncable(
    override val id: Long,
    val digital: Boolean,
    val cashless: Boolean,
    val amountCents: Int,
    val name: String,
    val timestamp: Long,
    val purpose: String?
): Syncable