package com.myxoz.life.storage.dbwrapper.events

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "tags", primaryKeys = ["id", "tag"])
data class TagsEntity(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "tag") val tag: Int,
)