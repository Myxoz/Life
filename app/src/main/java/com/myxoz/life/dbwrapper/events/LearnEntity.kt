package com.myxoz.life.dbwrapper.events

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learn")
data class LearnEntity(
    @PrimaryKey(false)
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "title") val title: String,

    @ColumnInfo(name = "details") val details: String?,
)