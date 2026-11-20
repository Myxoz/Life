package com.myxoz.life.feature.socialgraph

import android.widget.ToggleButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import com.myxoz.life.Theme
import com.myxoz.life.ui.ToggleButton
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.MaterialShapes
import com.myxoz.life.utils.atEndAsMillis
import com.myxoz.life.utils.atStartAsMillis
import com.myxoz.life.utils.combinedRippleClick
import com.myxoz.life.utils.def
import com.myxoz.life.utils.formatToDate
import com.myxoz.life.utils.nextOrFirst
import com.myxoz.life.utils.previousOrLast
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.utils.toLocalDate
import com.myxoz.life.utils.toShape
import com.myxoz.life.utils.transformed
import com.myxoz.life.utils.updatingToday
import com.myxoz.life.utils.windowPadding
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.pow
import kotlin.ranges.coerceAtMost
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun BoxScope.SocialGraphButtons(socialGraphRepo: SocialGraphRepo) {
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .setMaxTabletWidth()
            .padding(bottom = windowPadding.calculateBottomPadding())
        ,
    ) {
        val panel = socialGraphRepo.controlPanel
        val endDate by panel.endDate()
        val panelVisibilityProgress by animateFloatAsState(if(panel.isVisible) 1f else 0f, SocialGraphPanel.animation)
        LaunchedEffect(panel.isPlaying) {
            while(panel.isPlaying) {
                delay((1.1f.pow(panel.playingSpeedExponent) * 1000L / 5).toLong().milliseconds)
                panel.setEndDate(endDate.plusDays(1).coerceAtMost(LocalDate.now()))
            }
        }
        AnimatedVisibility(
            panel.isVisible,
            enter = fadeIn(SocialGraphPanel.animation) + slideInVertically(SocialGraphPanel.animationIntOffset) { it/5 },
            exit = fadeOut(SocialGraphPanel.animation) + slideOutVertically(SocialGraphPanel.animationIntOffset) { it/5 }
        ) {
            val boxShape = RoundedCornerShape(ToggleButton.ICONBUTTONSIZE * 0.3f + 10.dp)
            Column(
                Modifier
                    .alpha(panelVisibilityProgress)
                    .background(Theme.surfaceContainer, boxShape)
                    .border(panelVisibilityProgress.dp, Theme.outlineVariant, boxShape)
                    .padding(10.dp)
            ) {
                val startDate: Long? by socialGraphRepo.earliestEventFlow.collectAsState()
                val today by updatingToday()
                val zone = ZoneId.systemDefault()
                Box(
                    Modifier
                        .fillMaxWidth()
                        .pointerInput(today, startDate) {
                            var startProgress = 0.0
                            var totalDrag = 0f
                            val startMs = startDate?.toLocalDate(zone).def(today).atStartAsMillis(zone)
                            val endMs = today.atEndAsMillis(zone)
                            val range = endMs - startMs
                            if (range <= 0L) return@pointerInput

                            detectHorizontalDragGestures({
                                val currentMs = panel.endDate(today).atEndAsMillis(zone)
                                startProgress = (currentMs - startMs).toDouble() / range
                                totalDrag = 0f
                            }) { _, dragAmount ->
                                totalDrag += dragAmount
                                val progress = (startProgress + totalDrag / size.width.toDouble()).coerceIn(0.0, 1.0)
                                val newMs = startMs + (progress * range).toLong()
                                panel.setEndDate(newMs.toLocalDate(zone))
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    val startDateBup = startDate
                    val todayMs = today.atEndAsMillis(zone)
                    val currentMs = panel.endDate(today).atEndAsMillis(zone)
                    val dayProgress = if(startDateBup != null) {
                        val startMs = startDateBup.toLocalDate(zone).atStartAsMillis(zone)
                        (currentMs - startMs).toFloat() / (todayMs - startMs)
                    } else 0f
                    Row(
                        Modifier
                            .height(3.dp)
                            .align(Alignment.CenterStart)
                            .padding(horizontal = 10.dp)
                        ,
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        val durationWidthPercentage by animateFloatAsState(
                            if(startDateBup != null) {
                                val durationMs = panel.chartScale.millis()
                                if(durationMs != null) {
                                    val startMs = startDateBup.toLocalDate(zone).atStartAsMillis(zone)
                                    durationMs.toFloat() / (todayMs - startMs)
                                } else 1f
                            } else 1f,
                            SocialGraphPanel.animation
                        )
                        val firstBlockWidth = (dayProgress - durationWidthPercentage).coerceAtLeast(0f)
                        Box(
                            Modifier
                                .fillMaxWidth(firstBlockWidth)
                                .height(2.dp)
                                .padding(horizontal = 1.dp)
                                .background(Theme.outline, CircleShape)
                        )
                        Box(
                            Modifier
                                .fillMaxWidth(durationWidthPercentage.coerceAtMost(dayProgress) / (1-firstBlockWidth))
                                .height(5.dp)
                                .padding(horizontal = 3.dp)
                                .background(Theme.primary, CircleShape)
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .padding(horizontal = 1.dp)
                                .background(Theme.outline, CircleShape)
                        )
                    }
                    Box(
                        Modifier
                            .align(BiasAlignment(dayProgress * 2 - 1, 0f))
                            .size(15.dp)
                            .align(Alignment.CenterStart)
                            .background(Theme.primary, CircleShape)
                    )
                }
                Spacer(Modifier.height(5.dp))
                Row(
                    Modifier
                        .padding(horizontal = 10.dp)
                    ,
                    verticalAlignment = Alignment.Top
                ) {
                    val zone = ZoneId.systemDefault()
                    val days = panel.chartScale.days()
                    val startDate = (startDate?.toLocalDate(zone) ?: endDate).atStartAsMillis(zone)
                    val timelineInMillis = (endDate.atStartAsMillis(zone) - startDate)
                    val displayedStartDate by animateFloatAsState(
                        if(days == null) 0f else {
                            val millis = (endDate.minusDays(days)).atStartAsMillis(zone).coerceAtLeast(startDate) - startDate
                            millis.toFloat() / timelineInMillis
                        },
                        SocialGraphPanel.animation
                    )
                    val timelineStartDate = (startDate + (displayedStartDate * timelineInMillis)).toLong().toLocalDate(zone)
                    Text(
                        timelineStartDate.formatToDate(),
                        Modifier.weight(1f),
                        style = TypoStyle(Theme.outline, FontSize.SMALL)
                    )
                    val animationProgress by animateFloatAsState(if(panel.isPlaying) 1f else 0f)
                    val morph = remember {
                        val rotationMatrix = Matrix().apply { rotateZ(90f) }
                        Morph(
                            MaterialShapes.Triangle.transformed(rotationMatrix),
                            MaterialShapes.Bun.transformed(rotationMatrix),
                        )
                    }
                    val path = remember(animationProgress) {
                        morph.toPath(animationProgress).asComposePath()
                    }
                    val playColor by animateColorAsState(
                        if(!panel.isPlaying) Theme.outline else Theme.primary,
                        SocialGraphPanel.animationColor
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val iconSize = FontSize.XLARGE.size.toDp()
                        Icon(
                            painterResource(R.drawable.fast_rewind),
                            "Fast rewind",
                            Modifier
                                .size(iconSize)
                                .clip(CircleShape)
                                .rippleClick{
                                    panel.playingSpeedExponent++
                                },
                            playColor
                        )
                        Box(Modifier
                            .size(iconSize)
                            .rippleClick{
                                panel.isPlaying = !panel.isPlaying
                            }
                            .background(playColor, path.toShape())
                        )
                        Icon(
                            painterResource(R.drawable.fast_forward),
                            "Fast forward",
                            Modifier
                                .size(iconSize)
                                .clip(CircleShape)
                                .rippleClick{
                                    panel.playingSpeedExponent--
                                },
                            playColor
                        )
                    }
                    Text(
                        endDate.formatToDate(),
                        Modifier.weight(1f),
                        style = TypoStyle(Theme.outline, FontSize.SMALL),
                        textAlign = TextAlign.End
                    )
                }
                Spacer(Modifier.height(50.dp + 20.dp))
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(10.dp)
            ,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .graphicsLayer{
                        translationX = (-size.width + ToggleButton.ICONBUTTONSIZE.toPx()) * panelVisibilityProgress
                    },
                contentAlignment = Alignment.CenterEnd
            ) {
                ToggleButton.WithIcon(panel.addMyself, painterResource(R.drawable.graph_hub)) {
                    panel.addMyself = !panel.addMyself
                }
            }
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(Theme.primary)
                    .height(50.dp)
                    .combinedRippleClick({
                        panel.chartScale = panel.chartScale.previousOrLast()
                    }){
                        panel.chartScale = panel.chartScale.nextOrFirst()
                    }
                    .padding(horizontal = 20.dp)
                ,
                contentAlignment = Alignment.Center
            ) {
                val fontSize = FontSize.MEDIUM.size.toDp()
                SocialGraphPanel.SocialGraphScale.entries.forEach { scale ->
                    val multiplier = when(socialGraphRepo.controlPanel.chartScale) {
                        scale -> 1f
                        scale.nextOrFirst(), scale.previousOrLast() -> 0.5f
                        else -> 0f
                    }
                    val alpha by animateFloatAsState(multiplier)
                    val offsetMult by animateFloatAsState((scale.ordinal - panel.chartScale.ordinal).toFloat())
                    Text(
                        scale.displayName,
                        Modifier
                            .offset(y = fontSize * offsetMult)
                            .alpha(alpha)
                            .scale(1 - abs(offsetMult)*.5f), style = TypoStyle(Theme.onPrimary, FontSize.MEDIUM))
                }
            }
            Box(
                Modifier
                    .weight(1f)
                    .graphicsLayer{
                        translationX = (size.width - ToggleButton.ICONBUTTONSIZE.toPx()) * panelVisibilityProgress
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                ToggleButton.WithIcon(panel.isVisible, painterResource(R.drawable.history)) {
                    panel.isVisible = !panel.isVisible
                }
            }
        }
    }
}
