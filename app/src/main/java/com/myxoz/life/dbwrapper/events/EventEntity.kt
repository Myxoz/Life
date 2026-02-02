package com.myxoz.life.dbwrapper.events

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = false) // we’ll provide our own id
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "type") val type: Int,

    @ColumnInfo(name = "start") val start: Long,

    @ColumnInfo(name = "ends") val end: Long,

    @ColumnInfo(name = "created") val created: Long,

    @ColumnInfo(name = "edited") val edited: Long?,

    @ColumnInfo(name = "uss") val uss: Boolean,

    @ColumnInfo(name = "usl") val usl: Boolean
)