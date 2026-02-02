package com.myxoz.life.dbwrapper.events

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "digsocmapping", primaryKeys = ["event_id", "app"])
data class DigSocMappingEntity(
    @ColumnInfo(name = "event_id") val eventId: Long,

    @ColumnInfo(name = "app") val app: Int,

    @ColumnInfo(name = "duration_ms") val durationMs: Long,
)