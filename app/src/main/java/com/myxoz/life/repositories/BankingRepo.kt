package com.myxoz.life.repositories

import android.content.SharedPreferences
import android.icu.util.Calendar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import com.myxoz.life.R
import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.DeleteEntry
import com.myxoz.life.api.syncables.ManualTransactionSyncable
import com.myxoz.life.api.syncables.TransactionSplitSyncable
import com.myxoz.life.dbwrapper.WaitingSyncDao
import com.myxoz.life.dbwrapper.banking.BankingEntity
import com.myxoz.life.dbwrapper.banking.BankingSidecarEntity
import com.myxoz.life.dbwrapper.banking.ReadBankingDao
import com.myxoz.life.repositories.utils.Cached
import com.myxoz.life.repositories.utils.Cached.Companion.cached
import com.myxoz.life.repositories.utils.PerformantInterlockedCache
import com.myxoz.life.repositories.utils.PerformantInterlockedCache.Companion.loadDays
import com.myxoz.life.repositories.utils.PerformantInterlockedCache.Companion.overwrite
import com.myxoz.life.repositories.utils.PerformantInterlockedCache.Companion.remove
import com.myxoz.life.utils.atEndAsMillis
import com.myxoz.life.utils.atStartAsMillis
import com.myxoz.life.utils.def
import com.myxoz.life.utils.formatTimeStamp
import com.myxoz.life.utils.toLocalDate
import com.myxoz.life.viewmodels.TransactionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

    private val _transactions = PerformantInterlockedCache.dayedSame<String, BankingDisplayEntity>(
        appScope,
        { listOf(it.timestamp.toLocalDate(zone)) },
        { first, other -> first.key.second == other.key.second },
        { key ->
            BankingDisplayEntity.from(key, readBankingDao) ?: throw Error("This transaction doesnt exist. Check where you get the id from! $key")
        },
        { from, to ->
            val start = from.atStartAsMillis(zone)
            val end = to.atEndAsMillis(zone)
            val sidecars = readBankingDao.getSidecarsBetween(start, end).associateBy { it.transactionId }
            val transactions = readBankingDao.getCombinedTransactions(start, end)
            transactions.map {
                it.id to BankingDisplayEntity.from(it, sidecars[it.id])
            }
        }
    ) { _, new ->
        val newTransactionDate = new.timestamp.toLocalDate(zone)
        if(newTransactionDate > _lastTransactionDay.value){
            _lastTransactionDay.value = newTransactionDate
        }
        if(newTransactionDate < _earliestTransaction.value){
            _earliestTransaction.value = newTransactionDate
        }
        appScope.launch {
            checkForFutureTransaction(new)
        }
    }
    suspend fun prepareBeween(from: LocalDate, to: LocalDate): Int {
        return _transactions.loadDays(from, to).values.sumOf { it.size } + _manualTransactions.loadDays(from, to).values.sumOf { it.size }
    }

    fun updateCachedTransaction(transactionId: String, new: BankingDisplayEntity){
        _transactions.overwrite(transactionId, new)
    }

    fun updateCachedManualTransaction(new: ManualTransactionSyncable){
        _manualTransactions.overwrite(new.id, new)
    }

    suspend fun deleteManualTransaction(old: ManualTransactionSyncable){
        DeleteEntry.requestSyncDelete(waitingSyncDao, old)
        writeSyncableDaos.bankingDao.deleteManualTransactionSyncable(old.id)
        _manualTransactions.remove(old.id)
    }

    private val _manualTransactions = PerformantInterlockedCache.dayedCached(
        appScope,
        {
            listOfNotNull(it.value?.timestamp?.toLocalDate(zone))
        },
        { first, other -> first.value?.id == other.id },
        { id ->
            ManualTransactionSyncable.fromEntity(
                readBankingDao.getManualTransaction(id) ?:
                error("Trying to fetch $id in ManualTransactionSyncable. This id is unknown to the db. Check where its from. If this id is not -1 or 0. This might actually mean this erroring should be replaced with an empty string. Review carefully")
            ).cached
        },
        { from, to ->
            val start = from.atStartAsMillis(zone)
            val end = to.atEndAsMillis(zone)
            readBankingDao.getManualTransactionsBetween(start, end).map { entity ->
                entity.id to ManualTransactionSyncable.fromEntity(entity).cached
            }
        },
    )
    fun getSortedTransactionsAt(date: LocalDate): Flow<List<BankingDisplayEntity>> {
        return combine(
            _transactions.getInterlockedFlowFor(date),
            _manualTransactions.getInterlockedFlowFor(date)
        ) { transactions, manualTransactions ->
            (
                    transactions.def(listOf()) +
                            manualTransactions.def(listOf())
                                .map { BankingDisplayEntity.from(it) }
                    )
                .sortedBy { it.timestamp }
        }
    }
    val sortedAllFlow = combine(
        _transactions.everythingFlow,
        _manualTransactions.everythingFlow
    ) { transactions, manualTransactions ->
        transactions.entries.sortedByDescending { it.key }.mapNotNull { (day, transactionList) ->
            val manualMapped = manualTransactions[day]
                ?.map { manual ->
                    BankingDisplayEntity.from(manual)
                }.orEmpty()

            day to (transactionList + manualMapped)
                .sortedByDescending { entity -> entity.timestamp }
                .ifEmpty { return@mapNotNull null }
        }
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
        _manualTransactions.overwrite(manual.id, manual)
        manual.saveToDB(writeSyncableDaos)
        waitingSyncDao.requestSync(manual)
    }
    private suspend fun checkForExistingTransaction(future: ManualTransactionSyncable): Boolean {
        val day = future.timestamp.toLocalDate(zone)
        val transactions = _transactions.loadDays(day, day)[day] ?: return false
        val found = transactions.any { real -> matches(real, future) }
        if(found) deleteManualTransaction(future)
        return found
    }
    private suspend fun migrateFutureTransaction(future: ManualTransactionSyncable, replacement: BankingDisplayEntity) {
        val new = _transactionSplitCache.getCachedContent(future.id to null)?.value?.copy(remoteId = replacement.key.second).cached
        if(new.value != null) {
            new.value.saveToDB(writeSyncableDaos)
            _transactionSplitCache.overwriteAll(
                listOf(
                    (null to replacement.key.second) to new,
                    (future.id to replacement.key.second) to new,
                    (future.id to null) to new
                )
            )
        }
        deleteManualTransaction(future)
    }
    private suspend fun checkForFutureTransaction(newTransaction: BankingDisplayEntity) {
        val day = newTransaction.timestamp.toLocalDate(zone)
        val futureForDay = _manualTransactions.fetchOrGetInterlocked(day)
        val match = futureForDay.find { manual -> matches(newTransaction, manual) }
        if(match!=null) {
            migrateFutureTransaction(match, newTransaction)
        }
    }
    private val _transactionSplitCache = PerformantInterlockedCache.cached<BankingDisplayEntityKey, Long, TransactionSplitSyncable>(
        appScope,
        { it.value?.parts?.map { part -> part.person } ?: emptyList() },
        { first, other -> first.value?.id == other.id },
        {
            val syncableId = it.first
            val remoteId = it.second
            if(syncableId == null && remoteId == null) return@cached Cached.Null
            val entity = readBankingDao.getTransactionSplit(remoteId, syncableId) ?: return@cached Cached.Null
            TransactionSplitSyncable(
                entity.id,
                syncableId,
                remoteId,
                readBankingDao.getTransactionSplitParts(entity.id).map { part ->
                    TransactionSplitSyncable.Companion.Part(part.person, part.amount)
                }
            ).cached
        },
        { _, _ -> listOf() },
    )
    fun getSplitFlow(key: BankingDisplayEntityKey) =
        _transactionSplitCache.flowFor(key)

    suspend fun saveAndSyncSplit(maybeUnsyncedSplit: TransactionSplitSyncable) {
        val split = maybeUnsyncedSplit.ensureSynced()
        _transactionSplitCache.overwrite(split.key, split)
        split.saveToDB(writeSyncableDaos)
        waitingSyncDao.requestSync(split)
    }
    suspend fun deleteSplit(old: TransactionSplitSyncable) {
        DeleteEntry.requestSyncDelete(waitingSyncDao, old)
        writeSyncableDaos.bankingDao.deleteSplitAndParts(old.id)
        _transactionSplitCache.remove(old.key)
    }
    fun updateCachedSplit(new: TransactionSplitSyncable){
        _transactionSplitCache.overwrite(new.key, new)
    }

    private fun matches(
        real: BankingDisplayEntity,
        future: ManualTransactionSyncable
    ) = abs(real.timestamp - future.timestamp) <= 5 * 60 * 1000
            && real.amount == future.amountCents

    fun getDebtFor(person: Long): Flow<List<TransactionSplitSyncable>?> {
        appScope.launch {
            val splits = readBankingDao.getTransactionSplitsByPerson(person)
            val ids = splits.map { it.id }
            val allSplits = readBankingDao.getAllSplits(ids)
            val allSplitsParts = readBankingDao.getAllSplitParts(ids).groupBy { it.id }
            _transactionSplitCache.overwriteAll(
                allSplits.map { (it.syncableId to it.remoteId) to
                        TransactionSplitSyncable(
                            it.id,
                            it.syncableId,
                            it.remoteId,
                            (allSplitsParts[it.id] ?: listOf()).map (
                                TransactionSplitSyncable.Companion.Part::from
                            )
                        ).cached
                }
            )
        }
        return _transactionSplitCache.getInterlockedFlowFor(person)
    }

    fun getTransaction(pair: BankingDisplayEntityKey): Flow<BankingDisplayEntity?> {
        val flow = if(pair.second != null)
            _transactions.flowFor(pair.second!!)
        else
            _manualTransactions.flowFor(pair.first ?: error("Trying to fetch transaction with $pair (both null)")).map {
                val a = it?.value ?: return@map null
                BankingDisplayEntity.from(a)
            }
        return flow
    }

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
        val key = manual?.id to entity?.id
        val timestamp = manual?.timestamp ?: sidecar?.date ?: entity?.purposeDate ?: entity?.valueDate ?: throw Error("This cannot happen")
        val displayTimestamp = manual?.timestamp ?: sidecar?.date ?: entity?.purposeDate
        fun displayName(predicted: String?): String = when{
            predicted != null -> predicted
            !entity?.fromName.isNullOrEmpty() -> "${entity.fromName}${if (sidecar != null) " (${sidecar.name})" else ""}"
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

        override fun toString(): String = "Title: $title; Subtitle: $subTitle; Amount: $amount; At: $timestamp (${timestamp.formatTimeStamp(Calendar.getInstance())}); Categorization: $categorization; Key: $key"
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
typealias BankingDisplayEntityKey = Pair<Long?, String?>