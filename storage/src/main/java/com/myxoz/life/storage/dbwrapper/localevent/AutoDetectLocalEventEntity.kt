package com.myxoz.life.storage.dbwrapper.localevent

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("autodetectlocalevent")
class AutoDetectLocalEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "type") val type: Int,

    @ColumnInfo(name = "start") val start: Long,

    @ColumnInfo(name = "ends") val end: Long,

    @ColumnInfo(name = "content") val content: String,
)