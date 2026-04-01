package com.myxoz.life.dbwrapper.banking

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplitAndParts(splitEntity: TransactionSplitEntity, splitParts: List<TransactionSplitPartEntity>)

    @Query("DELETE FROM transaction_split WHERE id = :id")
    suspend fun deleteTransactionSplit(id: Long)

    @Query("DELETE FROM transaction_split_part WHERE id = :id")
    suspend fun deleteTransactionSplitParts(id: Long)

    @Transaction
    suspend fun deleteSplitAndParts(id: Long) {
        deleteTransactionSplitParts(id)
        deleteTransactionSplit(id)
    }
}