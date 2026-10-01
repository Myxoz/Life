package com.myxoz.life.storage.api.syncables

data class BankingSidecarSyncable(
    override val id: Long,
    val transactionId: String,
    val name: String,
    val date: Long,
) : Syncable