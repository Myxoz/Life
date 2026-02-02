package com.myxoz.life.aggregator

import android.content.Context
import com.myxoz.life.dbwrapper.DayScreenTimeEntity
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.repositories.utils.FlowCache
import com.myxoz.life.screens.options.getMappedUsageDataBetween
import com.myxoz.life.utils.atEndAsMillis
import com.myxoz.life.utils.atStartAsMillis
import com.myxoz.life.utils.diagrams.PieChart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import java.time.LocalDate
import java.time.ZoneId

class DaySummaryAggregator(
    private val repos: AppRepositories
) {
    private val zone = ZoneId.systemDefault()
    val pieChartCache = FlowCache<LocalDate, Map<String, PieChart.Companion.PieChartPart>>{ date ->
        repos.calendarRepo.eventsForDay(date).map { rawEvents ->
            val total = mutableMapOf<EventType, Long>()
            val startOfDay = date.atStartAsMillis(zone)
            val endOfDay = date.atEndAsMillis(zone)
            val events = rawEvents?.data ?: return@map mapOf()
            events.forEach {
                val duration = it.proposed.end.coerceAtMost(endOfDay) - it.proposed.start.coerceAtLeast(startOfDay)
                total[it.proposed.type] = total[it.proposed.type]?.plus(duration) ?: duration
            }
            total.mapValues {
                PieChart.Companion.PieChartPart(it.key.color, it.value.toDouble())
            }.mapKeys {
                it.key.calendarName
            }
        }
    }
    fun getDayPieChart(day: LocalDate) = pieChartCache.get(day)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val getLifeScreenTime = FlowCache<LocalDate, List<DayScreenTimeEntity>> { date ->
        repos.calendarRepo.todayFlow.flatMapLatest { today ->
            if(date > today) return@flatMapLatest MutableStateFlow(listOf())
            if(today == LocalDate.now()) {
                getScreenTimeOnAsFlow(date, repos.context)
            } else {
                MutableStateFlow(
                    getMappedUsageDataBetween(
                        repos.context,
                        date.atStartAsMillis(zone),
                        date.atEndAsMillis(zone)
                    )
                )
            }
        }
    }
    /**  Collect with lifecycle. Else this might emit unnecesarily */
    fun getScreenTimeOnDayLive(date: LocalDate) = getLifeScreenTime.get(date)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val getScreentimeForDayFlow = FlowCache<LocalDate, Long>{ date ->
        repos.daySummaryRepo.getDaySummary(date).flatMapLatest { summary ->
            if(summary?.data != null) return@flatMapLatest MutableStateFlow(summary.data.screenTimeMs.toLong())
            return@flatMapLatest getLifeScreenTime.get(date).map { entry -> entry.sumOf { it.duration } }
        }
    }
    fun getScreenTimeForDay(date: LocalDate) = getScreentimeForDayFlow.get(date)
    fun getScreenTimeOnAsFlow(
        date: LocalDate,
        context: Context,
    ): Flow<List<DayScreenTimeEntity>> {
        val baseFlow: Flow<List<DayScreenTimeEntity>> = flow {
            while (currentCoroutineContext().isActive) {
                val list = getMappedUsageDataBetween(
                    context,
                    date.atStartAsMillis(zone),
                    if(date == LocalDate.now()) System.currentTimeMillis() else date.atEndAsMillis(zone)
                ).sortedByDescending { it.duration }
                emit(list)
                delay(1000)
            }
        }

        return baseFlow
    }
}