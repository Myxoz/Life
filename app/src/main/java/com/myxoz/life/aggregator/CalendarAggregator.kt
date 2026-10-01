package com.myxoz.life.aggregator

import android.app.AlarmManager
import com.myxoz.life.R
import com.myxoz.life.core.def
import com.myxoz.life.storage.api.syncables.CommitSyncable
import com.myxoz.life.storage.api.syncables.ManualTransactionSyncable
import com.myxoz.life.storage.dbwrapper.banking.formatCents
import com.myxoz.life.storage.interfaces.BankingInterface
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.ui.feed.instantevents.InstantEvent
import com.myxoz.life.ui.feed.instantevents.InstantEvent.Companion.INSTANTEVENTSIZE
import com.myxoz.life.ui.feed.main.PrerenderedEvent
import com.myxoz.life.utils.AndroidUtils.sendWithBal
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import kotlin.collections.listOf
import kotlin.collections.sortedBy
import kotlin.text.compareTo
import kotlin.text.toLong

class CalendarAggregator(
    private val repos: DatabaseInterface
) {
    fun getInstantEventsForDay(date: LocalDate) = combine(
        repos.calendarInterface.nextAlarmClockTs,
        repos.commitsInterface.getCommitsForDay(date),
        repos.bankingInterface.getSortedTransactionsAt(date),
        repos.todoInterface.getTodosForDay(date)
    ) { nextAlarm, commits, transactions, todos ->
        val nextAlarmEvent = listOfNotNull(
            nextAlarm?.let { alarmToInstantEvent(it) }
        )
        val commits = commits.def(listOf()).mapNotNull {
            commitToInstantEvent(it)
        }
        val transactions = transactions.def(listOf()).mapNotNull {
            bankEntryAsInstantEvent(it)
        }
        val todos = todos.def(listOf()).map {
            it.asInstantEvent()
        }
        createGroupedInstantEvents(commits + transactions + nextAlarmEvent + todos)
    }
    private fun createGroupedInstantEvents(mix: List<InstantEvent>): List<InstantEvent.InstantEventGroup>{
        val mix = mix.sortedBy { it.timestamp }
        val groups = mutableListOf<MutableList<InstantEvent>>(mutableListOf())
        var lastTs = mix.getOrNull(0)?.timestamp ?: return listOf()
        for (event in mix) {
            if(event.timestamp - lastTs.compareTo(INSTANTEVENTSIZE * (3600 * 1000L))) {
                groups.add(mutableListOf())
            }
            groups.last().add(event)
            lastTs = event.timestamp
        }
        return groups.map { InstantEvent.InstantEventGroup(it) }
    }
    fun getPrerenderedEvents(date: LocalDate) = combine(
        repos.calendarInterface.eventsForDay(date),
        getInstantEventsForDay(date),
    ) { events, instantEvents ->
        PrerenderedEvent.getPrerenderedEvents(
            events.def(listOf()),
            instantEvents,
            (INSTANTEVENTSIZE * 3600L).toLong() * 1000L
        )
    }
    companion object {
        private fun asInstantEvent(entity: BankingInterface.MixedBankingEntity): InstantEvent{
            return InstantEvent(
                entity.icon,
                entity.amount.formatCents(),
                entity.timestamp,
                { it is ManualTransactionSyncable && it.id == entity.getStoredManualTransactionSyncable()?.id }
            ) { screens ->
                screens.openTransaction(entity)
            }
        }
        fun bankEntryAsInstantEvent(entity: BankingInterface.MixedBankingEntity): InstantEvent?{
            return if(!entity.isTransaction) asInstantEvent(entity) else null
        }
        fun manualTransactionEntityAsInstantEvent(manualTransactionSyncable: ManualTransactionSyncable): InstantEvent {
            return asInstantEvent(BankingInterface.MixedBankingEntity.from(manualTransactionSyncable))
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
                { false },
            ) {
                alarm.showIntent?.sendWithBal()
            }
    }
}