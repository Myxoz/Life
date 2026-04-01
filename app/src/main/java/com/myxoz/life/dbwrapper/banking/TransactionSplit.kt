package com.myxoz.life.dbwrapper.banking

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("transaction_split")
data class TransactionSplitEntity(
    @PrimaryKey(autoGenerate = false) // we’ll provide our own id. Btw: I love to copy-paste this comment
    val id: Long,

    @ColumnInfo(name = "syncable_id")
    val syncableId: Long?,

    @ColumnInfo(name = "remote_id")
    val remoteId: String?,
)

@Entity("transaction_split_part", primaryKeys = ["id", "person"])
data class TransactionSplitPartEntity(
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "person")
    val person: Long,

    @ColumnInfo(name = "amount")
    val amount: Int,
)