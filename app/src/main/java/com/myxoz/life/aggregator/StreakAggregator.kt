package com.myxoz.life.aggregator

import com.myxoz.life.api.extensions.StreakExtendable
import com.myxoz.life.api.syncables.ExtensionSyncable
import com.myxoz.life.api.syncables.FullDaySyncable
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.utils.Cached.Companion.cached
import com.myxoz.life.storage.interfaces.utils.subscribeToColdFlow
import com.myxoz.life.utils.UnixWeek
import com.myxoz.life.utils.atEndAsMillis
import com.myxoz.life.utils.atStartAsMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

typealias EventFlow = StateFlow<List<SyncedEvent>?>
typealias FullDayFlow = StateFlow<FullDaySyncable?>
typealias FullDayFlows = StateFlow<List<FullDaySyncable>?>

class StreakAggregator(
    private val repos: DatabaseInterface
) {
    private val zone = ZoneId.systemDefault()
    private val dayedEventFlows = ConcurrentHashMap<LocalDate, EventFlow>()
    private val weekEventFlows = ConcurrentHashMap<UnixWeek, EventFlow>()
    private val dayedFullDayFlows = ConcurrentHashMap<LocalDate, FullDayFlow>()
    private val weekFullDayFlows = ConcurrentHashMap<UnixWeek, FullDayFlows>()
    val streaks = repos.extensionInterface
        .flowFor(ExtensionSyncable.ExtensionSyncableType.Streak)
        .map {
            it?.streaks?.associate { item -> item.id to item.cached } ?: emptyMap()
        }
        .subscribeToColdFlow(repos.appScope, emptyMap())
    fun getStreak(id: Long) = streaks.map { it[id] }
    fun getDayedFullDayFlow(day: LocalDate): FullDayFlow {
        return dayedFullDayFlows.computeIfAbsent(day) {
            repos.daySummaryInterface
                .getDaySummary(it)
                .map{ summary -> summary?.value }
                .subscribeToColdFlow(repos.appScope, null)
        }
    }
    fun getWeekFullDayFlow(week: UnixWeek): FullDayFlows {
        return weekFullDayFlows.computeIfAbsent(week) { week ->
            repos.daySummaryInterface
                .getSummaryWeek(week)
                .map{ summaries -> summaries.mapNotNull { it.value } }
                .subscribeToColdFlow(repos.appScope, null)
        }
    }
    fun getDayedEventFlow(day: LocalDate): EventFlow {
        return dayedEventFlows.computeIfAbsent(day) {
            repos.calendarInterface.eventsForDay(it).subscribeToColdFlow(repos.appScope, null)
        }
    }
    fun getWeekEventFlow(week: UnixWeek): EventFlow {
        return weekEventFlows.computeIfAbsent(week) {
            repos.calendarInterface.eventsForWeek(it).subscribeToColdFlow(repos.appScope, null)
        }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getStreakValueAtTimestamp(streak: StreakExtendable.StreakItem, timespan: StreakExtendable.Timespan, timestamp: Long): Flow<Long> {
        val maxValueFlow = getMaxValue(streak, timespan)
        fun Long.updateMaxValue(): Long {
            maxValueFlow.update { max(it, this) }
            return this
        }
        return when(timespan) {
            StreakExtendable.Timespan.Day -> {
                val date = LocalDate.ofEpochDay(timestamp)
                when(streak.target) {
                    is StreakExtendable.StreakTarget.EventTarget -> {
                        getDayedEventFlow(date)
                            .mapNotNull { eventList ->
                                if(eventList == null) return@mapNotNull null
                                streak.target.currentStreakValue(eventList, streak.atLeast, date.atStartAsMillis(zone), date.atEndAsMillis(zone))
                                    .updateMaxValue()
                            }
                    }
                    is StreakExtendable.StreakTarget.DayTarget -> {
                        getDayedFullDayFlow(date)
                            .mapNotNull { daySummary ->
                                if(daySummary == null) return@mapNotNull null
                                streak.target.currentStreakValue(listOf(daySummary))
                                    .updateMaxValue()
                            }
                    }
                }
            }
            StreakExtendable.Timespan.Week -> {
                val date = UnixWeek(timestamp)
                when(streak.target) {
                    is StreakExtendable.StreakTarget.EventTarget -> {
                        getWeekEventFlow(date)
                            .mapNotNull { eventList ->
                                if(eventList == null) return@mapNotNull null
                                streak.target.currentStreakValue(eventList, streak.atLeast, date.start.atStartAsMillis(zone), date.end.atEndAsMillis(zone))
                                    .updateMaxValue()
                            }
                    }
                    is StreakExtendable.StreakTarget.DayTarget -> {
                        getWeekFullDayFlow(date)
                            .mapNotNull { daySummaries ->
                                if(daySummaries == null) return@mapNotNull null
                                streak.target.currentStreakValue(daySummaries)
                                    .updateMaxValue()
                            }
                    }
                }
            }
        }
    }

    private val streakValueTimespanCache = ConcurrentHashMap<Long, ConcurrentHashMap<StreakExtendable.Timespan, ConcurrentHashMap<Long, StateFlow<Long?>>>>()
    private val streakCache = ConcurrentHashMap<Long, ConcurrentHashMap<Long, StateFlow<Int?>>>()
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCurrentStreak(streak: StreakExtendable.StreakItem, timestamp: Long): StateFlow<Int?> {
        return streakCache
            .computeIfAbsent(streak.id) { ConcurrentHashMap() }
            .computeIfAbsent(timestamp) {
                getCurrentStreakValue(streak, streak.timespan, timestamp)
                    .mapNotNull {
                        streak.target.continuesStreak(it ?: return@mapNotNull null, streak.threshold, streak.atLeast)
                    }
                    .flatMapLatest {
                        if(it) getCurrentStreak(streak, timestamp - 1).mapNotNull { it?.plus(1) } else MutableStateFlow(0)
                    }
                    .subscribeToColdFlow(repos.appScope, 0)

            }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCurrentStreakValue(streak: StreakExtendable.StreakItem, timespan: StreakExtendable.Timespan, timestamp: Long): StateFlow<Long?> {
        return streakValueTimespanCache
            .computeIfAbsent(streak.id) { ConcurrentHashMap() }
            .computeIfAbsent(timespan) { ConcurrentHashMap() }
            .computeIfAbsent(timestamp) {
                getStreakValueAtTimestamp(streak, timespan, timestamp).subscribeToColdFlow(repos.appScope, null)
            }
    }
    val maxValueCache = ConcurrentHashMap<Long, ConcurrentHashMap<StreakExtendable.Timespan, MutableStateFlow<Long>>>()
    fun getMaxValue(streak: StreakExtendable.StreakItem, timespan: StreakExtendable.Timespan) =
        maxValueCache
            .computeIfAbsent(streak.id) { ConcurrentHashMap() }
            .computeIfAbsent(timespan) {
                MutableStateFlow(0)
            }
    fun invalidateCacheFor(streak: StreakExtendable.StreakItem) {
        streakValueTimespanCache.remove(streak.id)
        streakCache.remove(streak.id)
        maxValueCache.remove(streak.id)
    }
}