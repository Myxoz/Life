package com.myxoz.life.repositories

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import com.myxoz.life.R
import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.DeleteEntry
import com.myxoz.life.api.syncables.ManualTransactionSyncable
import com.myxoz.life.dbwrapper.WaitingSyncDao
import com.myxoz.life.dbwrapper.banking.BankingEntity
import com.myxoz.life.dbwrapper.banking.BankingSidecarEntity
import com.myxoz.life.dbwrapper.banking.ReadBankingDao
import com.myxoz.life.repositories.utils.VersionedDayedCache
import com.myxoz.life.repositories.utils.VersionedDayedCache.Companion.updateDayedCacheFromTo
import com.myxoz.life.utils.atEndAsMillis
import com.myxoz.life.utils.atStartAsMillis
import com.myxoz.life.utils.def
import com.myxoz.life.utils.toLocalDate
import com.myxoz.life.viewmodels.TransactionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

class BankingRepo(
    private val readBankingDao: ReadBankingDao,
    private val writeSyncableDaos: API.WriteSyncableDaos,
    private val mainPrefs: SharedPreferences,
    private val appScope: CoroutineScope,
    private val waitingSyncDao: WaitingSyncDao,
) {
    private val _lastTransactionDay = MutableStateFlow(LocalDate.ofEpochDay(0L))
    val lastTransactionDay: StateFlow<LocalDate> = _lastTransactionDay
    private val _earliestTransaction = MutableStateFlow(LocalDate.now())
    val earliestTransaction: StateFlow<LocalDate> = _earliestTransaction
    private val zone: ZoneId = ZoneId.systemDefault()

    private val _transactions = VersionedDayedCache<String, BankingDisplayEntity, BankingDisplayEntity>(
        { key ->
            BankingDisplayEntity.from(key, readBankingDao) ?:throw Error("This transaction doesnt exist. Check where you get the id from! $key")
        },
        { date, cache ->
            val start = date.atStartOfDay(zone).toEpochSecond() * 1000L
            val end = date.plusDays(1).atStartOfDay(zone).toEpochSecond() * 1000L
            val sidecars = readBankingDao.getSidecarsBetween(start, end)
            val transactions = readBankingDao.getCombinedTransactions(start, end)
            cache.overwriteAll(
                transactions.map {
                    it.id to BankingDisplayEntity.from(it, sidecars.find { c -> c.transactionId == it.id })
                }
            )
        }
    ) { cache, _, old, new ->
        val newTransactionDate = new.timestamp.toLocalDate(zone)
        if(newTransactionDate > _lastTransactionDay.value){
            _lastTransactionDay.value = newTransactionDate
        }
        if(newTransactionDate < _earliestTransaction.value){
            _earliestTransaction.value = newTransactionDate
        }
        cache.updateDayedCacheFromTo(
            old?.timestamp?.toLocalDate(zone),
            new.timestamp.toLocalDate(zone),
            new
        ){
            it.equals(new)
        }
        checkForFutureTransaction(new)
    }
    suspend fun getCachedOrCache(date: LocalDate): Int {
        return _transactions.getCachedDayOrCache(date).data.size + _manualTransactions.getCachedDayOrCache(date).data.size
    }

    suspend fun updateCachedTransaction(transactionId: String, new: BankingDisplayEntity){
        _transactions.cache.overwrite(transactionId, new)
    }

    suspend fun updateCachedManualTransaction(new: ManualTransactionSyncable){
        _manualTransactions.cache.overwrite(new.id, new)
    }

    suspend fun deleteManualTransaction(old: ManualTransactionSyncable){
        DeleteEntry.requestSyncDelete(waitingSyncDao, old)
        writeSyncableDaos.bankingDao.deleteManualTransactionSyncable(old.id)
        _manualTransactions.cache.overwrite(old.id, null)
    }

    private val _manualTransactions: VersionedDayedCache<Long, ManualTransactionSyncable?, ManualTransactionSyncable> =
        VersionedDayedCache(
            { id ->
                ManualTransactionSyncable.fromEntity(
                    readBankingDao.getManualTransaction(id) ?:
                        error("Trying to fetch $id in ManualTransactionSyncable. This id is unknown to the db. Check where its from. If this id is not -1 or 0. This might actually mean this erroring should be replaced with an empty string. Review carefully")
                )
            },
            { date, cache ->
                val start = date.atStartAsMillis(zone)
                val end = date.atEndAsMillis(zone)
                cache.overwriteAll(readBankingDao.getManualTransactionsBetween(start, end).map { entity ->
                    entity.id to ManualTransactionSyncable.fromEntity(entity)
                })
            }
        ) { cache, key, old, new ->
            cache.updateDayedCacheFromTo(
                old?.timestamp?.toLocalDate(zone),
                new?.timestamp?.toLocalDate(zone),
                new
            ) { key == it.id }
        }
    fun getSortedTransactionsAt(date: LocalDate): Flow<List<BankingDisplayEntity>> {
        return combine(
            _transactions.getDayFlowFor(appScope, date),
            _manualTransactions.getDayFlowFor(appScope, date)
        ) { transactions, manualTransactions ->
            (
                    transactions?.data.def(listOf()) +
                            manualTransactions?.data.def(listOf())
                                .map { BankingDisplayEntity.from(it) }
                    )
                .sortedBy { it.timestamp }
        }
    }
    val sortedAllFlow = combine(
        _transactions.allDaysFlow,
        _manualTransactions.allDaysFlow
    ) { transactions, manualTransactions ->
        transactions.mapValues { (day, transactionList) ->
            val manualMapped = manualTransactions[day]
                ?.map { manual ->
                    BankingDisplayEntity.from(manual)
                }.orEmpty()

            (transactionList + manualMapped)
                .sortedByDescending { entity -> entity.timestamp }
        }
            .filterValues { it.isNotEmpty() }
    }
    suspend fun putFutureTransaction(
        amount: Int,
        timestamp: Long,
        digital: Boolean,
        cashless: Boolean,
        name: String?,
        purpose: String?
    ) {
        val future = ManualTransactionSyncable(
            -1L,
            digital,
            cashless,
            amount,
            name ?: "Unbekannt",
            timestamp,
            purpose
        )
        val alreadyExists = checkForExistingTransaction(future)
        if(alreadyExists) return
        putManualTransaction(future)
    }
    suspend fun putManualTransaction(maybeUnsyncedManual: ManualTransactionSyncable){
        val manual = maybeUnsyncedManual.ensureSynced()
        _manualTransactions.cache.overwrite(manual.id, manual)
        manual.saveToDB(writeSyncableDaos)
        waitingSyncDao.requestSync(manual)
    }
    private suspend fun checkForExistingTransaction(future: ManualTransactionSyncable): Boolean {
        val day = future.timestamp.toLocalDate(zone)
        val transactions = _transactions.getCachedDayOrCache(day).data
        val found = transactions.any { real -> matches(real, future) }
        if(found) removeFutureTransaction(future)
        return found
    }
    private suspend fun removeFutureTransaction(future: ManualTransactionSyncable) {
        _manualTransactions.cache.overwrite(future.id, null)
        DeleteEntry.requestSyncDelete(waitingSyncDao, future)
    }
    private suspend fun checkForFutureTransaction(newTransaction: BankingDisplayEntity) {
        val day = newTransaction.timestamp.toLocalDate(zone)
        val futureForDay = _manualTransactions.getCachedDayOrCache(day).data
        val match = futureForDay.find { manual -> matches(newTransaction, manual) }
        if(match!=null){
            removeFutureTransaction(match)
        }
    }
    private fun matches(
        real: BankingDisplayEntity,
        future: ManualTransactionSyncable
    ) = abs(real.timestamp - future.timestamp) <= 5 * 60 * 1000
            && real.amount == future.amountCents
    init {
        appScope.launch {
            _earliestTransaction.value = readBankingDao.getEarliestTransactionDate()?.toLocalDate(zone) ?: LocalDate.now()
        }
    }
    class BankingDisplayEntity private constructor(
        private val entity: BankingEntity?,
        private val sidecar: BankingSidecarEntity?,
        private val manual: ManualTransactionSyncable?,
    ) {
        val amount = manual?.amountCents ?: entity?.amountCents
            ?: throw Error("Trying to construct banking display entity without entity and manual transaction entity: null sidecar: $sidecar manualTransaction: null")
        val digital = manual?.digital ?: true
        val cashless = manual?.cashless ?: !entity!!.card
        val icon: Int = when{
            digital && cashless -> R.drawable.bank_transfer
            digital && !cashless -> R.drawable.pay_with_card
            !digital && cashless -> R.drawable.wireless_pay
            else -> R.drawable.cash
        }
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
        val timestamp = manual?.timestamp ?: sidecar?.date ?: entity?.purposeDate ?: entity?.valueDate ?: throw Error("This cannot happen")
        val displayTimestamp = manual?.timestamp ?: sidecar?.date ?: entity?.purposeDate
        fun displayName(predicted: String?): String = when{
            predicted != null -> predicted
            !entity?.fromName.isNullOrEmpty() -> "${entity.fromName} ${if (sidecar != null) " (${sidecar.name})" else ""}"
            !digital && !cashless -> manual?.name ?: ""
            else -> "Unbekannt"
        }
        val title = manual?.name ?: sidecar?.name ?: entity?.fromName ?: error("This can't happend")
        val subTitle = if(manual != null) manual.purpose ?: ""
            else if(sidecar == null && entity != null) entity.fromIban.chunked(4).joinToString(" ") else entity?.fromName ?: ""
        fun getStoredManualTransactionSyncable(): ManualTransactionSyncable? = manual
        fun equals(other: BankingDisplayEntity): Boolean {
            return if(entity != null && other.entity?.id == entity.id)
                true
            else manual != null && other.manual?.id == manual.id
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
            suspend fun from(id: String, readBankingDao: ReadBankingDao): BankingDisplayEntity? {
                return BankingDisplayEntity(
                    readBankingDao.getTransactionById(id)?:return null,
                    readBankingDao.getSidecar(id),
                    null
                )
            }
            fun from(entity: BankingEntity, sidecar: BankingSidecarEntity?): BankingDisplayEntity {
                return BankingDisplayEntity(entity, sidecar, null)
            }
            suspend fun from(entity: BankingEntity, readBankingDao: ReadBankingDao): BankingDisplayEntity =
                BankingDisplayEntity(entity, readBankingDao.getSidecar(entity.id), null)

            suspend fun from(id: Long, readBankingDao: ReadBankingDao): BankingDisplayEntity? {
                return BankingDisplayEntity(null, null, readBankingDao.getManualTransaction(id)?.let {
                    ManualTransactionSyncable.fromEntity(it)
                } ?: return null)
            }
            fun from(manualTransactionSyncable: ManualTransactionSyncable): BankingDisplayEntity {
                return BankingDisplayEntity(null, null, manualTransactionSyncable)
            }
            @Composable
            fun getResolvedName(transaction: BankingDisplayEntity, transactionViewModel: TransactionViewModel): State<String?> {
                return if(!transaction.isCashlessPayment) {
                    flowOf(null).collectAsState(null)
                } else {
                    produceState(null) {
                        value = transactionViewModel.predictTransaction(transaction)
                    }
                }
            }
            fun finalDailyBalance(transactions: List<BankingDisplayEntity>?): Long {
                if (transactions?.isEmpty() != false) return 0L
                val entites = transactions.mapNotNull { it.entity }

                // Try to find the "last" transaction recursively
                fun findLast(current: BankingEntity, remaining: List<BankingEntity>): BankingEntity {
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

                error("Could not determine transaction order")
            }
        }
    }
}