package com.myxoz.life.dbwrapper.events

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "digsoc")
data class DigSocEntity(
    @PrimaryKey(false)
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "title") val title: String,
)