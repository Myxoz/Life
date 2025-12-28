package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlin.math.abs

@Entity(tableName = "banking")
data class BankingEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String, // auto-generated DB id

    @ColumnInfo(name = "card")
    val card: Boolean,

    @ColumnInfo(name = "transfer")
    val transfer: Boolean,

    @ColumnInfo(name = "amount_cents")
    val amountCents: Int,

    @ColumnInfo(name = "currency")
    val currency: String,

    @ColumnInfo(name = "booking_time")
    val bookingTime: String,

    @ColumnInfo(name = "value_date")
    val valueDate: Long,

    @ColumnInfo(name = "from_name")
    val fromName: String,

    @ColumnInfo(name = "from_iban")
    val fromIban: String,

    @ColumnInfo(name = "from_bic")
    val fromBic: String,

    @ColumnInfo(name = "purpose")
    val purpose: String,

    @ColumnInfo(name = "saldo_after_cents")
    val saldoAfterCents: Long,

    @ColumnInfo(name = "purpose_date")
    val purposeDate: Long?,

    @ColumnInfo(name = "last_update")
    val lastUpdate: Long
){
    companion object {
        suspend fun getAllBankingEntriesFor(storage: StorageManager, startOfDay: Long, endOfDay: Long, futureEntries: List<BankingEntity>): List<BankingEntity>{
            val allSidecars = storage.bankingSidecar.getSidecarsBetween(startOfDay, endOfDay)
            val transactionsForSidecars = storage.banking.getTransactionByIds(allSidecars.map { it.transactionId })
            val bankingEntries = storage.banking.getTransactionsOnDay(startOfDay, endOfDay) +
                    allSidecars.mapNotNull { sidecar ->
                        transactionsForSidecars.find { it.id == sidecar.transactionId }?.copy(
                            purposeDate = sidecar.date,
                            fromName = sidecar.name
                        )
                    } +
                    futureEntries.filter { it.purposeDate!=null && it.purposeDate < endOfDay && it.purposeDate > startOfDay }
            return bankingEntries
        }
    }
}

@Dao
interface BankingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: BankingEntity)

    @Query("SELECT * FROM banking WHERE purpose_date IS NOT NULL AND purpose_date >= :start AND purpose_date < :end")
    suspend fun getTransactionsOnDay(start: Long, end: Long): List<BankingEntity>

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

    @Query("SELECT * FROM banking WHERE value_date > :start AND value_date < :end AND amount_cents = :amount")
    suspend fun findPossibleMobileTransaction(start: Long, end: Long, amount: Int): List<BankingEntity>

    @Query("SELECT * FROM banking WHERE value_date = (SELECT MAX(value_date) FROM banking)")
    suspend fun getLastTransactionDay(): List<BankingEntity>
}
// Methods would be put somewhere else, but banking doesnt have a special event class so:
fun Int.formatCents(euroSign: Boolean=true) = "${if(this<0) "-" else ""}${abs(this)/100},${(abs(this)%100).toString().padStart(2, '0')}${if(euroSign) "€" else ""}"