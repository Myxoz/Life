package com.myxoz.life.storage.dbwrapper.state

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "keyvalue", primaryKeys = ["key"])
internal data class KeyValueEntity(
    @ColumnInfo(name = "key") val key: String,

    @ColumnInfo(name = "value") val value: String,
)