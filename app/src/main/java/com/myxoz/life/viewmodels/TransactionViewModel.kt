package com.myxoz.life.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.repositories.BankingRepo
import com.myxoz.life.repositories.utils.FlowCache
import com.myxoz.life.repositories.utils.StateFlowCache
import com.myxoz.life.repositories.utils.subscribeToColdFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

class TransactionViewModel(private val repos: AppRepositories): ViewModel() {
    val lazyListState = LazyListState()
    private val _visibleDates =
        MutableStateFlow<List<LocalDate>>(emptyList())
    val visibleDates: StateFlow<List<LocalDate>> = _visibleDates

    init {
        val today = LocalDate.now()
        _visibleDates.value = listOf(today)
    }

    fun onLastVisibleIndexChanged(lastVisibleIndex: Int?) {
        if (lastVisibleIndex == null) return

        val currentDates = _visibleDates.value
        // same threshold logic you had
        if (lastVisibleIndex >= currentDates.size - 30 && currentDates.isNotEmpty()) {
            val lastDay = currentDates.last()
            val newDates = (1..30)
                .map { lastDay.minusDays(it.toLong()) }
                .filter { it !in currentDates }

            if (newDates.isEmpty()) return

            _visibleDates.value = currentDates + newDates
        }
    }
    val onDayCachedFlows = StateFlowCache<LocalDate, List<BankingRepo.BankingDisplayEntity>>{
        repos.bankingRepo.getTransactionsAt(it).map { it ?: listOf() }.subscribeToColdFlow(viewModelScope, listOf())
    }
    fun getOnDay(date: LocalDate) = onDayCachedFlows.get(date)
    val showBalance = MutableStateFlow(repos.prefs.getBoolean("show_balance", false))
    init {
        viewModelScope.launch {
            showBalance.collect {
                repos.prefs.edit {
                    putBoolean("show_balance", it)
                }
            }
        }
    }
    private val peopleWithIbanLikeCached = FlowCache<String, List<PersonSyncable>>{
        repos.peopleRepo.getPeopleWithIbanLike(it)
    }
    fun getPeopleWithIbanLike(iban: String) = peopleWithIbanLikeCached.get(iban)
    val getSelf = repos.peopleRepo.meFlow
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getLastTransactionDay() =
        repos.bankingRepo.earliestTransaction.flatMapLatest { date ->
            repos.bankingRepo.getTransactionsAt(date)
        }.subscribeToColdFlow(viewModelScope, listOf())
    val inspectedTransaction = MutableStateFlow<BankingRepo.BankingDisplayEntity?>(null)
}
