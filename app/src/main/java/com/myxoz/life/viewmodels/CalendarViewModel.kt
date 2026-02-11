package com.myxoz.life.viewmodels

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
import com.myxoz.life.api.syncables.FullDaySyncable
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.events.ProposedEvent
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.repositories.utils.StateFlowCache
import com.myxoz.life.repositories.utils.subscribeToColdFlow
import com.myxoz.life.screens.feed.dayoverview.getMonthByCalendarMonth
import com.myxoz.life.screens.feed.instantevents.InstantEvent
import com.myxoz.life.screens.feed.main.SegmentedEvent
import com.myxoz.life.screens.feed.search.SearchField
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

class CalendarViewModel(
    private val repos: AppRepositories
): ViewModel() {
    val days = MutableStateFlow(listOf<LocalDate>())
    val search = SearchField()
    @OptIn(ExperimentalCoroutinesApi::class)
    val yesterdaySummaryAdded = repos.calendarRepo.todayFlow.flatMapLatest {
        getDaySummary(it.minusDays(1))
    }
    val steps = repos.stepRepo.steps
    private val calendar: Calendar = Calendar.getInstance()
    val currentYear = MutableStateFlow(calendar.get(Calendar.YEAR))
    val currentMonth = MutableStateFlow(getMonthByCalendarMonth(calendar.get(Calendar.MONTH)))
    val lazyListState = LazyListState(0, 0)
    val snapFlingBehavior = snapFlingBehavior(
        SnapLayoutInfoProvider(lazyListState, SnapPosition.Start),
        exponentialDecay(3f),
        spring(stiffness = Spring.StiffnessMediumLow)
    )
    val lastInsertedSteps = repos.stepRepo.lastInsertedSteps
    val dayAmount = MutableStateFlow(repos.prefs.getInt("displayed_days", 2))
    val minuteFlow = flow {
        emit(System.currentTimeMillis())
        while (true){
            delay(60*1000L-System.currentTimeMillis()%(60*1000L))
            emit(System.currentTimeMillis())
        }
    }.subscribeToColdFlow(viewModelScope, System.currentTimeMillis())
    suspend fun resync() { repos.api.resync() }
    init {
        viewModelScope.launch {
            viewModelScope.launch {
                dayAmount.collect { repos.prefs.edit { putInt("displayed_days", it) } }
            }
            viewModelScope.launch {
                snapshotFlow { lazyListState.firstVisibleItemIndex }.collect {
                    val day = days.value.getOrNull(it) ?: return@collect
                    repos.prefs.edit { putLong("visible_date", day.toEpochDay()) }
                }
            }

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
    }

    private fun onDayScrolled(index: Int) {
        val current = days.value[index]
        fun insertDay(newDay: LocalDate, front: Boolean){
            if(!days.value.contains(newDay)) {
                if(front) {
                    days.value = listOf(newDay) + days.value
                } else {
                    days.value += newDay
                }
                preloadDay(newDay)
            }
        }
        repeat(10){
            insertDay(current.minusDays(it.toLong()+1), true)
        }
        repeat(10){
            insertDay(current.plusDays(it.toLong()+1), false)
        }
        currentMonth.value = getMonthByCalendarMonth(current.monthValue-1)
        currentYear.value = current.year
    }

    suspend fun removeProposedEvent(event: ProposedEvent) = repos.calendarRepo.removeProposedEvent(event)
    fun setDay(selectedDay: LocalDate) {
        days.value = listOf(selectedDay)
    }
    fun preloadDay(date: LocalDate){
        viewModelScope.launch {
            val flow = segmentedEventsCache.get(date)
            if (flow.value.isEmpty()) {
                flow.first() // We collect the first value to warm up the flow and allow instant
                // collecttion as soon as we scroll to it, precaching
            }
            repos.daySummaryRepo.prefetchDay(date)
        }
    }

    private val daySummaryFlowCache = StateFlowCache<LocalDate, FullDaySyncable?> {
        repos.daySummaryRepo.getDaySummary(it).map { it?.data }.subscribeToColdFlow(viewModelScope, null)
    }
    fun getDaySummary(date: LocalDate) = daySummaryFlowCache.get(date)
    private val birthDayAtCached = StateFlowCache<LocalDate, List<PersonSyncable>>{
        repos.peopleRepo.getPeopleWithBirthdayAt(it).map { it?:listOf() }.subscribeToColdFlow(viewModelScope, listOf())
    }
    fun getPeopleWithBirthdayAt(date: LocalDate) = birthDayAtCached.get(date)
    private val getProposedEventsAtCache = StateFlowCache<LocalDate, List<ProposedEvent>>{
        repos.calendarRepo.getProposedEventsAt(it).map { it?.data ?: listOf() }.subscribeToColdFlow(viewModelScope, listOf())
    }
    fun getProposedEventsAt(date: LocalDate) = getProposedEventsAtCache.get(date)
    fun saveProposedEvent(event: ProposedEvent) = repos.calendarRepo.saveProposedNotYetSyncedEvent(event)
    fun requestAutoDetectedEventStart(settings: Settings) = repos.calendarRepo.fetchAutoDetectEvents(settings)
    suspend fun testSign() = repos.api.testSign()
    fun getBase64Public() = repos.api.getBase64Public()
    fun requireAllPeople() = repos.peopleRepo.requireAllPeople()
    val segmentedEventsCache = StateFlowCache<LocalDate, List<SegmentedEvent>>{ date ->
        repos.aggregators.calendarAggregator.getSegmentedEvents(date).subscribeToColdFlow(viewModelScope, listOf())
    }
    fun getSegmentedEvents(date: LocalDate) = segmentedEventsCache.get(date)
    val instantEventsForDayCache = StateFlowCache<LocalDate, List<InstantEvent.InstantEventGroup>>{ date ->
        repos.aggregators.calendarAggregator.getInstantEventsForDay(date).subscribeToColdFlow(viewModelScope, listOf())
    }
    fun getInstantEventsForDay(date: LocalDate) = instantEventsForDayCache.get(date)
    fun getCachedLocation(id: Long) = repos.locationRepo.getCachedLocation(id)
    fun getCachedPeopleById(id: List<Long>) = repos.peopleRepo.getCachedPeopleById(id)
    fun getScreentime(date: LocalDate) = repos.aggregators.daySummaryAggregator.getScreenTimeForDay(date)
}