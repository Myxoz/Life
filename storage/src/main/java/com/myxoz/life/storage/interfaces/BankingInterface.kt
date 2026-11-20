package com.myxoz.life.storage.interfaces

import android.util.Log
import com.myxoz.life.core.Rangable
import com.myxoz.life.core.Rangable.Companion.rangableTo
import com.myxoz.life.core.Rangable.Companion.startAndEndInMs
import com.myxoz.life.core.dif
import com.myxoz.life.core.toLocalDate
import com.myxoz.life.storage.api.API
import com.myxoz.life.storage.api.helper.MixedBankingEntity
import com.myxoz.life.storage.api.helper.SyncHelper
import com.myxoz.life.storage.api.helper.SyncHelper.Companion.deleteAndSync
import com.myxoz.life.storage.api.helper.SyncHelper.Companion.saveAndRequestSync
import com.myxoz.life.storage.api.syncables.ManualTransactionSyncable
import com.myxoz.life.storage.api.syncables.TransactionSplitSyncable
import com.myxoz.life.storage.api.syncables.additionals.TransactionSplitPart
import com.myxoz.life.storage.caches.InterlockedCache
import com.myxoz.life.storage.caches.InterlockedCache.Companion.overwrite
import com.myxoz.life.storage.caches.InterlockedCache.Companion.remove
import com.myxoz.life.storage.convertion.SyncableFromEntity.fromEntity
import com.myxoz.life.storage.dbwrapper.banking.ReadBankingDao
import com.myxoz.life.storage.interfaces.utils.Cached
import com.myxoz.life.storage.interfaces.utils.Cached.Companion.cached
import com.myxoz.life.storage.toLocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.minutes

