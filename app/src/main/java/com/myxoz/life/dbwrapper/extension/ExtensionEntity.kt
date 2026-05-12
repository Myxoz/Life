package com.myxoz.life.dbwrapper.extension

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "extension", primaryKeys = ["id"])
data class ExtensionEntity(
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "data") val data: String,
)