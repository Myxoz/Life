package com.myxoz.life.aggregator

import android.util.Log
import com.myxoz.life.feature.socialgraph.SocialGraph
import com.myxoz.life.feature.socialgraph.SocialGraphPanel
import com.myxoz.life.feature.socialgraph.SocialGraphRepo
import com.myxoz.life.storage.interfaces.CalendarInterface
import com.myxoz.life.storage.interfaces.PeopleInterface.Companion.ME_ID
import kotlin.collections.get
import kotlin.text.get
import kotlin.time.Duration.Companion.seconds

import com.myxoz.life.storage.api.syncables.ProfilePictureSyncable
import com.myxoz.life.storage.api.syncables.SyncedEvent
import com.myxoz.life.storage.events.RawEvent
import com.myxoz.life.storage.events.additionals.EventType
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
    private val calendarInterface: CalendarInterface
) {
    suspend fun generateNodes(query: SocialGraphRepo.SocialGraphQuery): SocialGraph {
        return withContext(Dispatchers.IO) {
            val readSyncableDaos = calendarInterface.eventsBetween(query.)
            val edges = mutableMapOf<SocialGraph.EdgeKey, Long>()
            val duration = query.chartScale.millis()
            val start = if(duration == null) 0L else query.endsAt - duration
            val socialEventsByEventId =
            val participantsByEventId = readSyncableDaos.eventDetailsDao.getMappingsForPeopleParticipatingIn(socialEventsByEventId.keys).groupBy { it.eventId }
            val iterStart = if(query.addMyself) -1 else 0
            val nodesList = mutableSetOf<Long>()
            for ((eventId, participatingPeople) in participantsByEventId) {
                val decodedEvent = socialEventsByEventId[eventId] ?: continue
                val coercedEnd = decodedEvent.end.coerceAtMost(query.endsAt)
                val coercedStart = decodedEvent.start.coerceAtLeast(start)
                val weight = (coercedEnd - coercedStart).seconds
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
}