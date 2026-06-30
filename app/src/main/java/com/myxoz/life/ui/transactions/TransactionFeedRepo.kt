package com.myxoz.life.ui.transactions

import androidx.compose.foundation.lazy.LazyListState
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.api.syncables.TransactionSplitSyncable
import com.myxoz.life.storage.dbwrapper.banking.ReadBankingDao
import com.myxoz.life.storage.interfaces.BankingDisplayEntityKey
import com.myxoz.life.storage.interfaces.BankingRepo
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.utils.Cached
import com.myxoz.life.storage.interfaces.utils.StateFlowCache
import com.myxoz.life.storage.interfaces.utils.subscribeToColdFlow
import com.myxoz.life.utils.syncToPrefs
import com.myxoz.life.utils.toLocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class TransactionFeedRepo(private val repos: DatabaseInterface) {
    // TODO Consider splitting this up into application and activity scoped repo
    suspend fun predictTransaction(transaction: BankingRepo.BankingDisplayEntity): String? {
        val ts = transaction.timestamp
        return repos.aiPredictionRepo.predictPaymentName(
            ReadBankingDao.BankingTrainingRow(
                transaction.amount,
                ts,
                "(What we want to find out)",
                repos.readSyncableDaos.bankingDao.getLastTravelEventEndBefore(ts)
            )
        )
    }

    val isEditingSplit = MutableStateFlow(false)
    val lazyListState = LazyListState()
    val orderedAllTransactionFlow = repos.bankingRepo.sortedAllFlow.subscribeToColdFlow(repos.appScope, listOf())

    val lastFetchedDay = MutableStateFlow<LocalDate>(LocalDate.now().plusDays(1))

    fun onLastVisibleIndexChanged(lastVisibleIndex: Int) {
        val zone = ZoneId.systemDefault()
        val flat = orderedAllTransactionFlow.value.flatMap { it.second.map { it.timestamp.toLocalDate(zone) } + listOf(it.first) }
        val currentMostRecentDay = flat.firstOrNull() ?: LocalDate.now()
        var difference = flat.size - lastVisibleIndex
        repos.appScope.launch {
            while (difference < 20 || currentMostRecentDay < lastFetchedDay.value) { // At least 20 transaction
                val next = lastFetchedDay.value.minusDays(20)
                if (next < repos.bankingRepo.earliestTransaction.value) return@launch
                val forDay = repos.bankingRepo.prepareBeween(next, lastFetchedDay.value)
                difference += forDay
                lastFetchedDay.value = next
            }
        }
    }
    val showBalance = MutableStateFlow(repos.prefs.getBoolean("show_balance", false)).apply {
        syncToPrefs(repos.appScope, repos.prefs, "show_balance", Boolean::class)
    }
    private val peopleWithIbanLikeCached = StateFlowCache<String, List<PersonSyncable>> {
        repos.peopleInterface.getPeopleWithIbanLike(it)
            .subscribeToColdFlow(repos.appScope, listOf())
    }
    fun getPeopleWithIbanLike(iban: String) = peopleWithIbanLikeCached.get(iban)
    private val editingSplitCache =
        StateFlowCache<BankingDisplayEntityKey, Cached<TransactionSplitSyncable>?> {
            repos.bankingRepo.getSplitFlow(it).subscribeToColdFlow(repos.appScope, null)
        }
    val editingSplit = MutableStateFlow(TransactionSplitSyncable(-1L, null, null, listOf()))
    fun getSplit(transaction: BankingRepo.BankingDisplayEntity) = editingSplitCache.get(transaction.key)
    suspend fun deleteSplit(split: TransactionSplitSyncable) = repos.bankingRepo.deleteSplit(split)
    suspend fun saveAndSyncSplit(split: TransactionSplitSyncable) {
        repos.bankingRepo.saveAndSyncSplit(split)
    }

    private val _transactionFlowCache =
        StateFlowCache<BankingDisplayEntityKey, BankingRepo.BankingDisplayEntity?> {
            repos.bankingRepo.getTransaction(it).subscribeToColdFlow(repos.appScope, null)
        }
    fun getTransaction(key: BankingDisplayEntityKey) = _transactionFlowCache.get(key)

    val getSelf = repos.peopleInterface.meFlow
    @OptIn(ExperimentalCoroutinesApi::class)
    val lastTransactions = repos.bankingRepo.lastTransactionDay.flatMapConcat { date ->
        // Surely banks will be able to process payments in a wekk of time
        repos.bankingRepo.getBankTransactionsBetweenAt(date.minusDays(7), date )
    }.subscribeToColdFlow(repos.appScope, listOf())
    val inspectedTransaction = MutableStateFlow<BankingRepo.BankingDisplayEntity?>(null)
    init {
        repos.appScope.launch {
            // This might be really unnecessary, but is really clean
            // This could stop an edge case if today is not yet loaded and the day flips over
            // to a new day and then the day is not added so you need to restart the app
            repos.calendarInterface.todayFlow.collect { date ->
                repos.bankingRepo.prepareGetBankTransactionsAt(date)
            }
        }
    }
}