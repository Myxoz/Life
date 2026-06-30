package com.myxoz.life.storage.dbwrapper.events

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "people_mapping", primaryKeys = ["event_id", "person_id"])
data class PeopleMappingEntity(
    @ColumnInfo(name = "event_id") val eventId: Long,

    @ColumnInfo(name = "person_id") val personId: Long,
)