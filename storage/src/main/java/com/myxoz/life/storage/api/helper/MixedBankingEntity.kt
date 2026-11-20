package com.myxoz.life.storage.api.helper

import android.util.Log
import com.myxoz.life.core.toInstant
import com.myxoz.life.storage.api.syncables.BankingSyncable
import com.myxoz.life.storage.api.syncables.ManualTransactionSyncable
import com.myxoz.life.storage.convertion.SyncableFromEntity.fromEntity
import com.myxoz.life.storage.dbwrapper.banking.BankingSidecarEntity
import com.myxoz.life.storage.dbwrapper.banking.ReadBankingDao

class MixedBankingEntity private constructor(
    private val entity: BankingSyncable?,
    private val sidecar: BankingSidecarEntity?,
    private val manual: ManualTransactionSyncable?,
) {
    val amount = manual?.amountCents ?: entity?.amountCents
    ?: throw Error("Trying to construct banking display entity without entity and manual transaction entity: null sidecar: $sidecar manualTransaction: null")
    val digital = manual?.digital ?: true
    val cashless = manual?.cashless ?: !entity!!.card
    val isTransaction = digital && cashless
    val isCashlessPayment = !digital && cashless
    val currency = entity?.currency ?: "EUR"
    val iban = entity?.fromIban ?: ""
    val bookingTime = entity?.bookingTime
    val purpose = manual?.purpose ?: entity?.purpose
    val categorization = when {
        digital && cashless -> "Überweisung"
        digital && !cashless -> "Kartenzahlung"
        !digital && cashless -> "Bargeldlos"
        else -> "Bar"
    }
    val key = manual?.id to entity?.id
    val displayTimestamp = manual?.timestamp ?: sidecar?.date?.toInstant() ?: entity?.purposeDate
    val timestamp = displayTimestamp ?: entity?.valueDate ?: throw Error("This cannot happen")
    fun displayName(predicted: String?): String = when{
        predicted != null -> predicted
        !entity?.fromName.isNullOrEmpty() -> "${entity.fromName}${if (sidecar != null) " (${sidecar.name})" else ""}"
        !digital && !cashless -> manual?.name ?: ""
        else -> "Unbekannt"
    }
    val processDate = entity?.valueDate
    val title = manual?.name ?: sidecar?.name ?: entity?.fromName ?: error("This can't happend")
    val subTitle = if(manual != null) manual.purpose ?: ""
    else if(sidecar == null && entity != null) entity.fromIban.chunked(4).joinToString(" ") else entity?.fromName ?: ""
    fun getStoredManualTransactionSyncable(): ManualTransactionSyncable? = manual
    override fun equals(other: Any?): Boolean {
        return other is MixedBankingEntity && ((entity != null && other.entity?.id == entity.id) || (manual != null && other.manual?.id == manual.id))
    }

    companion object {
        /* I dare you to find better asignments. Yes they arent perfect.
            Categorize them into 4 boolean categories. Go girl! You can mail them to:
                mailto:ifoundbetterassigments@myxoz.de
                            cashless    digital
            manualPayment:      -          -
            card payment:       -          X
            cashless:           X          -
            transaction:        X          X
         */
        /** Be careful with the nulls. If entity is not null sidecar is optional, else provide manualTransaction */
        internal suspend fun from(id: String, readBankingDao: ReadBankingDao): MixedBankingEntity? {
            return MixedBankingEntity(
                readBankingDao.getTransactionById(id)?.fromEntity() ?: return null,
                readBankingDao.getSidecar(id),
                null
            )
        }
        internal fun from(entity: BankingSyncable, sidecar: BankingSidecarEntity?): MixedBankingEntity {
            return MixedBankingEntity(entity, sidecar, null)
        }
        internal suspend fun from(entity: BankingSyncable, readBankingDao: ReadBankingDao): MixedBankingEntity =
            MixedBankingEntity(entity, readBankingDao.getSidecar(entity.id), null)

        internal suspend fun from(id: Long, readBankingDao: ReadBankingDao): MixedBankingEntity? {
            return MixedBankingEntity(null, null, readBankingDao.getManualTransaction(id)?.fromEntity() ?: return null)
        }
        internal fun from(manualTransactionSyncable: ManualTransactionSyncable): MixedBankingEntity {
            return MixedBankingEntity(null, null, manualTransactionSyncable)
        }
        fun finalDailyBalance(transactions: List<MixedBankingEntity>?): Long? {
            if (transactions.isNullOrEmpty()) {
                Log.w("Transaction", "No transactions for the last day: $transactions")
                return null
            }
            val entites = transactions.mapNotNull { it.entity }

            // Try to find the "last" transaction recursively
            fun findLast(current: BankingSyncable, remaining: List<BankingSyncable>): BankingSyncable {
                // Look for a transaction that could come AFTER the current one
                val next = remaining.find { it.saldoAfterCents - it.amountCents == current.saldoAfterCents }
                return if (next == null) current else findLast(next, remaining - next)
            }

            // Try every transaction as a possible starting point
            for (candidate in entites) {
                val previous =
                    entites.find { candidate.saldoAfterCents - candidate.amountCents == it.saldoAfterCents }
                if (previous == null) {
                    return findLast(candidate, entites - candidate).saldoAfterCents
                }
            }

            Log.w("Transaction", "Could not determine transaction order $entites")
            return null
        }
    }

    override fun hashCode(): Int {
        var result = amount
        result = 31 * result + (entity?.hashCode() ?: 0)
        result = 31 * result + (sidecar?.hashCode() ?: 0)
        result = 31 * result + (manual?.hashCode() ?: 0)
        return result
    }
}