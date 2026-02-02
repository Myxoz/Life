package com.myxoz.life.viewmodels

import androidx.lifecycle.ViewModel
import com.myxoz.life.api.syncables.FullDaySyncable
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.repositories.BankingRepo
import com.myxoz.life.repositories.utils.FlowCache
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

class DayOverviewViewModel(
    private val repos: AppRepositories
): ViewModel() {
    private val zone: ZoneId = ZoneId.systemDefault()

    private val getAllBirthdaysAtCached = FlowCache<LocalDate, List<PersonSyncable>>{ date -> repos.peopleRepo.getPeopleWithBirthdayAt(date).map { it ?: listOf() } }
    fun getAllBirthdaysAt(date: LocalDate) = getAllBirthdaysAtCached.get(date)

    private val getDaySummaryCached = FlowCache<LocalDate, FullDaySyncable?>{ date -> repos.daySummaryRepo.getDaySummary(date).map { it?.data } }
    fun getDaySummary(date: LocalDate) = getDaySummaryCached.get(date)

    private val getAllTransactionsCached = FlowCache<LocalDate, List<BankingRepo.BankingDisplayEntity>>{ date ->
        repos.bankingRepo.getTransactionsAt(date).map { it ?: listOf() }
    }
    fun getAllTransactions(date: LocalDate) = getAllTransactionsCached.get(date)
    fun getPieChart(date: LocalDate) = repos.aggregators.daySummaryAggregator.getDayPieChart(date)
    val getAllSteps = repos.stepRepo.steps
    val lastInsertedSteps = repos.stepRepo.lastInsertedSteps

    fun getScreentimeLive(date: LocalDate) = repos.aggregators.daySummaryAggregator.getScreenTimeOnDayLive(date)
    fun getScreentime(date: LocalDate) = repos.aggregators.daySummaryAggregator.getScreenTimeForDay(date)
    suspend fun getLastNDaysAsNonFlow(days: Int) = repos.daySummaryRepo.getLastNDaysAsNonFlow(days)

    suspend fun setAndStageDaySummary(fullDayEvent: FullDaySyncable) = repos.daySummaryRepo.setAndStageDaySummary(fullDayEvent)
    fun getProfilePicture(personId: Long) = repos.aggregators.peopleAggregator.getProfilePicture(personId)
}