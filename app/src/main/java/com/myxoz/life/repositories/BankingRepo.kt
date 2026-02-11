package com.myxoz.life.repositories

import android.content.SharedPreferences
import androidx.core.content.edit
import com.myxoz.life.dbwrapper.banking.BankingEntity
import com.myxoz.life.dbwrapper.banking.BankingSidecarEntity
import com.myxoz.life.dbwrapper.banking.ReadBankingDao
import com.myxoz.life.repositories.utils.VersionedCache
import com.myxoz.life.utils.getStringOrNull
import com.myxoz.life.utils.jsonObjArray
import com.myxoz.life.utils.toLocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

class BankingRepo(
    private val readBankingDao: ReadBankingDao,
    private val mainPrefs: SharedPreferences,
    private val appScope: CoroutineScope,
) {
    private val _lastTransactionDay = MutableStateFlow(LocalDate.ofEpochDay(0L))
    val lastTransaction: StateFlow<LocalDate> = _lastTransactionDay
    private val _earliestTransaction = MutableStateFlow(LocalDate.now())
    val earliestTransaction: StateFlow<LocalDate> = _earliestTransaction
    private val zone: ZoneId = ZoneId.systemDefault()
    private val prefetchedDays = mutableSetOf<LocalDate>()
    private val _dayCache = VersionedCache<LocalDate, List<BankingDisplayEntity>>(
        { listOf() }
    )
    fun getTransactionsAt(date: LocalDate): Flow<List<BankingDisplayEntity>?> {
        appScope.launch { loadTransactionForDayIfNeeded(date) }
        return _dayCache.flowByKey(appScope, date).map { it?.data?.sortedByDescending { it.resolveEffectiveDate() } }
    }
    suspend fun getCachedOrCache(date: LocalDate): List<BankingDisplayEntity> {
        return _dayCache.get(date).data.sortedByDescending { it.resolveEffectiveDate() }
    }
    val allTransactionsFlow = _dayCache.allMapedFlows()

    private val _cache = VersionedCache<String, BankingDisplayEntity>(
        {
            BankingDisplayEntity.from(it, readBankingDao) ?:throw Error("This transaction doesnt exist. Check where you get the id from! $it")
        }
    ) { key, old, new ->
        val newTransactionDate = new.resolveEffectiveDate().toLocalDate(zone)
        if(newTransactionDate > _lastTransactionDay.value){
            _lastTransactionDay.value = newTransactionDate
        }
        if(newTransactionDate < _earliestTransaction.value){
            _earliestTransaction.value = newTransactionDate
        }
        if(old != null) {
            _dayCache.updateWith(old.resolveEffectiveDate().toLocalDate(zone)){ list ->
                list.filterNot { it.entity.id == key }
            }
        }
        checkForFutureTransaction(new)
    }
    suspend fun updateTransaction(new: BankingDisplayEntity){
        _cache.overwrite(new.entity.id, new)
    }

    private suspend fun loadTransactionForDayIfNeeded(date: LocalDate) {
        if(date in prefetchedDays) return
        prefetchedDays.add(date)
        val start = date.atStartOfDay(zone).toEpochSecond() * 1000L
        val end = date.plusDays(1).atStartOfDay(zone).toEpochSecond() * 1000L
        val sidecars = readBankingDao.getSidecarsBetween(start, end)
        val transactions = readBankingDao.getCombinedTransactions(start, end)
        val trans = transactions.map {
            BankingDisplayEntity(
                it,
                sidecars.find { c -> c.transactionId == it.id }
            )
        }
        _cache.overwriteAll(trans.map { it.entity.id to it })
    }
    private val _futureTransactions: VersionedCache<LocalDate, List<BankingDisplayEntity>> = VersionedCache(
        {
            listOf()
            // Again: Not exact but not necessary because we construct these in contructor
        }
    )
    fun getFutureTransactions(date: LocalDate) = _futureTransactions.flowByKey(appScope, date)
    suspend fun putFutureTransaction(amount: Int, timestamp: Long, from: String?) {
        val future = BankingDisplayEntity(getFutureTransactionBy(amount, timestamp, from), null)
        _futureTransactions.updateWith(future.resolveEffectiveDate().toLocalDate(zone)) { list ->
            list + future
        }
        val payment = JSONObject().apply {
            put("amount", amount)
            put("timestamp", timestamp)
        }

        val paymentsRaw = mainPrefs.getString("payments", "[]")?:"[]"
        val paymentsArray = JSONArray(paymentsRaw)

        paymentsArray.put(payment)

        mainPrefs.edit {
            putString("payments", paymentsArray.toString())
        }

        checkForExistingTransaction(future)
    }
    private suspend fun checkForExistingTransaction(future: BankingDisplayEntity) {
        val day = future.resolveEffectiveDate().toLocalDate(zone)
        loadTransactionForDayIfNeeded(day)
        val transactions = _dayCache.getCached(day)?.data ?: return
        if(transactions.any { real -> matches(real, future) })
            removeFutureTransaction(future)
    }
    private suspend fun removeFutureTransaction(future: BankingDisplayEntity) {
        val json = mainPrefs.getString("payments", null) ?: "[]"
        println("WE FOUND A TRANSACTION: ${future.entity.amountCents} at ${future.resolveEffectiveDate()}");
        val filtered = JSONArray(json).jsonObjArray.filterNot {
            -it.getInt("amount") == future.entity.amountCents &&
                    it.getLong("timestamp") == future.entity.purposeDate
        }
        mainPrefs.edit {
            putString("payments", JSONArray().apply {
                filtered.forEach { this.put(it) }
            }.toString())
        }
        _futureTransactions.updateWith(future.resolveEffectiveDate().toLocalDate(zone)) { list ->
            list.filterNot { matches(it, future) }
        }
    }
    private suspend fun checkForFutureTransaction(real: BankingDisplayEntity) {
        val day = real.resolveEffectiveDate().toLocalDate(zone)
        val futureForDay = _futureTransactions.getCached(day)?.data ?: return
        val match = futureForDay.firstOrNull { future ->
            matches(real, future)
        } ?: return

        removeFutureTransaction(match)
    }
    private fun matches(
        real: BankingDisplayEntity,
        future: BankingDisplayEntity
    ) = abs(real.resolveEffectiveDate() - future.resolveEffectiveDate()) <= 2 * 60 * 1000
            && real.entity.amountCents == future.entity.amountCents
    init {
        appScope.launch {
            _earliestTransaction.value = readBankingDao.getEarliestTransactionDate()?.toLocalDate(zone) ?: LocalDate.now()
        }
        val allTransactions =
            JSONArray(mainPrefs.getString("payments", null) ?: "[]").jsonObjArray.map {
                getFutureTransactionBy(
                    it.getInt("amount"),
                    it.getLong("timestamp"),
                    it.getStringOrNull("from")
                )
            }
        appScope.launch {
            _futureTransactions.overwriteAll(
                allTransactions
                    .groupBy { it.valueDate.toLocalDate(zone) }
                    .map { dated ->
                        dated.key to dated.value.map {
                            BankingDisplayEntity(it, null)
                        }
                    }
            )
        }
    }
    class BankingDisplayEntity(
        val entity: BankingEntity,
        val sidecar: BankingSidecarEntity?
    ) {
        fun resolveEffectiveDate() = sidecar?.date ?: entity.purposeDate ?: entity.valueDate
        companion object {
            suspend fun from(id: String, readBankingDao: ReadBankingDao): BankingDisplayEntity? {
                return BankingDisplayEntity(
                    readBankingDao.getTransactionById(id)?:return null,
                    readBankingDao.getSidecar(id)
                )
            }
            suspend fun from(entity: BankingEntity, readBankingDao: ReadBankingDao): BankingDisplayEntity {
                return BankingDisplayEntity(
                    entity,
                    readBankingDao.getSidecar(entity.id)
                )
            }
        }
    }
    companion object {
        fun getFutureTransactionBy(
            amount: Int,
            timestamp: Long,
            from: String?
        ) = BankingEntity(
                "",
                true,
                true, // Semantic value, but works. Compatible with .isWirelessPayment
                amount,
                "EUR",
                "",
                timestamp,
                from?:"",
                "",
                "",
                "",
                0,
                timestamp,
                timestamp
            )
    }
}