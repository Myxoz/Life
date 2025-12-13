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
)

@Dao
interface BankingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: BankingEntity)

    @Query("SELECT * FROM banking WHERE purpose_date IS NOT NULL AND purpose_date >= :start AND purpose_date < :end")
    suspend fun getTransactionsOnDay(start: Long, end: Long): List<BankingEntity>

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
fun Int.centsToDisplay(euroSign: Boolean=true) = "${if(this<0) "-" else ""}${abs(this)/100},${(abs(this)%100).toString().padStart(2, '0')}${if(euroSign) "€" else ""}"