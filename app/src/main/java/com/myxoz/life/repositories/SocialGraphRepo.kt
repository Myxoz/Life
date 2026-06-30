package com.myxoz.life.repositories

import androidx.compose.ui.geometry.Offset
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.ui.person.SocialGraphNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SocialGraphRepo(
    val repos: DatabaseInterface
) {
    val scale = MutableStateFlow(.4f)
    val offset = MutableStateFlow<Offset?>(null)
    val totalWeight = MutableStateFlow(1L)
    val addMyself = MutableStateFlow(false)
    val showTimes = MutableStateFlow(false)
    val nodes = MutableStateFlow(mapOf<Long, SocialGraphNode>())
    val selectedNode = MutableStateFlow<Long?>(null)
    val chartScale = MutableStateFlow(2)
    private fun mergeNodesWith(newMap: Map<Long, SocialGraphNode>){
        val oldNodes = nodes.value
        val newNodes = newMap.toMutableMap()
        for (pair in oldNodes) {
            val entry = newNodes[pair.key] ?: continue
            entry.position = pair.value.position
            entry.velocity = pair.value.velocity
        }
        nodes.value = newNodes
    }
    init {
        repos.appScope.launch {
            combine(
                addMyself,
                chartScale,
                repos.calendarInterface.interactedWithAnyPerson
            ) { addMyselfValue, chartScaleValue, eventUpdate ->
                Triple(addMyselfValue, chartScaleValue, eventUpdate)
            }
                .collectLatest {
                    val gen = repos.aggregators.peopleAggregator.generateNodes(it.first, it.second)
                    totalWeight.value = gen.second
                    mergeNodesWith(gen.first)
                }
        }
    }
}