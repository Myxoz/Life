package com.myxoz.life.calendar.feed.instantevents

import android.app.AlarmManager
import com.myxoz.life.core.minus
import com.myxoz.life.lifecore.instantevents.InstantEvent
import com.myxoz.life.storage.api.helper.MixedBankingEntity
import com.myxoz.life.storage.api.syncables.CommitSyncable
import com.myxoz.life.storage.api.syncables.TodoSyncable
import com.myxoz.life.storage.interfaces.BankingInterface
import com.myxoz.life.storage.interfaces.CalendarInterface
import com.myxoz.life.storage.interfaces.CommitsInterface
import com.myxoz.life.storage.interfaces.TodoInterface
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import java.time.LocalDate
import kotlin.time.Duration.Companion.hours

internal class InstantEventAggregator(
    private val calendarInterface: CalendarInterface,
    private val commitsInterface: CommitsInterface,
    private val bankingInterface: BankingInterface,
    private val todoInterface: TodoInterface
) {
    fun getInstantEventsForDay(date: LocalDate) = combine(
        calendarInterface.nextAlarmClockTs,
        commitsInterface.getCommitsForDay(date).filterNotNull(),
        bankingInterface.getSortedTransactionsAt(date),
        todoInterface.getTodosForDay(date).filterNotNull()
    ) { nextAlarm, commits, transactions, todos ->
        val instantEvents = buildList {
            val alarmEvent = nextAlarm?.alarmToInstantEvent()
            if(alarmEvent != null) add(alarmEvent)
            commits.forEach {
                val commit = commitToInstantEvent(it)
                if(commit != null) add(commit)
            }
            transactions.forEach {
                add(bankEntryAsInstantEvent(it))
            }
            todos.forEach {
                add(todoToInstantEvent(it))
            }
        }
        createGroupedInstantEvents(instantEvents)
    }
    private fun createGroupedInstantEvents(mix: List<InstantEvent>): List<InstantEventGroup>{
        val mix = mix.sortedBy { it.timestamp }
        val groups = mutableListOf<MutableList<InstantEvent>>(mutableListOf())
        var lastTs = mix.getOrNull(0)?.timestamp ?: return listOf()
        for (event in mix) {
            if((event.timestamp - lastTs) > INSTANTEVENTSIZE.hours) {
                groups.add(mutableListOf())
            }
            groups.last().add(event)
            lastTs = event.timestamp
        }
        return groups.map(::InstantEventGroup)
    }
    companion object {
        private fun commitToInstantEvent(commitSyncable: CommitSyncable): InstantEvent? =
            InstantEvent.Commit(commitSyncable, commitSyncable.commitDate ?: return null)

        private fun bankEntryAsInstantEvent(mixedBankingEntity: MixedBankingEntity) =
            InstantEvent.Transaction(mixedBankingEntity, mixedBankingEntity.timestamp)

        private fun todoToInstantEvent(todoSyncable: TodoSyncable) =
            InstantEvent.Todo(todoSyncable)

        private fun AlarmManager.AlarmClockInfo.alarmToInstantEvent() =
            InstantEvent.AlarmClock(this)
    }
}