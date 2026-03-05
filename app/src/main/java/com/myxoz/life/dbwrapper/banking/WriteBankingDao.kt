package com.myxoz.life.dbwrapper.banking

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WriteBankingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: BankingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSidecar(sidecarEntity: BankingSidecarEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManualTransaction(manualTransaction: ManualTransactionEntity)
    @Query("DELETE FROM manual_transactions WHERE id = :id")
    suspend fun deleteManualTransactionSyncable(id: Long)
}