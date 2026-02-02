package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "dayscreentime", primaryKeys = ["date", "packagename"])
data class DayScreenTimeEntity(
    @ColumnInfo(name = "date") val date: Long,

    @ColumnInfo(name = "packagename") val packagename: String,

    @ColumnInfo(name = "duration") val duration: Long,
)