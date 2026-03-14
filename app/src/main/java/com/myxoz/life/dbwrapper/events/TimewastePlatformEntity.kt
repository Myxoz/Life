package com.myxoz.life.dbwrapper.events;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "timewasteplatform", primaryKeys = ["event_id", "timewasteplatform"])
data class TimewastePlatformEntity(
        @ColumnInfo(name = "event_id") val eventId: Long,

        @ColumnInfo(name = "timewasteplatform") val timewastePlatform: Int,

        @ColumnInfo(name = "duration_ms") val durationMs: Long,
)
