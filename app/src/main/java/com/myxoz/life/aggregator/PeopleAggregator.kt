package com.myxoz.life.aggregator

import android.util.Log
import com.myxoz.life.api.syncables.ProfilePictureSyncable
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.events.RawEvent
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.ui.options.settings.ME_ID
import com.myxoz.life.ui.socialgraph.SocialGraph
import com.myxoz.life.ui.socialgraph.SocialGraphPanel
import com.myxoz.life.utils.diagrams.PieChart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

class PeopleAggregator(
    private val repos: DatabaseInterface
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getLastInteraction(personId: Long) = repos.calendarInterface.interactedWithPerson(personId).mapLatest {
        SyncedEvent.from(
            RawEvent.PreparedEventContent.prepareContentFor(
                repos.readSyncableDaos.peopleDao.getLastInteractionByPerson(personId, System.currentTimeMillis()) ?: return@mapLatest null,
                repos.readSyncableDaos.eventDetailsDao
            ) ?: return@mapLatest null
        )
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getNextInteraction(personId: Long) = repos.calendarInterface.interactedWithPerson(personId).mapLatest {
        SyncedEvent.from(
            RawEvent.PreparedEventContent.prepareContentFor(
                repos.readSyncableDaos.peopleDao.getNextInteractionByPerson(personId, System.currentTimeMillis()) ?: return@mapLatest null,
                repos.readSyncableDaos.eventDetailsDao
            ) ?: return@mapLatest null
        )
    }
    private fun mergeIntervals(intervals: List<Pair<Long, Long>>): List<Pair<Long, Long>> {
        if (intervals.isEmpty()) return emptyList()
        val sorted = intervals.sortedBy { it.first }
        val out = mutableListOf<Pair<Long, Long>>()
        var current = sorted[0]

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.first <= current.second) {
                // overlap, extend
                current = current.first to maxOf(current.second, next.second)
            } else {
                out += current
                current = next
            }
        }
        out += current
        return out
    }
    suspend fun getPieChartFor(id: Long, chartScale: Int): Map<String, PieChart.Companion.PieChartPart> {
        val db = repos.readSyncableDaos.eventDetailsDao
        val totals = mutableMapOf<Int, Long>()
        val socialEvents = db.getEventsWithPerson(id).filter {
            it.start > (if(chartScale == 0) 0L else System.currentTimeMillis() - 24*3600L*1000*(when (chartScale) {
                1 -> 356L
                2 -> 30L
                else -> 7L
            }))
        }
        for (social in socialEvents) {
            if(social.start > System.currentTimeMillis()) continue
            val sStart = social.start
            val sEnd = social.end
            val overlapping = db.getEventsOverlapping(sStart, sEnd)
            val coverage = mutableListOf<Pair<Long, Long>>()
            for (e in overlapping) {
                if (e.id == social.id) continue
                val oStart = maxOf(sStart, e.start)
                val oEnd = minOf(sEnd, e.end)
                if (oStart < oEnd) {
                    val duration = oEnd - oStart
                    totals[e.type] = (totals[e.type] ?: 0) + duration
                    coverage += oStart to oEnd
                }
            }
            val merged = mergeIntervals(coverage)
            // Not already grabed parts
            var cursor = sStart
            for ((cStart, cEnd) in merged) {
                if (cursor < cStart) {
                    val pureSocial = cStart - cursor
                    totals[social.type] = (totals[social.type] ?: 0) + pureSocial
                }
                cursor = maxOf(cursor, cEnd)
            }
            if (cursor < sEnd) {
                val pureSocial = sEnd - cursor
                totals[social.type] = (totals[social.type] ?: 0) + pureSocial
            }
        }
        val newChart = PieChart.build {
            totals.forEach {
                val type = EventType.getById(it.key) ?: EventType.Empty
                add(type.colors.bg, it.value.toDouble(), type.id.toString())
            }
        }
        return newChart
    }
    suspend fun generateNodes(query: SocialGraphQuery): SocialGraph  {
        return withContext(Dispatchers.IO) {
            val readSyncableDaos = repos.readSyncableDaos
            val edges = mutableMapOf<SocialGraph.EdgeKey, Long>()
            val duration = query.chartScale.millis()
            val start = if(duration == null) 0L else query.endsAt - duration
            val socialEventsByEventId = readSyncableDaos.eventDetailsDao.getAllPeopleEvents(start, query.endsAt).associateBy { it.id }
            val participantsByEventId = readSyncableDaos.eventDetailsDao.getMappingsForPeopleParticipatingIn(socialEventsByEventId.keys).groupBy { it.eventId }
            val iterStart = if(query.addMyself) -1 else 0
            val nodesList = mutableSetOf<Long>()
            for ((eventId, participatingPeople) in participantsByEventId) {
                val decodedEvent = socialEventsByEventId[eventId] ?: continue
                val coercedEnd = decodedEvent.end.coerceAtMost(query.endsAt)
                val coercedStart = decodedEvent.start.coerceAtLeast(start)
                val weight = (coercedEnd - coercedStart) / 1000
                for (personIndex in iterStart..<participatingPeople.size) {
                    val firstPersonId = if(personIndex == -1) ME_ID else participatingPeople[personIndex].personId
                    nodesList.add(firstPersonId)
                    for(secondPerson in (personIndex + 1)..<participatingPeople.size) {
                        val secondPersonId = participatingPeople[secondPerson].personId
                        edges.merge(SocialGraph.EdgeKey.of(firstPersonId, secondPersonId), weight, Long::plus)
                    }
                }
            }
            Log.d("SocialGraph","We have ${nodesList.size} nodes with ${edges.size} edges")
            SocialGraph(nodesList, edges)
        }
    }
    fun getProfilePicture(person: Long) = repos.peopleInterface.getProfilePicture(person).map { person ->
        ProfilePictureSyncable.base64ToBitmap(
            person?.bitmapBase64 ?: return@map null
        )
    }
    class NeverEqual{
        override fun equals(other: Any?) = false
        override fun hashCode() = System.identityHashCode(this)
    }
    data class SocialGraphQuery(
        val addMyself: Boolean,
        val chartScale: SocialGraphPanel.SocialGraphScale,
        val endsAt: Long
    )
}