package com.myxoz.life.ui.socialgraph

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RenderableSocialGraph {
    var origin by mutableStateOf(Offset.Zero)
    var nodeStartOrigin by mutableStateOf(Offset.Zero)
    var selectedNode by mutableStateOf<Long?>(null)
        private set
    private val offsetOrigin by derivedStateOf {
        val selectNode = selectedNode
        if(selectNode != null)
            allNodes[selectNode]?.position?.plus(nodeStartOrigin) ?: Offset.Zero
        else
            Offset.Zero
    }
    fun setNodeSelection(node: Long?) {
        val oldNode = selectedNode
        selectedNode = node
        if(oldNode != null) {
            val selectedNode = allNodes[oldNode] ?: return
            origin += selectedNode.position + nodeStartOrigin
        }
        if(node != null) {
            val selectedNode = allNodes[node] ?: return
            nodeStartOrigin = -selectedNode.position
        }
    }
    val offset by derivedStateOf {
        offsetOrigin + origin
    }
    var scale by mutableFloatStateOf(.4f)
    var angle by mutableFloatStateOf(0f)
    private var graph by mutableStateOf(SocialGraph.EMPTY)
    val allPeople by derivedStateOf { graph.nodes }
    val totalWeight by derivedStateOf { graph.totalWeight() }
    private val allNodes = mutableStateMapOf<Long, NodeState>()
    val renderedNodes by derivedStateOf {
        allNodes.filterKeys(graph.nodes::contains)
    }
    val edges by derivedStateOf {
        graph.edges
    }
    fun updateSocialGraph(newSocialGraph: SocialGraph) {
        val newPeople = newSocialGraph.nodes - allNodes.keys
        val newNodes = newPeople.mapIndexed { index, personId ->
            val angle = 2 * Math.PI * index / newPeople.size
            personId to NodeState(
                Offset(
                    x = SPAWNRADIUS * cos(angle).toFloat(),
                    y = SPAWNRADIUS * sin(angle).toFloat()
                )
            )
        }
        allNodes.putAll(newNodes)
        graph = newSocialGraph
    }
    fun renderPhysicsFrame() {
        // A. Apply Forces
        renderedNodes.forEach { (mainPersonId, mainNode) ->
            var fx = 0f
            var fy = 0f
            // 1. Repulsion (Every node pushes every other node)
            renderedNodes.forEach { (secondaryPersonId, node2) ->
                if (mainPersonId != secondaryPersonId) {
                    val dx = mainNode.position.x - node2.position.x
                    val dy = mainNode.position.y - node2.position.y
                    val distSq = dx * dx + dy * dy

                    // Prevent division by zero and extreme forces
                    if (distSq > 0.1f) {
                        val force = REPULSION_FORCE / distSq
                        val dist = sqrt(distSq)
                        fx += (dx / dist) * force
                        fy += (dy / dist) * force
                    } else {
                        // If nodes are too close, push them apart significantly to break the stack
                        val randomAngle = Math.random() * 2 * Math.PI
                        fx += cos(randomAngle).toFloat() / 100f
                        fy += sin(randomAngle).toFloat() / 100f
                    }
                }
            }
            // 3. Center Gravity (Pull towards 0,0 or center of screen)
            // Assuming center is (0,0) for the calculation, visual offset handled by Canvas
            fx -= mainNode.position.x * CENTER_STRENGTH
            fy -= mainNode.position.y * CENTER_STRENGTH
            mainNode.applyForce(fx, fy)
        }

        // 2. Spring Attraction (Edges pull nodes together)
        edges.forEach { (edge, weight) ->
            // Get the other node
            val aNode = renderedNodes[edge.a] ?: return@forEach
            val bNode = renderedNodes[edge.b] ?: return@forEach
            val dx = aNode.position.x - bNode.position.x
            val dy = aNode.position.y - bNode.position.y
            val dist = sqrt(dx * dx + dy * dy)

            if (dist > 0) {
                // Hooke's Law: Force proportional to displacement from resting length
                val weightRatio = weight.toFloat() / totalWeight
                val displacement = dist - SPRING_LENGTH
                val force = displacement * SPRING_STIFFNESS * weightRatio
                val fx = (dx / dist) * force
                val fy = (dy / dist) * force
                aNode.applyForce(-fx, -fy)
                bNode.applyForce(fx, fy)
            }
        }
        renderedNodes.forEach { (_, node) ->
            node.applyVelocity()
        }
    }
    companion object {
        private const val REPULSION_FORCE = 5_000_000f // How much nodes hate each other
        private const val SPRING_LENGTH = 300f      // Ideal distance between connected nodes
        private const val SPRING_STIFFNESS = 10f  // How strong the edges are
        private const val DAMPING = .05f           // Friction (0.9 = loses 10% speed per frame)
        private const val CENTER_STRENGTH = 0.01f   // Keeps graph centered

        private const val SPAWNRADIUS = 500f // Radius of the initial spawn
        fun Offset.rotateBy(angle: Float): Offset {
            val angleInRadians = angle * (PI / 180)
            val cos = cos(angleInRadians)
            val sin = sin(angleInRadians)
            return Offset((x * cos - y * sin).toFloat(), (x * sin + y * cos).toFloat())
        }
    }

    fun graphToRotatedPosition(
        position: Offset,
    ): Offset {
        return offset + (position - offset).rotateBy(angle)
    }

    fun nodePositions(edge: SocialGraph.EdgeKey): Pair<Offset, Offset>?{
        val aPosition = renderedNodes[edge.a]?.position ?: return null
        val toPosition = renderedNodes[edge.b]?.position ?: return null
        return aPosition to toPosition
    }

    fun centerOf(
        edge: SocialGraph.EdgeKey,
    ): Offset? {
        val fromPosition = renderedNodes[edge.a]?.position ?: return null
        val toPosition = renderedNodes[edge.b]?.position ?: return null
        return graphToRotatedPosition((fromPosition + toPosition) / 2f)
    }

    class NodeState(
        initPosition: Offset = Offset.Zero,
    ) {
        fun applyForce(fx: Float, fy: Float) {
            velocity.x += fx
            velocity.y += fy
        }

        fun applyVelocity() {
            velocity.damp()
            position += Offset(velocity.x, velocity.y)
        }

        var position by mutableStateOf(initPosition)
        private val velocity = Velocity()
        private class Velocity(
            var x: Float = 0f,
            var y: Float = 0f
        ) {
            fun damp() {
                x *= DAMPING
                y *= DAMPING
            }
        }
    }
}
