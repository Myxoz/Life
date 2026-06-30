package com.myxoz.life.ui.feed.dayoverview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.api.syncables.FullDaySyncable
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.utils.subscribeToColdFlow
import com.myxoz.life.ui.NavPath
import com.myxoz.life.utils.getDate
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class DayOverviewViewModel(
    private val dbInterface: DatabaseInterface,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    val date = savedStateHandle.getDate(NavPath.DAY_OVERVIEW)
    val birthdays = dbInterface.peopleInterface
        .getPeopleWithBirthdayAt(date)
        .map { it ?: listOf() }
        .subscribeToColdFlow(viewModelScope, listOf())

    val daySummary = dbInterface.daySummaryInterface
        .getDaySummary(date)
        .subscribeToColdFlow(viewModelScope, null)

    val transactions = dbInterface.bankingRepo
        .getSortedTransactionsAt(date)
        .subscribeToColdFlow(viewModelScope, listOf())

    val pieChart = dbInterface.aggregators.daySummaryAggregator
        .getDayPieChart(date)
        .subscribeToColdFlow(viewModelScope, mapOf())

    val getAllSteps = dbInterface.stepInterface.steps
    fun getStepsFor(it: LocalDate) = dbInterface.stepInterface.getStepsCached(it)

    val liveScreenTime = dbInterface.aggregators.daySummaryAggregator.getScreenTimeOnDayLive(date)
    val screentimeFlow = dbInterface.aggregators.daySummaryAggregator.getScreenTimeForDay(date)
    // Used for Liffy TODO Consider wether needed
    fun getLastNDaysAsNonFlow(days: Int) = dbInterface.daySummaryInterface.getLastNDaysAsNonFlow(days)

    suspend fun setAndStageDaySummary(fullDayEvent: FullDaySyncable) = dbInterface.daySummaryInterface.setAndStageDaySummary(fullDayEvent)
    val birthdayProfilePics = birthdays.map { it.map { person ->
        person to dbInterface.aggregators.peopleAggregator.getProfilePicture(person.id)
    } }.subscribeToColdFlow(viewModelScope, listOf())
}