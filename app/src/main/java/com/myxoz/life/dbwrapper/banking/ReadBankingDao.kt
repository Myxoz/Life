package com.myxoz.life.dbwrapper.banking

import androidx.room.ColumnInfo
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

    @Query("""
        SELECT MIN(first_date) FROM (
            SELECT MIN(date) AS first_date FROM bankingsidecar
            UNION ALL
            SELECT MIN(purpose_date) AS first_date FROM banking WHERE purpose_date IS NOT NULL
            UNION ALL
            SELECT MIN(value_date) AS first_date FROM banking
        )
    """)
    suspend fun getEarliestTransactionDate(): Long?

    @Query("SELECT * FROM banking WHERE purpose_date IS NOT NULL AND purpose_date >= :start AND purpose_date < :end")
    suspend fun getTransactionsOnDay(start: Long, end: Long): List<BankingEntity>

    @Query("SELECT * FROM bankingsidecar WHERE id = :id LIMIT 1")
    suspend fun getSidecar(id: Long): BankingSidecarEntity?

    @Query("SELECT * FROM bankingsidecar WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getSidecar(transactionId: String): BankingSidecarEntity?

    @Query("SELECT * FROM bankingsidecar WHERE transactionId IN (:transactionId)")
    suspend fun getAllSidecars(transactionId: List<String>): List<BankingSidecarEntity>

    @Query("SELECT * FROM banking WHERE purpose_date IS NOT NULL")
    suspend fun getAllTransactionsWithPurposeDate(): List<BankingEntity>

    @Query("SELECT * FROM bankingsidecar WHERE date >= :start AND date < :end")
    suspend fun getSidecarsBetween(start: Long, end: Long): List<BankingSidecarEntity>


    data class BankingTrainingRow(
        @ColumnInfo(name = "amount_cents") val amountCents: Int,
        @ColumnInfo(name = "purpose_date") val purposeDate: Long,
        @ColumnInfo(name = "from_name") val fromName: String,
        @ColumnInfo(name = "travel_location_id") val travelLocationId: Long?
    )

    @Query("""
SELECT 
    b.amount_cents,
    b.purpose_date,
    b.from_name,
    (
        SELECT t.`to`
        FROM events e
        INNER JOIN travel t ON t.id = e.id
        WHERE 
            e.type = 7
            AND e.start <= b.purpose_date
        ORDER BY e.ends DESC
        LIMIT 1
    ) AS travel_location_id
FROM banking b
WHERE b.purpose_date IS NOT NULL
""")
    suspend fun getBankingTrainingRows(): List<BankingTrainingRow>

    @Query("""
    SELECT t.`to`
    FROM events e
    INNER JOIN travel t ON t.id = e.id
    WHERE 
        e.type = 7
        AND e.start <= :ts
    ORDER BY e.ends DESC
    LIMIT 1
""")
    suspend fun getLastTravelEventEndBefore(ts: Long): Long?
}