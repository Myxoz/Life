package com.myxoz.life.dbwrapper.days

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "days")
data class DaysEntity(
    @PrimaryKey(autoGenerate = false) // we’ll provide our own id
    @ColumnInfo(name = "date") val id: Int,

    @ColumnInfo(name = "screen_time_ms") val screenTimeMs: Int,

    @ColumnInfo(name = "steps") val steps: Int,

    @ColumnInfo(name = "happyness") val happyness: Int,

    @ColumnInfo(name = "stress") val stress: Int,

    @ColumnInfo(name = "successfulness") val successfulness: Int,
)