package com.myxoz.life.feature.socialgraph

import android.util.Log
import androidx.compose.runtime.snapshotFlow
import com.myxoz.life.core.atEndAsMillis
import com.myxoz.life.lifeui.time.updatingTodayFlow
import com.myxoz.life.storage.api.syncables.PersonSyncable
import com.myxoz.life.storage.interfaces.CalendarInterface
import com.myxoz.life.storage.interfaces.PeopleInterface
import com.myxoz.life.storage.interfaces.aggregator.PeopleEventAggregator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import java.time.ZoneId

class SocialGraphRepo(
    private val peopleInterface: PeopleInterface,
    private val peopleEventAggregator: PeopleEventAggregator,
    private val calendarInterface: CalendarInterface
) {
    val earliestEventFlow = calendarInterface.firstEventTimestamp
    val controlPanel = SocialGraphPanel()
    val socialGraph = RenderableSocialGraph()
    val socialGraphQueryFlow: Flow<SocialGraphQuery> =
        combine(
            snapshotFlow {
                Triple(controlPanel.addMyself, controlPanel.chartScale, controlPanel.currentTimelineDate)
            },
            updatingTodayFlow(),
            calendarInterface.debouncedInteractedWithAnyPerson
        ) { (addMyself, scale, _), today, _ ->
            SocialGraphQuery(
                addMyself,
                scale,
                controlPanel.endDate(today).atEndAsMillis(ZoneId.systemDefault())
            )
        }
    fun updateSocialGraph(newSocialGraph: SocialGraph) {
        socialGraph.updateSocialGraph(newSocialGraph)
    }
    suspend fun keepNodesUpdated(){
        socialGraphQueryFlow.collectLatest { query ->
            Log.d(
                "SocialGraph",
                "Generating: scale=${query.chartScale}, endsAt=${query.endsAt}, addMyself=${query.addMyself}"
            )
            updateSocialGraph(repos.aggregators.peopleAggregator.generateNodes(query))
        }
    }

    fun allPeopleForIds(it: Set<Long>): Flow<List<PersonSyncable>> = peopleInterface.getPeople(it)

    data class SocialGraphQuery(
        val addMyself: Boolean,
        val chartScale: SocialGraphPanel.SocialGraphScale,
        val endsAt: Long
    )
}