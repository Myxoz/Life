package com.myxoz.life.ui.feed.main

import android.icu.util.Calendar
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.utils.subscribeToColdFlow
import com.myxoz.life.ui.feed.dayoverview.getMonthByCalendarMonth
import com.myxoz.life.utils.datesThrough
import com.myxoz.life.utils.syncToPrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class CalendarViewModel(
    val repos: DatabaseInterface
): ViewModel() {
    val isSelectDayVisible = MutableStateFlow(false)
    val minuteFlow = flow {
        emit(System.currentTimeMillis())
        while (true){
            delay(60*1000L-System.currentTimeMillis()%(60*1000L))
            emit(System.currentTimeMillis())
        }
    }.subscribeToColdFlow(repos.appScope, System.currentTimeMillis())
    private val calendar: Calendar = Calendar.getInstance()
    val currentYear = MutableStateFlow(calendar.get(Calendar.YEAR))
    val currentMonth = MutableStateFlow(getMonthByCalendarMonth(calendar.get(Calendar.MONTH)))
    val days = MutableStateFlow(listOf<LocalDate>())
    val dayAmount = MutableStateFlow(repos.prefs.getInt("displayed_days", 2)).apply {
        syncToPrefs(repos.appScope, repos.prefs, "displayed_days", Int::class)
    }
    val lazyListState = LazyListState(0, 0)
    val snapFlingBehavior = snapFlingBehavior(
        SnapLayoutInfoProvider(lazyListState, SnapPosition.Start),
        exponentialDecay(3f),
        spring(stiffness = Spring.StiffnessMediumLow)
    )
    init {
        viewModelScope.launch {
            snapshotFlow { lazyListState.firstVisibleItemIndex }.collect {
                val day = days.value.getOrNull(it) ?: return@collect
                repos.prefs.edit { putLong("visible_date", day.toEpochDay()) }
            }
        }
        viewModelScope.launch {
            // Init and scroll logic
            val date = LocalDate.ofEpochDay(
                repos.prefs.getLong("visible_date", LocalDate.now().toEpochDay())
            )
            days.value += date
            onDayScrolled(0)
            // Snap correct item to viewport
            lazyListState.scrollToItem(days.value.indexOfFirst { it == date })
            snapshotFlow { lazyListState.firstVisibleItemIndex }
                .collect { index ->
                    onDayScrolled(index)
                }
        }
        viewModelScope.launch {
            repos.calendarInterface.todayFlow.collect {
                repos.stepInterface.insertYesterdayIfNeeded(it)
            }
        }
    }

    private suspend fun onDayScrolled(index: Int) {
        val current = days.value[index]
        val startOfRange = current.minusDays(dayAmount.value * 3L)
        val endOfRange = current.plusDays(dayAmount.value * 3L)
        val between = startOfRange.datesThrough(endOfRange)

        days.update { days ->
            (days + between).distinct().sorted()
        }

        val snap = days.value
        var earliestMissing: LocalDate? = null
        var lastWasMissing = false
        suspend fun prefetch(from: LocalDate, to: LocalDate){
            repos.calendarInterface.prefetchDays(from, to)
            repos.bankingRepo.prepareBeween(from, to)
            repos.daySummaryInterface.prefetchDay(from, to)
        }

        for (day in between) {
            if (day !in snap) {
                if (!lastWasMissing) {
                    earliestMissing = day
                }
                lastWasMissing = true
            } else {
                if (lastWasMissing && earliestMissing != null) {
                    prefetch(earliestMissing, day.minusDays(1))
                    earliestMissing = null
                }
                lastWasMissing = false
            }
        }

        if (lastWasMissing && earliestMissing != null) {
            repos.calendarInterface.prefetchDays(earliestMissing, endOfRange)
        }
        currentMonth.value = getMonthByCalendarMonth(current.monthValue-1)
        currentYear.value = current.year
    }
    fun setDay(selectedDay: LocalDate) {
        days.value = listOf(selectedDay)
    }
}