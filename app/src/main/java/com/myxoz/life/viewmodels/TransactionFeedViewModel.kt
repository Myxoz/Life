package com.myxoz.life.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.dbwrapper.BankingEntity
import com.myxoz.life.dbwrapper.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class ScrollPosition(
    val index: Int = 0,
    val offset: Int = 0,
)

class TransactionFeedViewModel(
    private val db: StorageManager,
    private val zone: ZoneId
): ViewModel() {
    private val _transactionsByDate =
        MutableStateFlow<Map<LocalDate, List<BankingEntity>>>(emptyMap())
    val transactionsByDate: StateFlow<Map<LocalDate, List<BankingEntity>>> =
        _transactionsByDate

    private val _visibleDates =
        MutableStateFlow<List<LocalDate>>(emptyList())
    val visibleDates: StateFlow<List<LocalDate>> = _visibleDates

    private val _scrollPosition =
        MutableStateFlow(ScrollPosition())
    val scrollPosition: StateFlow<ScrollPosition> = _scrollPosition

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

    fun updateScrollPosition(state: LazyListState) {
        _scrollPosition.value = ScrollPosition(
            index = state.firstVisibleItemIndex,
            offset = state.firstVisibleItemScrollOffset,
        )
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
): List<BankingEntity> {
    val start = date.atStartOfDay(zone).toEpochSecond() * 1000L
    val end = date.plusDays(1).atStartOfDay(zone).toEpochSecond() * 1000L
    val transactions = db.banking.getFullDayTransactions(start, end)
    return transactions
}
