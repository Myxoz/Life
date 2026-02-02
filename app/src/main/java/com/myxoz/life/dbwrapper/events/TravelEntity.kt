package com.myxoz.life.dbwrapper.events

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "travel")
data class TravelEntity(
    @PrimaryKey(false)

    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "from") val from: Long,

    @ColumnInfo(name = "to") val to: Long,
)
