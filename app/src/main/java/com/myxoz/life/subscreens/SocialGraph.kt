package com.myxoz.life.subscreens

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalStorage
import com.myxoz.life.R
import com.myxoz.life.calendar.feed.msToDisplay
import com.myxoz.life.events.toPx
import com.myxoz.life.rippleClick
import com.myxoz.life.subscreens.displayperson.ButtonGroup
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.viewmodels.ProfileInfoModel
import com.myxoz.life.viewmodels.SocialGraphViewModel
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

const val REPULSION_FORCE = 900_000f // How much nodes hate each other
const val SPRING_LENGTH = 400f      // Ideal distance between connected nodes
const val SPRING_STIFFNESS = 0.7f  // How strong the edges are
const val DAMPING = 0.10f           // Friction (0.9 = loses 10% speed per frame)
const val CENTER_STRENGTH = 0.01f   // Keeps graph centered

@Composable
fun SocialGraph(socialGraphViewModel: SocialGraphViewModel, profileInforModel: ProfileInfoModel){
    val db = LocalStorage.current
    val nav = LocalNavController.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val addMyself by socialGraphViewModel.addMyself.collectAsState()
    val nodes by socialGraphViewModel.nodes.collectAsState()
    val totalWeight by socialGraphViewModel.totalWeight.collectAsState()
    LaunchedEffect(Unit) {
        socialGraphViewModel.regenerateNodes()
    }
    var frameTrigger by remember { mutableLongStateOf(0L) }
    val showTimes by socialGraphViewModel.showTimes.collectAsState()
    LaunchedEffect(nodes.size) {
        if (nodes.isEmpty()) return@LaunchedEffect

        while (true) {
            // Wait for the next frame (vsync)
            withInfiniteAnimationFrameMillis { frameTime ->
                val nodes = nodes.values.toList() // Snapshot for this frame

                // A. Apply Forces
                nodes.forEachIndexed { i, node1 ->
                    var fx = 0f
                    var fy = 0f
                    // 1. Repulsion (Every node pushes every other node)
                    nodes.forEachIndexed { j, node2 ->
                        if (i != j) {
                            val dx = node1.position.x - node2.position.x
                            val dy = node1.position.y - node2.position.y
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

                    // 2. Spring Attraction (Edges pull nodes together)
                    node1.edges.forEach { edge ->
                        // Get the other node
                        val target = if (edge.nodeA.personId == node1.personId) edge.nodeB else edge.nodeA

                        val dx = target.position.x - node1.position.x
                        val dy = target.position.y - node1.position.y
                        val dist = sqrt(dx * dx + dy * dy)

                        if (dist > 0) {
                            // Hooke's Law: Force proportional to displacement from resting length
                            val w = edge.weight.toFloat()
                            val weightRatio = w / totalWeight.toFloat()
                            val displacement = dist - SPRING_LENGTH
                            val force = displacement * SPRING_STIFFNESS * weightRatio

                            fx += (dx / dist) * force
                            fy += (dy / dist) * force
                        }
                    }

                    // 3. Center Gravity (Pull towards 0,0 or center of screen)
                    // Assuming center is (0,0) for the calculation, visual offset handled by Canvas
                    fx -= node1.position.x * CENTER_STRENGTH
                    fy -= node1.position.y * CENTER_STRENGTH

                    // Update Velocity
                    node1.velocity = Offset(
                        (node1.velocity.x + fx) * DAMPING,
                        (node1.velocity.y + fy) * DAMPING
                    )
                }

                // B. Apply Position Updates
                nodes.forEach { node ->
                    node.position += node.velocity
                }

                frameTrigger = frameTime // Trigger redraw
            }
        }
    }

    var hoveredNode by remember { mutableStateOf<SocialGraphNode?>(null) }
    var selectedNode by remember { mutableStateOf<SocialGraphNode?>(null) } // If you want click persistence

    // Helper to convert Screen Coordinates -> Graph Coordinates
    fun hitTest(pointerPosition: Offset, panOffset: Offset, zoomScale: Float): SocialGraphNode? {
        val graphX = (pointerPosition.x / zoomScale) + panOffset.x
        val graphY = (pointerPosition.y / zoomScale) + panOffset.y

        return nodes.values.find { node ->
            val dx = node.position.x - graphX
            val dy = node.position.y - graphY
            (dx * dx + dy * dy) < (45f * 45f)
        }
    }

    // 3. Rendering
    Box(Modifier.fillMaxSize()) {
        val config = LocalConfiguration.current
        val initWidth = config.screenWidthDp.dp.toPx()
        val initHeight = config.screenHeightDp.dp.toPx()
        var scale by remember { mutableFloatStateOf(0.4f) }
        var offset by remember { mutableStateOf(-Offset(initWidth / scale / 2f, initHeight / scale / 2f)) }

        val textPaint = remember {
            android.graphics.Paint().apply {
                color = Colors.PRIMARYFONT.toArgb()
                textSize = 40f
                textAlign = android.graphics.Paint.Align.CENTER
            }
        }
        val relevantEdgeTextPaint = remember {
            android.graphics.Paint().apply {
                color = Colors.PRIMARYFONT.toArgb()
                textSize = 30f
                textAlign = android.graphics.Paint.Align.CENTER
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, gestureZoom, _ ->
                        val oldScale = scale
                        val newScale = scale * gestureZoom
                        offset =
                            (offset + centroid / oldScale) - (centroid / newScale + pan / oldScale)
                        scale = newScale
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val clicked = hitTest(tapOffset, offset, scale)
                        if (clicked != null) {
                            if (selectedNode?.personId == clicked.personId) {
                                if (clicked.personId == 0L) return@detectTapGestures
                                coroutineScope.launch {
                                    profileInforModel.openPersonDetails(
                                        clicked.personId,
                                        nav,
                                        db,
                                        context
                                    )
                                }
                                return@detectTapGestures
                            }
                            selectedNode = clicked
                            println("Clicked on: ${clicked.name}")
                        } else {
                            selectedNode = null // Deselect if clicking empty space
                        }
                    }
                }
        ) {
            // Read frameTrigger to ensure recomposition happens on animation ticks
            val tick = frameTrigger

            withTransform({
                // Apply User Pan/Zoom first
                translate(
                    -offset.x * scale, -offset.y * scale
                    //offset.x + screenCenter.x, offset.y + screenCenter.y
                )
                scale(scale, pivot = Offset.Zero)
            }) {
                val activeNode = hoveredNode ?: selectedNode
                val isDimmedMode = activeNode != null
                fun GraphEdge.drawEdge(color: Color) {
                    drawLine(
                        color = color,
                        start = this.nodeA.position,
                        end = this.nodeB.position,
                        strokeWidth = (this.weight / totalWeight.toFloat()) * 60f
                    )
                }
                val relevantEdges = mutableListOf<GraphEdge>()
                nodes.values.forEach { node ->
                    node.edges.forEach { edge ->
                        val otherNode = if (edge.nodeA.personId == node.personId) edge.nodeB else edge.nodeA
                        val isRelevantEdge = node.personId == activeNode?.personId || otherNode.personId == activeNode?.personId
                        if (otherNode.personId <= node.personId) return@forEach
                        if (!isDimmedMode) {
                            edge.drawEdge(Colors.SocialGraphColors.EDGE)
                        } else {
                            if(!isRelevantEdge){
                                edge.drawEdge(Colors.SocialGraphColors.IRRELEVANT_EDGE)
                            } else {
                                relevantEdges.add(edge)
                            }
                        }
                    }
                }
                for (edge in relevantEdges) {
                    edge.drawEdge(Colors.SocialGraphColors.RELEVANT_EDGE)
                }
                if(showTimes)
                    for (edge in relevantEdges) {
                        drawContext.canvas.nativeCanvas.drawText(
                            (edge.weight  * 500 /* Div by two due to double counting */).toInt().msToDisplay(true),
                            (edge.nodeA.position.x + edge.nodeB.position.x) / 2,
                            (edge.nodeA.position.y + edge.nodeB.position.y) / 2,
                            relevantEdgeTextPaint
                        )
                    }
                nodes.values.forEach { node ->
                    val isConnected = activeNode != null && node.edges.any {
                        it.nodeA.personId == activeNode.personId || it.nodeB.personId == activeNode.personId
                    }
                    val isSelf = node.personId == activeNode?.personId
                    val nodeColor = when {
                        isSelf -> Colors.SocialGraphColors.SELECTED_NODE
                        isConnected -> Colors.SocialGraphColors.RELEVANT_NODE
                        isDimmedMode -> Colors.SocialGraphColors.IRRELEVANT_NODE
                        else -> Colors.SocialGraphColors.NODE
                    }
                    drawCircle(
                        color = nodeColor,
                        radius = if(isSelf) 50f else 40f,
                        center = node.position
                    )
                    if (isSelf || isConnected || !isDimmedMode) {
                        drawContext.canvas.nativeCanvas.drawText(
                            node.name,
                            node.position.x,
                            node.position.y - 50f,
                            textPaint
                        )
                    }
                }
            }
        }
        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .background(Colors.SECONDARY, CircleShape),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val list = remember { arrayOf("All", "1y", "30d", "1w") }
            ButtonGroup(
                list,
                200.dp,
                socialGraphViewModel.dateRange,
            ) {
                coroutineScope.launch {
                    socialGraphViewModel.regenerateNodes()
                    profileInforModel.chartScale.value = it
                }
            }
            val height = 20.dp
            VerticalDivider(Modifier.height(height), color = Colors.DIVIDERS)
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .rippleClick {
                        socialGraphViewModel.showTimes.value = !socialGraphViewModel.showTimes.value
                    }
                    .padding(5.dp)
            ) {
                Icon(
                    painterResource(
                        if(showTimes) R.drawable.hide_time else R.drawable.time
                    ),
                    "Me",
                    Modifier.fillMaxSize(),
                    tint = Colors.SECONDARYFONT
                )
            }
            Box(
                Modifier
                    .padding(end = 5.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .rippleClick {
                        val newMyself = !addMyself
                        socialGraphViewModel.addMyself.value = newMyself
                        coroutineScope.launch {
                            socialGraphViewModel.regenerateNodes()
                        }
                    }
                    .padding(5.dp)
            ) {
                Icon(
                    painterResource(R.drawable.contacts),
                    "Me",
                    Modifier.fillMaxSize(),
                    tint = Colors.SECONDARYFONT
                )
            }
        }
    }
}


data class SocialGraphNode(
    val personId: Long,
    val name: String,
    val edges: MutableList<GraphEdge>,
    var position: Offset = Offset.Zero,
    var velocity: Offset = Offset.Zero,
) {
    override fun equals(other: Any?) = other is SocialGraphNode && other.personId == personId

    override fun hashCode() = personId.hashCode()
}
data class GraphEdge(
    val nodeA: SocialGraphNode,
    val nodeB: SocialGraphNode,
    var weight: Long
)