package com.myxoz.life.dbwrapper.banking

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manual_transactions")
data class ManualTransactionEntity(
    @PrimaryKey(autoGenerate = false) // we’ll provide our own id
    val id: Long,

    @ColumnInfo(name = "digital")
    val digital: Boolean,

    @ColumnInfo(name = "cashless")
    val cashless: Boolean,

    @ColumnInfo(name = "amount_cents")
    val amountCents: Int,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "purpose")
    val purpose: String?,
)