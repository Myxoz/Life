package com.myxoz.life.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.dbwrapper.BankingEntity
import com.myxoz.life.dbwrapper.BankingSidecarEntity
import com.myxoz.life.dbwrapper.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class TransactionFeedViewModel(
    private val db: StorageManager,
    private val zone: ZoneId
): ViewModel() {
    val lazyListState = LazyListState()
    private val _transactionsByDate =
        MutableStateFlow<Map<LocalDate, List<Pair<BankingEntity, BankingSidecarEntity?>>>>(emptyMap())
    val transactionsByDate: StateFlow<Map<LocalDate, List<Pair<BankingEntity, BankingSidecarEntity?>>>> =
        _transactionsByDate

    private val _visibleDates =
        MutableStateFlow<List<LocalDate>>(emptyList())
    val visibleDates: StateFlow<List<LocalDate>> = _visibleDates

    init {
        // initial load
        val today = LocalDate.now(zone)
        _visibleDates.value = listOf(today)

        viewModelScope.launch {
            loadDateIfNeeded(today)
        }
    }

    fun onLastVisibleIndexChanged(lastVisibleIndex: Int?) {
        if (lastVisibleIndex == null) return

        val currentDates = _visibleDates.value
        // same threshold logic you had
        if (lastVisibleIndex >= currentDates.size - 10 && currentDates.isNotEmpty()) {
            val lastDay = currentDates.last()
            val newDates = (1..20)
                .map { lastDay.minusDays(it.toLong()) }
                .filter { it !in currentDates }

            if (newDates.isEmpty()) return

            _visibleDates.value = currentDates + newDates

            // batch load in background
            viewModelScope.launch {
                newDates.forEach { date ->
                    loadDateIfNeeded(date)
                }
            }
        }
    }

    private suspend fun loadDateIfNeeded(date: LocalDate) {
        // already loaded
        if (_transactionsByDate.value.containsKey(date)) return

        val list = loadTransactionsForDate(db, date, zone) // make this return the list
        _transactionsByDate.update { old ->
            // cache results; new map instance
            old + (date to list)
        }
    }
}

private suspend fun loadTransactionsForDate(
    db: StorageManager,
    date: LocalDate,
    zone: ZoneId
): List<Pair<BankingEntity, BankingSidecarEntity?>> {
    val start = date.atStartOfDay(zone).toEpochSecond() * 1000L
    val end = date.plusDays(1).atStartOfDay(zone).toEpochSecond() * 1000L
    val sidecars = db.bankingSidecar.getSidecarsBetween(start, end)
    val transactions = db.banking.getCombinedTransactions(start, end)
    return transactions.map { it to sidecars.find { c -> c.transactionId == it.id } }.sortedByDescending { it.second?.date ?: it.first.purposeDate ?: it.first.valueDate }
}
