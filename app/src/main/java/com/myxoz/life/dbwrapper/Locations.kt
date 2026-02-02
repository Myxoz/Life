package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "location", primaryKeys = ["id"])
data class LocationEntity(
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "name") val name: String,

    @ColumnInfo(name = "longitude") val longitude: Double,

    @ColumnInfo(name = "lat") val lat: Double,

    @ColumnInfo(name = "radiusM") val radiusM: Int,

    @ColumnInfo(name = "ssid") val ssid: String?,

    @ColumnInfo(name = "street") val street: String?,

    @ColumnInfo(name = "number") val number: String?,

    @ColumnInfo(name = "city") val city: String?,

    @ColumnInfo(name = "country") val country: String,
)