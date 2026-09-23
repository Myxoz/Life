package com.myxoz.life.ui.socialgraph

import android.util.Log
import androidx.compose.runtime.snapshotFlow
import com.myxoz.life.aggregator.PeopleAggregator
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.utils.atEndAsMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import java.time.ZoneId

class SocialGraphRepo(
    private val repos: DatabaseInterface
) {
    val earliestEventFlow = repos.calendarInterface.firstEventTimestamp
    val controlPanel = SocialGraphPanel()
    val socialGraph = RenderableSocialGraph()
    val socialGraphQueryFlow: Flow<PeopleAggregator.SocialGraphQuery> =
        combine(
            snapshotFlow {
                Triple(controlPanel.addMyself, controlPanel.chartScale, controlPanel.currentTimelineDate)
            },
            repos.calendarInterface.todayFlow,
            repos.calendarInterface.debouncedInteractedWithAnyPerson
        ) { (addMyself, scale, _), today, _ ->
            PeopleAggregator.SocialGraphQuery(
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

    fun allPeopleForIds(it: List<Long>): Flow<List<PersonSyncable>> = repos.peopleInterface.getPeople(it)


}