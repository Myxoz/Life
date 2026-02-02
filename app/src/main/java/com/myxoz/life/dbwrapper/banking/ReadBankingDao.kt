package com.myxoz.life.dbwrapper.banking

import androidx.room.Dao
import androidx.room.Query

@Dao
interface ReadBankingDao{
    @Query("""
    SELECT * FROM banking 
    WHERE 
    (
        -- Logic for 'filteredTransactions': 
        -- 1. Matches the date criteria (Purpose Date vs Value Date logic)
        (
            (purpose_date IS NOT NULL AND purpose_date >= :start AND purpose_date < :end) 
            OR 
            (purpose_date IS NULL AND value_date >= :start AND value_date < :end)
        )
        -- 2. Excludes transactions that have ANY sidecar
        AND id NOT IN (SELECT transactionId FROM bankingsidecar)
    )
    OR 
    (
        -- Logic for 'transactionsForSidecars':
        -- Include transactions linked to sidecars that exist within the date range
        id IN (SELECT transactionId FROM bankingsidecar WHERE date >= :start AND date < :end)
    )
""")
    suspend fun getCombinedTransactions(start: Long, end: Long): List<BankingEntity>

    @Query("SELECT * FROM banking WHERE value_date >= :start AND value_date < :end")
    suspend fun getFullDayTransactions(start: Long, end: Long): List<BankingEntity>

    @Query("SELECT * FROM banking WHERE id = :id")
    suspend fun getTransactionById(id: String): BankingEntity?

    @Query("SELECT * FROM banking WHERE id IN (:ids)")
    suspend fun getTransactionByIds(ids: List<String>): List<BankingEntity>

    @Query("SELECT * FROM banking WHERE purpose_date IS NOT NULL AND purpose_date > :start AND purpose_date < :end AND amount_cents = :amount")
    suspend fun findPossibleMobileTransaction(start: Long, end: Long, amount: Int): List<BankingEntity>

    @Query("SELECT * FROM banking WHERE value_date = (SELECT MAX(value_date) FROM banking)")
    suspend fun getLastTransactionDay(): List<BankingEntity>

    @Query("SELECT * FROM banking WHERE purpose_date IS NOT NULL AND purpose_date >= :start AND purpose_date < :end")
    suspend fun getTransactionsOnDay(start: Long, end: Long): List<BankingEntity>

    @Query("SELECT * FROM bankingsidecar WHERE id = :id LIMIT 1")
    suspend fun getSidecar(id: Long): BankingSidecarEntity?

    @Query("SELECT * FROM bankingsidecar WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getSidecar(transactionId: String): BankingSidecarEntity?

    @Query("SELECT * FROM bankingsidecar WHERE transactionId IN (:transactionId)")
    suspend fun getAllSidecars(transactionId: List<String>): List<BankingSidecarEntity>

    @Query("SELECT * FROM bankingsidecar WHERE date >= :start AND date < :end")
    suspend fun getSidecarsBetween(start: Long, end: Long): List<BankingSidecarEntity>
}