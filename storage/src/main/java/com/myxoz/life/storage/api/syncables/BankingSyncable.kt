package com.myxoz.life.storage.api.syncables

class BankingSyncable(
    val id: String,
    val card: Boolean,
    val transfer: Boolean,
    val amountCents: Int,
    val currency: String,
    val bookingTime: String,
    val valueDate: Long,
    val fromName: String,
    val fromIban: String,
    val fromBic: String,
    val purpose: String,
    val saldoAfterCents: Long,
    val purposeDate: Long?,
    val lastUpdate: Long
) : ServerSyncable