package com.myxoz.life.calendar.feed

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.calendar.feed.events.prerendered.PrerenderedCalendarContent
import com.myxoz.life.calendar.feed.events.prerendered.PrerenderedCalendarContent.Companion.getPrerenderedCalendarContent
import com.myxoz.life.calendar.feed.instantevents.INSTANTEVENTSIZE
import com.myxoz.life.calendar.feed.instantevents.InstantEventAggregator
import com.myxoz.life.calendar.feed.topbar.TopBarAggregator
import com.myxoz.life.core.Rangable.Companion.rangableTo
import com.myxoz.life.core.def
import com.myxoz.life.lifecore.android.ScreenTimeCollector
import com.myxoz.life.lifecore.flows.StateFlowCache
import com.myxoz.life.lifecore.flows.subscribeToColdFlow
import com.myxoz.life.lifecore.settings.CompositionSettings
import com.myxoz.life.lifecore.settings.Permission
import com.myxoz.life.lifecore.time.updatingMinuteFlow
import com.myxoz.life.lifecore.time.updatingTodayFlow
import com.myxoz.life.presentation.time.getShortMonthByCalendarMonth
import com.myxoz.life.storage.AutoDetectLocalEvent
import com.myxoz.life.storage.api.syncables.ExtensionSyncable
import com.myxoz.life.storage.api.syncables.SyncedEvent
import com.myxoz.life.storage.dbwrapper.state.KeyValueStore
import com.myxoz.life.storage.events.RawEvent
import com.myxoz.life.storage.interfaces.BankingInterface
import com.myxoz.life.storage.interfaces.CalendarInterface
import com.myxoz.life.storage.interfaces.CommitsInterface
import com.myxoz.life.storage.interfaces.DaySummaryInterface
import com.myxoz.life.storage.interfaces.ExtensionInterface
import com.myxoz.life.storage.interfaces.ExtensionInterface.Companion.flowFor
import com.myxoz.life.storage.interfaces.LocationInterface
import com.myxoz.life.storage.interfaces.PeopleInterface
import com.myxoz.life.storage.interfaces.StepInterface
import com.myxoz.life.storage.interfaces.TodoInterface
import com.myxoz.life.storage.stored.CalendarStored
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.hours

