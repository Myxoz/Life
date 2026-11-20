package com.myxoz.life.calendar.feed.topbar

import android.annotation.SuppressLint
import com.myxoz.life.lifecore.android.ScreenTimeCollector
import com.myxoz.life.lifecore.flows.StateFlowCache
import com.myxoz.life.lifecore.flows.subscribeToColdFlow
import com.myxoz.life.lifecore.settings.CompositionSettings
import com.myxoz.life.lifecore.settings.Feature
import com.myxoz.life.lifecore.time.updatingTodayFlow
import com.myxoz.life.storage.api.syncables.FullDaySyncable
import com.myxoz.life.storage.api.syncables.PersonSyncable
import com.myxoz.life.storage.interfaces.DaySummaryInterface
import com.myxoz.life.storage.interfaces.PeopleInterface
import com.myxoz.life.storage.interfaces.StepInterface
import com.myxoz.life.storage.interfaces.utils.Cached
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal class TopBarAggregator(
    val screenTimeCollector: ScreenTimeCollector,
    val viewModelScope: CoroutineScope,
    val settings: CompositionSettings,
    val peopleInterface: PeopleInterface,
    val stepInterface: StepInterface,
    val daySummaryInterface: DaySummaryInterface
) {
    private val birthDayAtCached = StateFlowCache<LocalDate, List<PersonSyncable>> { date ->
        peopleInterface.getPeopleWithBirthdayAt(date).map { it ?: listOf() }
            .subscribeToColdFlow(viewModelScope, listOf())
    }
    internal fun getPeopleWithBirthdayAt(date: LocalDate) = birthDayAtCached.get(date)

    private val daySummaryFlowCache = StateFlowCache<LocalDate, Cached<FullDaySyncable>?> {
        daySummaryInterface.getDaySummary(it).subscribeToColdFlow(viewModelScope, null)
    }
    internal fun getDaySummary(date: LocalDate) = daySummaryFlowCache.get(date)

    private val zone = ZoneId.systemDefault()
    @SuppressLint("MissingPermission")
    @OptIn(ExperimentalCoroutinesApi::class)
    private val screenTimeFlowCache = StateFlowCache<LocalDate, Duration?>{ date ->
        updatingTodayFlow().flatMapLatest { today ->
            if(date == today && settings.hasAssured(Feature.ScreenTime))
                screenTimeCollector.getTotalScreenTimeOnAsFlow(date, zone, 1.minutes)
            else if(date < today)
                getDaySummary(date).map { it?.value?.screenTimeMs }
            else
                flowOf(null)
        }.subscribeToColdFlow(viewModelScope, null)
    }
    fun getScreentime(date: LocalDate) = screenTimeFlowCache.get(date)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val stepsCache = StateFlowCache<LocalDate, Long?>{ date ->
        updatingTodayFlow().flatMapLatest { today ->
            if(date == today && settings.hasAssured(Feature.StepCounting))
                stepInterface.steps
            else if(date < today)
                getDaySummary(today).map { it?.value?.steps?.toLong() }
            else
                flowOf(null)
        }.subscribeToColdFlow(viewModelScope, null)
    }
    fun getSteps(date: LocalDate) = stepsCache.get(date)
}