package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(tableName = "bankingsidecar", primaryKeys = ["id"])
data class BankingSidecarEntity(
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "transactionId") val transactionId: String,

    @ColumnInfo(name = "name") val name: String,

    @ColumnInfo(name = "date") val date: Long,
)

@Dao
interface BankingSidecarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSidecar(sidecarEntity: BankingSidecarEntity)

    @Query("SELECT * FROM bankingsidecar WHERE id = :id LIMIT 1")
    suspend fun getSidecar(id: Long): BankingSidecarEntity?

    @Query("SELECT * FROM bankingsidecar WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getSidecar(transactionId: String): BankingSidecarEntity?

    @Query("SELECT * FROM bankingsidecar WHERE date >= :start AND date < :end")
    suspend fun getSidecarsBetween(start: Long, end: Long): List<BankingSidecarEntity>
}
