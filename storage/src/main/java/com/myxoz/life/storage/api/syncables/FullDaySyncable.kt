package com.myxoz.life.storage.api.syncables

import com.myxoz.life.storage.api.syncables.additionals.DayScreenTime

data class FullDaySyncable internal constructor(
    val happyness: Int,
    val stress: Int,
    val successfulness: Int,
    val steps: Int,
    val screenTimeMs: Int,
    val topApps: List<DayScreenTime>,
    override val id: Long
) : Syncable