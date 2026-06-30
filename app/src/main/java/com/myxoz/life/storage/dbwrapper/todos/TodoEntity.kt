package com.myxoz.life.storage.dbwrapper.todos

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "todos", primaryKeys = ["id"])
data class TodoEntity(
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "short") val short: String,

    @ColumnInfo(name = "details") val details: String?,

    @ColumnInfo(name = "done") val done: Boolean,

    @ColumnInfo(name = "timestamp") val timestamp: Long,
)