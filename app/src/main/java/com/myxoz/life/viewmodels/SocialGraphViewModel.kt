package com.myxoz.life.viewmodels

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import com.myxoz.life.dbwrapper.EventEntity
import com.myxoz.life.dbwrapper.PersonEntity
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.screens.options.ME_ID
import com.myxoz.life.screens.person.GraphEdge
import com.myxoz.life.screens.person.SocialGraphNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

class SocialGraphViewModel(val db: StorageManager): ViewModel() {
    var totalWeight = MutableStateFlow(1L)
    var addMyself = MutableStateFlow(false)
    var showTimes = MutableStateFlow(false)
    val nodes = MutableStateFlow(mapOf<Long, SocialGraphNode>())
    val selectedNode = MutableStateFlow<Long?>(null)
    val chartScale = MutableStateFlow(2)
    private fun mergeNodes(newMap: Map<Long, SocialGraphNode>){
        val oldNodes = nodes.value
        val newNodes = newMap.toMutableMap()
        for (pair in oldNodes) {
            val entry = newNodes[pair.key] ?: continue
            entry.position = pair.value.position
            entry.velocity = pair.value.velocity
        }
        nodes.value = newNodes
    }
    suspend fun regenerateNodes() {
        return withContext(Dispatchers.IO) {
            val nodes = mutableMapOf<Long, SocialGraphNode>()
            var totalWeightAcc = 0L
            val addMyself = addMyself.value
            if(addMyself) {
                nodes[ME_ID] = SocialGraphNode(ME_ID, "Ich", mutableListOf())
            }
            val end = System.currentTimeMillis()
            val start = if(chartScale.value==0) 0L else System.currentTimeMillis() - 1000L*3600L*24*when(chartScale.value){
                1 -> 356
                2 -> 30
                else -> 7
            }
            val allEventsMap = db.events.getAllPeopleEvents(start, end).run {
                val map = mutableMapOf<Long, EventEntity>()
                forEach { map[it.id] = it }
                map
            }
            val allMapping = db.peopleMapping.getAllSocialMappingAfter(start, end).groupBy { it.eventId }
            val allPeople = db.people.getAllPeople().run {
                val map = mutableMapOf<Long, PersonEntity>()
                forEach { map[it.id] = it }
                map
            }
            val iterStart = if(addMyself) -1 else 0
            for (event in allMapping) {
                val decodedEvent = allEventsMap[event.value[0].eventId]!!
                val weight = (decodedEvent.end - decodedEvent.start) / 1000
                totalWeightAcc += weight
                for (fi in iterStart..<event.value.size) {
                    val entry = if(fi == -1) ME_ID else event.value[fi].personId
                    val person = nodes.getOrPut(entry) {
                        SocialGraphNode(
                            entry, allPeople[entry]!!.name, mutableListOf()
                        )
                    }
                    for(si in iterStart..<event.value.size) {
                        val otherPerson = if(si == -1) ME_ID else event.value[si].personId
                        if(otherPerson == entry) continue
                        val otherPersonNode = nodes.getOrPut(otherPerson) {
                            SocialGraphNode(
                                otherPerson, allPeople[otherPerson]!!.name, mutableListOf()
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
            println("We have ${nodeList.size} nodes with ${nodeList.sumOf { it.edges.size }.toFloat() / 2f} edges")
            val radius = 500f
            nodeList.forEachIndexed { index, node ->
                val angle = 2 * Math.PI * index / nodeList.size
                node.position = Offset(
                    x = radius * cos(angle).toFloat(),
                    y = radius * sin(angle).toFloat()
                )
            }
            mergeNodes(nodes)
            totalWeight.value = totalWeightAcc
        }
    }
}