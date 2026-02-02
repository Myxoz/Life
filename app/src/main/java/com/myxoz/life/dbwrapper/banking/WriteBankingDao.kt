package com.myxoz.life.dbwrapper.banking

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface WriteBankingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: BankingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSidecar(sidecarEntity: BankingSidecarEntity)
}