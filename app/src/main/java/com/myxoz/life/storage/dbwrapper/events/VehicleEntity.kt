package com.myxoz.life.storage.dbwrapper.events

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "vehicle", primaryKeys = ["id", "type"])
data class VehicleEntity(
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "type") val type: Int,

    @ColumnInfo(name = "durationMs") val durationMs: Int,
)