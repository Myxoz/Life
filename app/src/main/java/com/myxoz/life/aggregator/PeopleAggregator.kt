package com.myxoz.life.aggregator

import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.myxoz.life.api.syncables.ProfilePictureSyncable
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.dbwrapper.events.EventEntity
import com.myxoz.life.dbwrapper.people.PersonEntity
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.screens.options.ME_ID
import com.myxoz.life.screens.person.GraphEdge
import com.myxoz.life.screens.person.SocialGraphNode
import com.myxoz.life.utils.diagrams.PieChart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

class PeopleAggregator(
    private val repos: AppRepositories
) {
    fun getLastInteraction(personId: Long) = repos.calendarRepo.interactedWithPerson(personId).map {
        SyncedEvent.from(
            repos.readSyncableDaos.eventDetailsDao,
            repos.readSyncableDaos.peopleDao.getLastInteractionByPerson(personId, System.currentTimeMillis()) ?: return@map null
        )
    }
    fun getNextInteraction(personId: Long) = repos.calendarRepo.interactedWithPerson(personId).map {
        SyncedEvent.from(
            repos.readSyncableDaos.eventDetailsDao,
            repos.readSyncableDaos.peopleDao.getNextInteractionByPerson(personId, System.currentTimeMillis()) ?: return@map null
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
                add(type.color, it.value.toDouble(), type.id.toString())
            }
        }
        return newChart
    }
    suspend fun generateNodes(addMyself: Boolean, chartScale: Int) =
        withContext(Dispatchers.IO) {
            val readSyncableDaos = repos.readSyncableDaos
            val nodes = mutableMapOf<Long, SocialGraphNode>()
            var totalWeightAcc = 0L
            val addMyself = addMyself
            if(addMyself) {
                nodes[ME_ID] = SocialGraphNode(ME_ID, "Ich", mutableListOf())
            }
            val end = System.currentTimeMillis()
            val start = if(chartScale==0) 0L else System.currentTimeMillis() - 1000L*3600L*24*when(chartScale){
                1 -> 356
                2 -> 30
                else -> 7
            }
            val allEventsMap = readSyncableDaos.eventDetailsDao.getAllPeopleEvents(start, end).run {
                val map = mutableMapOf<Long, EventEntity>()
                forEach { map[it.id] = it }
                map
            }
            val allMapping = readSyncableDaos.eventDetailsDao.getAllPeopleMappingAfter(start, end).groupBy { it.eventId }
            val allPeople = readSyncableDaos.peopleDao.getAllPeople().run {
                val map = mutableMapOf<Long, PersonEntity>()
                forEach { map[it.id] = it }
                map
            }
            val iterStart = if(addMyself) -1 else 0
            for (event in allMapping) {
                val decodedEvent = allEventsMap[event.value[0].eventId] ?: continue
                val weight = (decodedEvent.end - decodedEvent.start) / 1000
                totalWeightAcc += weight
                for (fi in iterStart..<event.value.size) {
                    val entry = if(fi == -1) ME_ID else event.value[fi].personId
                    val name = allPeople[entry]?.name ?: continue
                    val person = nodes.getOrPut(entry) {
                        SocialGraphNode(
                            entry, name, mutableListOf()
                        )
                    }
                    for(si in iterStart..<event.value.size) {
                        val otherPerson = if(si == -1) ME_ID else event.value[si].personId
                        if(otherPerson == entry) continue
                        val name = allPeople[otherPerson]?.name ?: continue
                        val otherPersonNode = nodes.getOrPut(otherPerson) {
                            SocialGraphNode(
                                otherPerson, name, mutableListOf()
                            )
                        }
                        val existingEdge = person.edges.find { it.nodeB.personId == otherPerson|| it.nodeA.personId == otherPerson}
                        if(existingEdge == null) {
                            val newEdge = GraphEdge(person, otherPersonNode, weight)
                            person.edges.add(newEdge)
                            otherPersonNode.edges.add(newEdge)
                        } else {
                            existingEdge.weight += weight
                            // Only for one, because the lists contain the same instance
                        }
                    }
                }
            }
            val nodeList = nodes.values.toList()
            Log.d("SocialGraph","We have ${nodeList.size} nodes with ${nodeList.sumOf { it.edges.size }.toFloat() / 2f} edges")
            val radius = 500f
            nodeList.forEachIndexed { index, node ->
                val angle = 2 * Math.PI * index / nodeList.size
                node.position = Offset(
                    x = radius * cos(angle).toFloat(),
                    y = radius * sin(angle).toFloat()
                )
            }
            return@withContext nodes to totalWeightAcc
        }
    fun getProfilePicture(person: Long) = repos.peopleRepo.getProfilePicture(person).map { person ->
        ProfilePictureSyncable.base64ToBitmap(
            person?.data?.bitmapBase64 ?: return@map null
        )
    }
}