class BankingInterface internal constructor(
    private val readBankingDao: ReadBankingDao,
    private val appScope: CoroutineScope,
    private val syncHelper: SyncHelper
) {
    private val _lastTransactionDay = MutableStateFlow(LocalDate.ofEpochDay(0L))
    val lastTransactionDay: StateFlow<LocalDate> = _lastTransactionDay
    private val _earliestTransaction = MutableStateFlow(LocalDate.now())
    val earliestTransaction: StateFlow<LocalDate> = _earliestTransaction
    private val zone: ZoneId = ZoneId.systemDefault()
    private fun updateTransactionsDate(new: MixedBankingEntity){
        val newTransactionDate = new.processDate?.toLocalDate(zone)
        if(newTransactionDate != null) {
            // Assures this is a bank transaction
            if(newTransactionDate > _lastTransactionDay.value) {
                Log.d("Transaction", "last transaction day $newTransactionDate (${new.processDate}) with $new")
                _lastTransactionDay.value = newTransactionDate
            }
            if(newTransactionDate < _earliestTransaction.value){
                _earliestTransaction.value = newTransactionDate
            }
        }
    }
    private val _transactions = InterlockedCache.dayedSame(
        appScope,
        { first, other -> first.key.second == other.key.second },
        { listOf(it.timestamp.toLocalDate(zone)) },
        { it.key.second ?: error("Tried to get banking display key in _transaction and got null meaning the transaction in the bankTransactionCache is not a bankTransaction. Check logic.") },
        { key ->
            (MixedBankingEntity.from(key, readBankingDao) ?: error("This transaction doesnt exist. Check where you get the id from! $key"))
                .also { transaction -> updateTransactionsDate(transaction) }
        },
        { range ->
            val (start, end) = range.startAndEndInMs(zone)
            val sidecars = readBankingDao.getSidecarsBetween(start, end).associateBy { it.transactionId }
            val transactions = readBankingDao.getCombinedTransactions(start, end)
            transactions.map {
                MixedBankingEntity.from(it.fromEntity(), sidecars[it.id])
                    .also { transaction -> updateTransactionsDate(transaction) }
            }
        }
    ) { _, new ->
        updateTransactionsDate(new)
        appScope.launch {
            checkForFutureTransaction(new)
        }
    }
    suspend fun prepareBeween(from: LocalDate, to: LocalDate): Int {
        val range = from.rangableTo(to)
        return _transactions.fetchRange(range).size + _manualTransactions.fetchRange(range).size
    }

    internal fun updateCachedTransaction(transactionId: String, new: MixedBankingEntity){
        _transactions.overwrite(transactionId, new)
    }

    internal fun updateCachedManualTransaction(new: ManualTransactionSyncable){
        _manualTransactions.overwrite(new.id, new)
    }

    internal suspend fun deleteManualTransaction(old: ManualTransactionSyncable){
        old.deleteAndSync(syncHelper)
        _manualTransactions.remove(old.id)
    }

    private val _manualTransactions = InterlockedCache.dayedCached(
        appScope,
        { first, other -> first.value?.id == other.id },
        {
            listOfNotNull(it.value?.timestamp?.toLocalDate(zone))
        },
        { it.id },
        { id ->
            val entity = readBankingDao.getManualTransaction(id) ?:
                error("Trying to fetch $id in ManualTransactionSyncable. This id is unknown to the db. Check where its from. If this id is not -1 or 0. This might actually mean this erroring should be replaced with an empty string. Review carefully")
            entity.fromEntity().cached
        },
        { range ->
            val (start, end) = range.startAndEndInMs(zone)
            readBankingDao.getManualTransactionsBetween(start, end).map { entity ->
                entity.fromEntity()
            }
        },
    )
    suspend fun prepareGetBankTransactionsAt(date: LocalDate) = _transactions.fetchOrGetInterlocked(date)
    fun getBankTransactionsAt(date: LocalDate) = _transactions.getInterlockedFlowFor(date)
    fun getBankTransactionsBetweenAt(start: LocalDate, end: LocalDate) = _transactions.flowByRange(start.rangableTo(end))
    fun getSortedTransactionsAt(date: LocalDate): Flow<List<MixedBankingEntity>> {
        return combine(
            _transactions.getInterlockedFlowFor(date).filterNotNull(),
            _manualTransactions.getInterlockedFlowFor(date).filterNotNull()
        ) { transactions, manualTransactions ->
            val manualTransactions = manualTransactions.map(MixedBankingEntity::from)
            (transactions + manualTransactions).sortedBy { it.timestamp }
        }
    }
    val sortedAllFlow = combine(
        _transactions.everythingFlow,
        _manualTransactions.everythingFlow
    ) { transactions, manualTransactions ->
        transactions.entries.sortedByDescending { it.key }.mapNotNull { (day, transactionList) ->
            val manualMapped = manualTransactions[day]
                ?.map { manual ->
                    MixedBankingEntity.from(manual)
                }.orEmpty()

            day to (transactionList + manualMapped)
                .sortedByDescending { entity -> entity.timestamp }
                .ifEmpty { return@mapNotNull null }
        }
    }
    suspend fun putFutureTransaction(
        amount: Int,
        timestamp: Instant,
        digital: Boolean,
        cashless: Boolean,
        name: String?,
        purpose: String?
    ) {
        val future = ManualTransactionSyncable(
            API.generateId(),
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
    private suspend fun putManualTransaction(manual: ManualTransactionSyncable){
        _manualTransactions.overwrite(manual.id, manual)
        manual.saveAndRequestSync(syncHelper)
    }
    private suspend fun checkForExistingTransaction(future: ManualTransactionSyncable): Boolean {
        val day = future.timestamp.toLocalDate(zone)
        val transactions = _transactions.fetchRange(Rangable.ofSingle(day))
        val found = transactions.any { real -> matches(real, future) }
        if(found) deleteManualTransaction(future)
        return found
    }
    private suspend fun migrateFutureTransaction(future: ManualTransactionSyncable, replacement: MixedBankingEntity) {
        val new = _transactionSplitCache.getCachedContent(future.id to null)?.value?.copy(remoteId = replacement.key.second).cached
        if(new.value != null) {
            new.value.saveAndRequestSync(syncHelper)
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
    private suspend fun checkForFutureTransaction(newTransaction: MixedBankingEntity) {
        val day = newTransaction.timestamp.toLocalDate(zone)
        val futureForDay = _manualTransactions.fetchOrGetInterlocked(day)
        val match = futureForDay.find { manual -> matches(newTransaction, manual) }
        if(match!=null) {
            migrateFutureTransaction(match, newTransaction)
        }
    }
    private val _transactionSplitCache = InterlockedCache.cachedNonRange<BankingDisplayEntityKey, Long, TransactionSplitSyncable>(
        appScope,
        { first, other -> first.value?.id == other.id },
        { it.value?.parts?.map { part -> part.person } ?: emptyList() },
        { it.key },
        {
            val syncableId = it.first
            val remoteId = it.second
            if(syncableId == null && remoteId == null) return@cachedNonRange Cached.Null
            val entity = readBankingDao.getTransactionSplit(remoteId, syncableId) ?: return@cachedNonRange Cached.Null
            TransactionSplitSyncable(
                entity.id,
                syncableId,
                remoteId,
                readBankingDao.getTransactionSplitParts(entity.id).map { part ->
                    TransactionSplitPart(part.person, part.amount)
                }
            ).cached
        },
        { person ->
            val splits = readBankingDao.getTransactionSplitsByPerson(person)
            val ids = splits.map { it.id }
            readBankingDao.getAllSplits(ids).map {
                it.fromEntity(readBankingDao)
            }
        }
    )
    fun getSplitFlow(key: BankingDisplayEntityKey) = _transactionSplitCache.flowFor(key)

    internal suspend fun saveAndSyncSplit(syncable: TransactionSplitSyncable) {
        _transactionSplitCache.overwrite(syncable.key, syncable)
        syncable.saveAndRequestSync(syncHelper)
    }
    suspend fun deleteSplit(old: TransactionSplitSyncable) {
        old.deleteAndSync(syncHelper)
        _transactionSplitCache.remove(old.key)
    }
    internal fun updateCachedSplit(new: TransactionSplitSyncable){
        _transactionSplitCache.overwrite(new.key, new)
    }

    private fun matches(
        real: MixedBankingEntity,
        future: ManualTransactionSyncable
    ) = dif(real.timestamp, future.timestamp) <= 5.minutes
            && real.amount == future.amountCents

    fun getDebtFor(person: Long): Flow<List<TransactionSplitSyncable>?> {
        return _transactionSplitCache.getInterlockedFlowFor(person)
    }

    fun getTransaction(pair: BankingDisplayEntityKey): Flow<MixedBankingEntity?> {
        val flow = if(pair.second != null)
            _transactions.flowFor(pair.second!!)
        else
            _manualTransactions.flowFor(pair.first ?: error("Trying to fetch transaction with $pair (both null)")).map {
                val a = it?.value ?: return@map null
                MixedBankingEntity.from(a)
            }
        return flow
    }

    init {
        appScope.launch {
            _earliestTransaction.value = readBankingDao.getEarliestTransactionDate()?.toLocalDate(zone) ?: LocalDate.now()
        }
    }
}
typealias BankingDisplayEntityKey = Pair<Long?, String?>