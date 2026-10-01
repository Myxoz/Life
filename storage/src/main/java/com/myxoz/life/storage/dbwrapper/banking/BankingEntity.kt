package com.myxoz.life.storage.dbwrapper.banking

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "banking")
internal data class BankingEntity(
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
        suspend fun getAllBankingEntriesFor(bankingDao: ReadBankingDao, startOfDay: Long, endOfDay: Long, futureEntries: List<BankingEntity>): List<BankingEntity>{
            val allSidecars = bankingDao.getSidecarsBetween(startOfDay, endOfDay)
            val transactionsForSidecars = bankingDao.getTransactionByIds(allSidecars.map { it.transactionId })
            val bankingEntries = bankingDao.getTransactionsOnDay(startOfDay, endOfDay) +
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