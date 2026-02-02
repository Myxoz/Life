package com.myxoz.life.dbwrapper.banking

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "bankingsidecar", primaryKeys = ["id"])
data class BankingSidecarEntity(
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "transactionId") val transactionId: String,

    @ColumnInfo(name = "name") val name: String,

    @ColumnInfo(name = "date") val date: Long,
)