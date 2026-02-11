package com.myxoz.life.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.api.syncables.FullDaySyncable
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.repositories.BankingRepo
import com.myxoz.life.repositories.utils.StateFlowCache
import com.myxoz.life.repositories.utils.subscribeToColdFlow
import com.myxoz.life.utils.diagrams.PieChart
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

class DayOverviewViewModel(
    private val repos: AppRepositories
): ViewModel() {
    private val zone: ZoneId = ZoneId.systemDefault()

    private val getAllBirthdaysAtCached = StateFlowCache<LocalDate, List<PersonSyncable>> { date ->
        repos.peopleRepo.getPeopleWithBirthdayAt(date).map { it ?: listOf() }.subscribeToColdFlow(viewModelScope, listOf())
    }
    fun getAllBirthdaysAt(date: LocalDate) = getAllBirthdaysAtCached.get(date)

    private val getDaySummaryCached = StateFlowCache<LocalDate, FullDaySyncable?>{
        date -> repos.daySummaryRepo.getDaySummary(date).map { it?.data }.subscribeToColdFlow(viewModelScope, null)
    }
    fun getDaySummary(date: LocalDate) = getDaySummaryCached.get(date)

    private val getAllTransactionsCached = StateFlowCache<LocalDate, List<BankingRepo.BankingDisplayEntity>>{ date ->
        repos.bankingRepo.getTransactionsAt(date).map { it ?: listOf() }.subscribeToColdFlow(viewModelScope, listOf())
    }
    fun getAllTransactions(date: LocalDate) = getAllTransactionsCached.get(date)
    val pieChartCache = StateFlowCache<LocalDate, Map<String, PieChart.Companion.PieChartPart>>{ date ->
        repos.aggregators.daySummaryAggregator.getDayPieChart(date).subscribeToColdFlow(viewModelScope, mapOf())
    }
    fun getPieChart(date: LocalDate) = pieChartCache.get(date)
    val getAllSteps = repos.stepRepo.steps
    val lastInsertedSteps = repos.stepRepo.lastInsertedSteps

    fun getScreentimeLive(date: LocalDate) = repos.aggregators.daySummaryAggregator.getScreenTimeOnDayLive(date)
    fun getScreentime(date: LocalDate) = repos.aggregators.daySummaryAggregator.getScreenTimeForDay(date)
    suspend fun getLastNDaysAsNonFlow(days: Int) = repos.daySummaryRepo.getLastNDaysAsNonFlow(days)

    suspend fun setAndStageDaySummary(fullDayEvent: FullDaySyncable) = repos.daySummaryRepo.setAndStageDaySummary(fullDayEvent)
    private val profilePictureCache = StateFlowCache<Long, Bitmap?>{ personId ->
        repos.aggregators.peopleAggregator.getProfilePicture(personId).subscribeToColdFlow(viewModelScope, null)
    }
    fun getProfilePicture(personId: Long) = profilePictureCache.get(personId)
}