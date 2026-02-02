package com.myxoz.life.dbwrapper.events

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "social")
data class SocialEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "title") val title: String,

    @ColumnInfo(name = "more") val more: Boolean,
)