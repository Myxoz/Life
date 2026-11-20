package com.myxoz.life.storage.api.syncables

data class LocationSyncable(
    val name: String,
    val longitude: Double,
    val lat: Double,
    val radiusM: Int,
    val ssid: String?,
    val street: String?,
    val number: String?,
    val city: String?,
    val country: String,
    override val id: Long
) : Syncable