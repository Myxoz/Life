package com.myxoz.life.feature.socialgraph

import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalWindowInfo
import com.myxoz.life.feature.socialgraph.RenderableSocialGraph.Companion.rotateBy
import com.myxoz.life.presentation.time.formatMsToDuration
import com.myxoz.life.storage.api.syncables.PersonSyncable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
internal fun SocialGraphGraph(socialGraph: RenderableSocialGraph, requestPeopleNames: (List<Long>) -> Flow<List<PersonSyncable>>, openPerson: (Long) -> Unit) {
    LaunchedEffect(1) {
        while (true) {
            // Wait for the next frame (vsync)
            withInfiniteAnimationFrameMillis { frameTime ->
                socialGraph.renderPhysicsFrame()
            }
        }
    }
    val peopleMap: Map<Long, String> by produceState(emptyMap(), socialGraph.allPeople) {
        requestPeopleNames(socialGraph.allPeople.toList()).collect { personList ->
            value = personList
                .associateBy { it.id }
                .mapValues { (_, value) -> value.name }
        }
    }
    val config = LocalWindowInfo.current.containerSize
    val center = Offset(
        config.width / 2f,
        config.height / 2f
    )
    BackHandler(
        socialGraph.selectedNode != null
    ) {
        socialGraph.setNodeSelection(null)
    }
    // Helper to convert Screen Coordinates -> Graph Coordinates
    fun hitTest(pointerPosition: Offset, panOffset: Offset, zoomScale: Float): Long? {
        val graphPosition = (pointerPosition - center).rotateBy(-socialGraph.angle) / zoomScale + panOffset

        return socialGraph.renderedNodes.entries.find { (_, node) ->
            val dx = node.position.x - graphPosition.x
            val dy = node.position.y - graphPosition.y
            (dx * dx + dy * dy) < (45f * 45f)
        }?.key
    }

    // 3. Rendering
    val textPaint = Paint().apply {
        color = MaterialTheme.colorScheme.primary.toArgb()
        textSize = SocialGraphVisual.NodeState.INITIALFONTSIZE
        textAlign = Paint.Align.CENTER
    }
    val relevantEdgeTextPaint = Paint().apply {
        color = MaterialTheme.colorScheme.secondary.toArgb()
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }
    val nodeAnimations = remember {
        mutableStateMapOf<Long, SocialGraphVisual.NodeState>()
    }
    val edgeAnimations = remember {
        mutableStateMapOf<SocialGraph.EdgeKey, SocialGraphVisual.EdgeState>()
    }
    @Suppress("LocalVariableName") val CurrentTheme = MaterialTheme.colorScheme
    LaunchedEffect(socialGraph.selectedNode, socialGraph.renderedNodes.keys, socialGraph.edges.keys) {
        val selectedNode = socialGraph.selectedNode
        val nodeSelected = selectedNode != null
        val nodeIdsConnectedToSelectedNode = if(nodeSelected) socialGraph.edges.keys.mapNotNull { edge ->
            if(selectedNode in edge) edge.oppositeTo(selectedNode) else null
        } else emptyList()
        socialGraph.renderedNodes.keys.forEach { id ->
            val animation = nodeAnimations.getOrPut(id) {
                SocialGraphVisual.NodeState()
            }

            launch {
                animation.progress.animateTo(
                    when{
                        selectedNode == null -> SocialGraphVisual.NodeState.DEFAULT
                        id == selectedNode -> SocialGraphVisual.NodeState.SELECTED
                        id in nodeIdsConnectedToSelectedNode -> SocialGraphVisual.NodeState.CONNECTEDTOSELECTED
                        else -> SocialGraphVisual.NodeState.IRRELEVANT
                    },
                    animationSpec = SocialGraphPanel.animation
                )
            }
        }
        socialGraph.edges.keys.forEach { edge ->
            val animation = edgeAnimations.getOrPut(edge) {
                SocialGraphVisual.EdgeState()
            }

            launch {
                animation.progress.animateTo(
                    when(selectedNode) {
                        null -> SocialGraphVisual.EdgeState.DEFAULT
                        in edge -> SocialGraphVisual.EdgeState.CONNECTEDTOSELECTED
                        else -> SocialGraphVisual.EdgeState.IRRELEVANT
                    },
                    animationSpec = SocialGraphPanel.animation
                )
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(CurrentTheme.background)
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, gestureZoom, gestureRotate ->
                    val oldScale = socialGraph.scale
                    val oldAngle = socialGraph.angle

                    val oldCentroid = centroid - pan

                    // Convert the point under the previous gesture centroid
                    // from screen space back into graph space.
                    val graphPoint =
                        (oldCentroid - center)
                            .rotateBy(-oldAngle) / oldScale +
                        socialGraph.origin

                    val newScale = oldScale * gestureZoom
                    val newAngle = oldAngle + gestureRotate

                    // Keep that graph point under the new centroid.
                    socialGraph.origin =
                        graphPoint -
                                (centroid - center).rotateBy(-newAngle) / newScale

                    socialGraph.scale = newScale
                    socialGraph.angle = newAngle
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val clickedPersonId = hitTest(tapOffset, socialGraph.offset, socialGraph.scale)
                    if (clickedPersonId != null) {
                        if (socialGraph.selectedNode == clickedPersonId) {
                            openPerson(clickedPersonId)
                            return@detectTapGestures
                        }
                        socialGraph.setNodeSelection(clickedPersonId)
                    } else {
                        socialGraph.setNodeSelection(null)
                    }
                }
            }
    ) {
        nodeTransformation(socialGraph, center) {
            socialGraph.edges.forEach { (edge, weight) ->
                val displayEdge = edgeAnimations[edge] ?: return@forEach
                val (aPosition, bPosition) = socialGraph.nodePositions(edge) ?: return@forEach
                val weightRatio = weight / socialGraph.totalWeight
                drawLine(displayEdge.edgeColor(CurrentTheme), aPosition, bPosition, weightRatio.toFloat() * 200f)
            }
        }
        textTranformation(socialGraph, center) {
            socialGraph.edges.forEach { (edge, weight) ->
                val displayNode = edgeAnimations[edge] ?: return@forEach
                if (!displayNode.shouldDrawTextEdge()) return@forEach
                val edgeCenter = socialGraph.centerOf(edge) ?: return@forEach
                relevantEdgeTextPaint.color = displayNode.fontColor(CurrentTheme).toArgb()
                drawContext.canvas.nativeCanvas.drawText(
                    weight.formatMsToDuration(true),
                    edgeCenter.x,
                    edgeCenter.y + 15f,
                    relevantEdgeTextPaint
                )
            }
        }
        nodeTransformation(socialGraph, center) {
            socialGraph.renderedNodes.forEach { (personId, node) ->
                val displayNode = nodeAnimations[personId] ?: return@forEach
                drawCircle(
                    color = displayNode.nodeColor(CurrentTheme),
                    radius = displayNode.radius(),
                    center = node.position
                )
            }
        }
        textTranformation(socialGraph, center) {
            socialGraph.renderedNodes.forEach { (personId, node) ->
                val displayNode = nodeAnimations[personId] ?: return@forEach
                if(!displayNode.shouldDrawNodeName()) return@forEach
                val personName = peopleMap[personId] ?: return@forEach
                val nodeRadius = displayNode.radius()
                val screenPosition = socialGraph.graphToRotatedPosition(node.position) -
                    Offset(0f,  nodeRadius + 10f)
                textPaint.color = displayNode.fontColor(CurrentTheme).toArgb()
                withTransform({
                    scale(displayNode.fontSize() / SocialGraphVisual.NodeState.INITIALFONTSIZE, screenPosition)
                }) {
                    drawContext.canvas.nativeCanvas.drawText(
                        personName,
                        screenPosition.x,
                        screenPosition.y,
                        textPaint
                    )
                }
            }
        }
    }
}
fun DrawScope.textTranformation(socialGraph: RenderableSocialGraph, center: Offset, draw: DrawScope.()->Unit) {
    withTransform({
        translate(
            -socialGraph.offset.x * socialGraph.scale + center.x,
            -socialGraph.offset.y * socialGraph.scale + center.y
        )
        scale(socialGraph.scale, pivot = Offset.Zero)
    }, draw)
}
fun DrawScope.nodeTransformation(socialGraph: RenderableSocialGraph, center: Offset, draw: DrawScope.()->Unit) {
    withTransform({
        rotate(degrees = socialGraph.angle, pivot = center)
        translate(
            -socialGraph.offset.x * socialGraph.scale + center.x,
            -socialGraph.offset.y * socialGraph.scale + center.y
        )
        scale(socialGraph.scale, pivot = Offset.Zero)
    }, draw)
}
