package com.myxoz.life.aggregator

import com.myxoz.life.R
import com.myxoz.life.api.syncables.CommitSyncable
import com.myxoz.life.dbwrapper.banking.formatCents
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.repositories.BankingRepo
import com.myxoz.life.repositories.utils.FlowCache
import com.myxoz.life.screens.feed.instantevents.InstantEvent
import com.myxoz.life.screens.feed.instantevents.InstantEvent.Companion.INSTANTEVENTSIZE
import com.myxoz.life.screens.feed.main.SegmentedEvent
import com.myxoz.life.utils.def
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class CalendarAggregator(
    private val repos: AppRepositories
) {
    private val _getInstantEventsForDayCache = FlowCache<LocalDate, List<InstantEvent.InstantEventGroup>> { date ->
        combine(
            repos.commitsRepo.getCommitsForDay(date),
            repos.bankingRepo.getTransactionsAt(date),
            repos.bankingRepo.getFutureTransactions(date)
        ) { commits, transactions, futureTransactions ->
            val commits = commits?.data.def(listOf()).mapNotNull {
                commitToInstantEvent(it)
            }
            val transactions = transactions.def(listOf()).mapNotNull {
                bankEntryAsInstantEvent(it)
            }
            val futureTransactions = futureTransactions?.data.def(listOf()).mapNotNull {
                bankEntryAsInstantEvent(it)
            }
            createGroupedInstantEvents(commits + transactions + futureTransactions)
        }
    }

    fun getInstantEventsForDay(date: LocalDate) = _getInstantEventsForDayCache.get(date)
    private fun commitToInstantEvent(commitSyncable: CommitSyncable): InstantEvent? {
        return InstantEvent(
            R.drawable.commit,
            commitSyncable.repoName,
            commitSyncable.commitDate ?: return null,
        ) { screens ->
            screens.openCommit(commitSyncable.commitSha)
        }
    }
    private fun bankEntryAsInstantEvent(it: BankingRepo.BankingDisplayEntity): InstantEvent?{
        return InstantEvent(
            if (it.entity.isWirelessPayment()) R.drawable.wireless_pay else if (it.entity.card) R.drawable.pay_with_card else R.drawable.bank_transfer,
            it.entity.amountCents.formatCents(),
            it.sidecar?.date ?: it.entity.purposeDate ?: return null
            // If this is not set, dont display them in feed, we only have a value date and this
            // is always around 23 o'clock. This IS UI code. And UI Code belongs here.
        ) { screens ->
            screens.openTransaction(it)
        }
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
    private val segmentedEventsFlow = FlowCache<LocalDate, List<SegmentedEvent>>{
        combine(
            repos.calendarRepo.eventsForDay(it),
            getInstantEventsForDay(it),
        ) { events, instantEvents ->
            SegmentedEvent.getSegmentedEvents(
                events?.data.def(listOf()),
                instantEvents,
                (INSTANTEVENTSIZE * 3600L).toLong() * 1000L
            )
        }
    }
    fun getSegmentedEvents(it: LocalDate) = segmentedEventsFlow.get(it)
}