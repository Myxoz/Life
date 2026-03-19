package com.myxoz.life.aggregator

import android.app.AlarmManager
import com.myxoz.life.R
import com.myxoz.life.api.syncables.CommitSyncable
import com.myxoz.life.api.syncables.ManualTransactionSyncable
import com.myxoz.life.dbwrapper.banking.formatCents
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.repositories.BankingRepo
import com.myxoz.life.screens.feed.instantevents.InstantEvent
import com.myxoz.life.screens.feed.instantevents.InstantEvent.Companion.INSTANTEVENTSIZE
import com.myxoz.life.screens.feed.main.PrerenderedEvent
import com.myxoz.life.utils.AndroidUtils.sendWithBal
import com.myxoz.life.utils.def
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class CalendarAggregator(
    private val repos: AppRepositories
) {
    fun getInstantEventsForDay(date: LocalDate) = combine(
        repos.calendarRepo.nextAlarmClockTs,
        repos.commitsRepo.getCommitsForDay(date),
        repos.bankingRepo.getSortedTransactionsAt(date),
        repos.todoRepo.getTodosForDay(date)
    ) { nextAlarm, commits, transactions, todos ->
        val nextAlarmEvent = listOfNotNull(
            nextAlarm?.let { alarmToInstantEvent(it) }
        )
        val commits = commits?.data.def(listOf()).mapNotNull {
            commitToInstantEvent(it)
        }
        val transactions = transactions.def(listOf()).mapNotNull {
            bankEntryAsInstantEvent(it)
        }
        val todos = todos?.data.def(listOf()).map {
            it.asInstantEvent()
        }
        createGroupedInstantEvents(commits + transactions + nextAlarmEvent + todos)
    }
    private fun createGroupedInstantEvents(mix: List<InstantEvent>): List<InstantEvent.InstantEventGroup>{
        val mix = mix.sortedBy { it.timestamp }
        val groups = mutableListOf<MutableList<InstantEvent>>(mutableListOf())
        var lastTs = mix.getOrNull(0)?.timestamp ?: return listOf()
        for (event in mix) {
            if(event.timestamp - lastTs > INSTANTEVENTSIZE * (3600 * 1000L)) {
                groups.add(mutableListOf())
            }
            groups.last().add(event)
            lastTs = event.timestamp
        }
        return groups.map { InstantEvent.InstantEventGroup(it) }
    }
    fun getPrerenderedEvents(it: LocalDate) = combine(
        repos.calendarRepo.eventsForDay(it),
        getInstantEventsForDay(it),
    ) { events, instantEvents ->
        PrerenderedEvent.getPrerenderedEvents(
            events?.data.def(listOf()),
            instantEvents,
            (INSTANTEVENTSIZE * 3600L).toLong() * 1000L
        )
    }
    companion object {
        private fun asInstantEvent(entity: BankingRepo.BankingDisplayEntity): InstantEvent{
            return InstantEvent(
                entity.icon,
                entity.amount.formatCents(),
                entity.timestamp,
                { it is ManualTransactionSyncable && it.id == entity.getStoredManualTransactionSyncable()?.id }
            ) { screens ->
                screens.openTransaction(entity)
            }
        }
        fun bankEntryAsInstantEvent(entity: BankingRepo.BankingDisplayEntity): InstantEvent?{
            return if(!entity.isTransaction) asInstantEvent(entity) else null
        }
        fun manualTransactionEntityAsInstantEvent(manualTransactionSyncable: ManualTransactionSyncable): InstantEvent {
            return asInstantEvent(BankingRepo.BankingDisplayEntity.from(manualTransactionSyncable))
        }
        private fun commitToInstantEvent(commitSyncable: CommitSyncable): InstantEvent? {
            return InstantEvent(
                R.drawable.commit,
                commitSyncable.repoName,
                commitSyncable.commitDate ?: return null,
                { it is CommitSyncable && it.commitSha == commitSyncable.commitSha }
            ) { screens ->
                screens.openCommit(commitSyncable.commitSha)
            }
        }
        private fun alarmToInstantEvent(alarm: AlarmManager.AlarmClockInfo) =
            InstantEvent(
                R.drawable.alarm_clock,
                "Wecker",
                alarm.triggerTime,
                {false},
            ) {
                alarm.showIntent?.sendWithBal()
            }

    }
}