abstract class CalendarFeedApplicationState(
    val store: KeyValueStore,
    val screenTimeCollector: ScreenTimeCollector,
    val settings: CompositionSettings,
    val calendarInterface: CalendarInterface,
    val bankingInterface: BankingInterface,
    val daySummaryInterface: DaySummaryInterface,
    val stepInterface: StepInterface,
    val extensionInterface: ExtensionInterface,
    val peopleInterface: PeopleInterface,
    val locationInterface: LocationInterface,
    val commitsInterface: CommitsInterface,
    val todoInterface: TodoInterface,
    val settingsChecker: Permission.SettingsChecker
): ViewModel() {
    private val instantEventAggregator = InstantEventAggregator(
        calendarInterface,
        commitsInterface,
        bankingInterface,
        todoInterface
    )
    internal val topBarAggregator = TopBarAggregator(
        screenTimeCollector,
        viewModelScope,
        settings,
        peopleInterface,
        stepInterface,
        daySummaryInterface
    )
    private var isInitialized = false
    internal val isSelectDayVisible = MutableStateFlow(false)
    internal val minuteFlow = updatingMinuteFlow()
    private val todayFlow = updatingTodayFlow()
    private var visibleDate by store.getAsState(CalendarStored.VisibleDate.key, LocalDate.now())
    private val currentCalendarDisplayDate = MutableStateFlow(visibleDate)
    internal val currentYear = currentCalendarDisplayDate.map { it.year }
        .subscribeToColdFlow(viewModelScope, visibleDate.year)
    internal val currentShortMonth = currentCalendarDisplayDate.map { getShortMonthByCalendarMonth(it.month) }
        .subscribeToColdFlow(viewModelScope, getShortMonthByCalendarMonth(visibleDate.month))
    internal val days = MutableStateFlow(listOf(visibleDate))
    internal val dayAmount = store.getAsWritableMutableState(CalendarStored.DisplayedDays.key, 2)
    internal val lazyListState = LazyListState(0, 0)
    internal val snapFlingBehavior = snapFlingBehavior(
        SnapLayoutInfoProvider(lazyListState, SnapPosition.Start),
        exponentialDecay(3f),
        spring(stiffness = Spring.StiffnessMediumLow)
    )

    private suspend fun onDayScrolled(index: Int) {
        val current = days.value.getOrNull(index) ?: return
        visibleDate = current
        currentCalendarDisplayDate.value = current
        val startOfRange = current.minusDays(dayAmount.value * 3L)
        val endOfRange = current.plusDays(dayAmount.value * 3L)
        val between = startOfRange.rangableTo(endOfRange).between

        days.update { days ->
            (days + between).distinct().sorted()
        }

        val snap = days.value
        var earliestMissing: LocalDate? = null
        var lastWasMissing = false
        suspend fun prefetch(from: LocalDate, to: LocalDate){
            calendarInterface.prefetchDays(from, to)
            bankingInterface.prepareBeween(from, to)
            daySummaryInterface.prefetchDay(from, to)
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
            calendarInterface.prefetchDays(earliestMissing, endOfRange)
        }
    }
    internal fun setDay(selectedDay: LocalDate) {
        days.value = listOf(selectedDay)
    }
    @Composable
    internal fun ObserveLazyListState() {
        LaunchedEffect(this) {
            if (!isInitialized) {
                onDayScrolled(0)
                isInitialized = true
            }
            launch {
                snapshotFlow { lazyListState.firstVisibleItemIndex }
                    .collect { index ->
                        onDayScrolled(index)
                    }
            }
            launch {
                todayFlow.collect {
                    stepInterface.insertYesterdayIfNeeded(it)
                }
            }
        }
    }

    // Day Summary
    @OptIn(ExperimentalCoroutinesApi::class)
    internal val yesterdaySummary = todayFlow.flatMapConcat {
        topBarAggregator.getDaySummary(it.minusDays(1))
    }

    // Steps
    // LocalEvents
    internal suspend fun removeLocalEvent(event: LocalEventDisplayable) {
        when(event) {
            is LocalEventDisplayable.AutoDetectLocalEventDisplayable -> {
                calendarInterface.removeAutoDetectLocalEvent(event.autoDetectable.localId)
            }
            is LocalEventDisplayable.RepeatingEventDisplayable -> {
                extensionInterface.moveRepeatingLocalEvent(event.repeatingId)
            }
        }
    }
    internal suspend fun acceptLocalEvent(event: LocalEventDisplayable) {
        when(event) {
            is LocalEventDisplayable.AutoDetectLocalEventDisplayable -> {
                calendarInterface.removeAutoDetectLocalEvent(event.autoDetectable.localId)
                calendarInterface.createSyncedEvent(event.event)
            }
            is LocalEventDisplayable.RepeatingEventDisplayable -> {
                val newEvent = calendarInterface.createSyncedEvent(event.event)
                extensionInterface.addNewEventIdAndMoveRepeatingEvent(event.repeatingId, newEvent.id)
            }
        }
    }
    private val getAutoDetectLocalEventsAtCache = StateFlowCache<LocalDate, List<AutoDetectLocalEvent>?> {
        calendarInterface.getAutoDetectEventAt(it).subscribeToColdFlow(viewModelScope, listOf())
    }
    internal fun getAutoDetectEventsAt(date: LocalDate) = getAutoDetectLocalEventsAtCache.get(date)

    internal suspend fun updateSyncedEvent(event: SyncedEvent, mergeWith: RawEvent) = calendarInterface.updateSyncedEvent(event, mergeWith)
    internal suspend fun createSyncedEvent(rawEvent: RawEvent) = calendarInterface.createSyncedEvent(rawEvent)

    abstract fun testSign(): String?
    abstract fun getBase64Public(): String
    private val repeatingEvents = extensionInterface.flowFor(ExtensionSyncable.ExtensionSyncableType.RepeatingEvents)
    private val zone = ZoneId.systemDefault()
    private val repeatingEventsDayed =
        StateFlowCache<LocalDate, List<LocalEventDisplayable.RepeatingEventDisplayable>?>{ date ->
            val dayStart = date.atStartOfDay(zone).toInstant()
            val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()
            repeatingEvents.mapNotNull { extendable ->
                (extendable?.events ?: return@mapNotNull null)
                    .filter {
                        it.event.eventTime.overlaps(dayStart, dayEnd)
                    }
                    .map { repeatingEvent ->
                        LocalEventDisplayable.RepeatingEventDisplayable(
                            repeatingEvent.event,
                            repeatingEvent.id
                        )
                    }
            }.subscribeToColdFlow(viewModelScope, null)
        }

    private fun getPrerenderedEvents(date: LocalDate) = combine(
        calendarInterface.eventsForDay(date).filterNotNull(),
        instantEventAggregator.getInstantEventsForDay(date),
    ) { events, instantEvents ->
        getPrerenderedCalendarContent(
            events.def(listOf()),
            instantEvents,
            INSTANTEVENTSIZE.hours
        )
    }
    private val prerenderedEventCache = StateFlowCache<LocalDate, PrerenderedCalendarContent?> { date ->
        getPrerenderedEvents(date).subscribeToColdFlow(viewModelScope, null)
    }
    internal fun getPrerenderedContent(date: LocalDate) = prerenderedEventCache.get(date)
    fun getCachedLocation(id: Long) = locationInterface.getCachedLocation(id)
    fun getCachedPeopleById(id: List<Long>) = peopleInterface.getCachedPeopleById(id)
    fun refetchAlarmClockTs() = calendarInterface.refetchAlarmClockTs()
    sealed class LocalEventDisplayable(val event: RawEvent) {
        class AutoDetectLocalEventDisplayable(val autoDetectable: AutoDetectLocalEvent): LocalEventDisplayable(autoDetectable.raw)
        class RepeatingEventDisplayable(
            raw: RawEvent,
            val repeatingId: Long
        ): LocalEventDisplayable(raw)
    }
}