package com.myxoz.life.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.api.syncables.TransactionSplitSyncable
import com.myxoz.life.dbwrapper.banking.ReadBankingDao
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.repositories.BankingDisplayEntityKey
import com.myxoz.life.repositories.BankingRepo
import com.myxoz.life.repositories.utils.StateFlowCache
import com.myxoz.life.repositories.utils.subscribeToColdFlow
import com.myxoz.life.utils.syncToPrefs
import com.myxoz.life.utils.toLocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class TransactionViewModel(private val repos: AppRepositories): ViewModel() {
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
    val orderedAllTransactionFlow = repos.bankingRepo.sortedAllFlow.subscribeToColdFlow(viewModelScope, mapOf())

    val lastFetchedDay = MutableStateFlow<LocalDate>(LocalDate.now().plusDays(1))

    fun onLastVisibleIndexChanged(lastVisibleIndex: Int) {
        val zone = ZoneId.systemDefault()
        val flat = orderedAllTransactionFlow.value.flatMap { it.value.map { it.timestamp.toLocalDate(zone) } + listOf(it.key) }
        val currentMostRecentDay = flat.firstOrNull() ?: LocalDate.now()
        var difference = flat.size - lastVisibleIndex
        viewModelScope.launch {
            while (difference < 20 || currentMostRecentDay < lastFetchedDay.value) { // At least 20 transaction
                val next = lastFetchedDay.value.minusDays(1)
                if (next < repos.bankingRepo.earliestTransaction.value) return@launch
                val forDay = repos.bankingRepo.getCachedOrCache(next)
                difference += forDay
                lastFetchedDay.value = next
            }
        }
    }
    private val onDayCachedFlows = StateFlowCache<LocalDate, List<BankingRepo.BankingDisplayEntity>>{ date ->
        repos.bankingRepo.getSortedTransactionsAt(date).subscribeToColdFlow(viewModelScope, listOf())
    }
    fun getOnDay(date: LocalDate) = onDayCachedFlows.get(date)
    val showBalance = MutableStateFlow(repos.prefs.getBoolean("show_balance", false)).apply {
        syncToPrefs(viewModelScope, repos.prefs, "show_balance", Boolean::class)
    }
    private val peopleWithIbanLikeCached = StateFlowCache<String, List<PersonSyncable>>{
        repos.peopleRepo.getPeopleWithIbanLike(it).subscribeToColdFlow(viewModelScope, listOf())
    }
    fun getPeopleWithIbanLike(iban: String) = peopleWithIbanLikeCached.get(iban)
    private val editingSplitCache = StateFlowCache<BankingDisplayEntityKey, TransactionSplitSyncable?>{
        repos.bankingRepo.getSplitFlow(it).map { it?.data }.subscribeToColdFlow(viewModelScope, null)
    }
    val editingSplit = MutableStateFlow(TransactionSplitSyncable(-1L, null, null, listOf()))
    fun getSplit(transaction: BankingRepo.BankingDisplayEntity) = editingSplitCache.get(transaction.key)
    suspend fun deleteSplit(split: TransactionSplitSyncable) = repos.bankingRepo.deleteSplit(split)
    suspend fun saveAndSyncSplit(split: TransactionSplitSyncable) {
        repos.bankingRepo.saveAndSyncSplit(split)
    }

    private val _peopleCache = StateFlowCache<Long, PersonSyncable?>({
        repos.peopleRepo.getPerson(it).map { it?.data }.subscribeToColdFlow(viewModelScope, null)
    })
    fun getPerson(person: Long) = _peopleCache.get(person)

    private val _transactionFlowCache = StateFlowCache<BankingDisplayEntityKey, BankingRepo.BankingDisplayEntity?>{
        repos.bankingRepo.getTransaction(it).map { it?.data }.subscribeToColdFlow(viewModelScope, null)
    }
    fun getTransaction(key: BankingDisplayEntityKey) = _transactionFlowCache.get(key)

    val getSelf = repos.peopleRepo.meFlow
    @OptIn(ExperimentalCoroutinesApi::class)
    val lastTransactions = repos.bankingRepo.lastTransactionDay.flatMapLatest { date ->
        repos.bankingRepo.getSortedTransactionsAt(date)
    }.subscribeToColdFlow(viewModelScope, listOf())
    val inspectedTransaction = MutableStateFlow<BankingRepo.BankingDisplayEntity?>(null)
    init {
        viewModelScope.launch {
            // This might be really unnecessary, but is really clean
            // This could stop an edge case if today is not yet loaded and the day flips over
            // to a new day and then the day is not added so you need to restart the app
            repos.calendarRepo.todayFlow.collect { date ->
                onDayCachedFlows.get(date) // Precaching
            }
        }
    }
}