package com.myxoz.life.storage.api.syncables

import java.time.Instant

data class BankingSyncable(
    val id: String,
    val card: Boolean,
    val transfer: Boolean,
    val amountCents: Int,
    val currency: String,
    val bookingTime: String,
    val valueDate: Instant,
    val fromName: String,
    val fromIban: String,
    val fromBic: String,
    val purpose: String,
    val saldoAfterCents: Long,
    val purposeDate: Instant?,
    val lastUpdate: Instant
) : ServerSyncable