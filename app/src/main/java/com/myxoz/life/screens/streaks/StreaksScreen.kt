package com.myxoz.life.screens.streaks

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.myxoz.life.LocalScreens
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.api.extensions.StreakExtendable
import com.myxoz.life.api.extensions.StreakExtendable.StreakItem.Companion.StreakFlameActive
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.ui.getMaxTabletScreenWidth
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.ui.theme.makeContrasty
import com.myxoz.life.utils.MaterialShapes
import com.myxoz.life.utils.asUnixWeek
import com.myxoz.life.utils.between
import com.myxoz.life.utils.diagrams.chartBasedAnimation
import com.myxoz.life.utils.plural
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toShape
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.StreakViewModel
import kotlinx.coroutines.flow.map

@Composable
fun StreaksScreen(streakViewModel: StreakViewModel) {
    Box(
        Modifier
            .background(Theme.background)
        ,
        contentAlignment = Alignment.TopCenter
    ) {
        val topPadding = 50.dp
        val fullPictureSize = 200.dp
        val extendedHeight = topPadding * 2 + fullPictureSize
        val smallTopPadding = 5.dp
        val smallPictureSize = 60.dp
        val scrollHeight = extendedHeight - smallTopPadding - smallPictureSize
        val scrollLength = with(LocalDensity.current){ streakViewModel.scrollState.value.toDp() }
        val progress = 1 - min(scrollLength, scrollHeight) / scrollHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(streakViewModel.scrollState)
            ,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(Modifier.height(windowPadding.calculateTopPadding()))
            Spacer(Modifier.height(extendedHeight))
            val allStreaks by streakViewModel.allStreaks.collectAsState()
            allStreaks.forEach { (_, cached) ->
                StreakItemComposable(streakViewModel, cached.value ?: return@forEach)
            }
            val screens = LocalScreens.current
            Box(
                Modifier
                    .clip(MaterialShapes.Cookie12Sided.toShape())
                    .rippleClick{
                        screens.openStreakEditing(
                            StreakExtendable.StreakItem(
                                0L,
                                true,
                                0L,
                                StreakExtendable.Timespan.Day,
                                StreakExtendable.StreakTarget.EventTarget(
                                    false,
                                    EventType.Empty,
                                    listOf(),
                                    listOf(),
                                    null
                                ),
                                "",
                                null
                            )
                        )
                    }
                    .background(Theme.surfaceContainer)
                    .border(1.dp, Theme.outlineVariant, MaterialShapes.Cookie12Sided.toShape())
                    .padding(10.dp)
            ) {
                Icon(
                    painterResource(R.drawable.add),
                    "Add",
                    Modifier
                        .size(30.dp)
                    ,
                    Theme.outlineVariant
                )
            }
            Spacer(Modifier.height(windowPadding.calculateBottomPadding()))
        }
        Box(
            Modifier
                .setMaxTabletWidth()
                .background(Theme.background)
                .padding(
                    top = windowPadding.calculateTopPadding(),
                )
        ) {
            val startPadding = 2.dp
            val conf = LocalWindowInfo.current.containerDpSize
            val screenWidth = getMaxTabletScreenWidth()
            val smallerScreenDimension = min(screenWidth, conf.height)
            val blur = 30.dp
            val pictureSize = between(progress, smallPictureSize, fullPictureSize)
            Box(
                Modifier
                    .padding(
                        start = between(progress, startPadding, smallerScreenDimension / 2 - pictureSize / 2),
                        top = between(progress, smallTopPadding, topPadding),
                        bottom = between(progress, smallTopPadding, topPadding),
                    )
                    .size(pictureSize)
            ) {
                StreakFlameActive(between(progress, blur / 4, blur))
            }
            Box(
                Modifier
                    .height(smallPictureSize)
                    .offset(
                        between(progress, startPadding + smallPictureSize + startPadding, conf.width),
                        smallTopPadding
                    )
                    .alpha(1-progress)
                ,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Streaks",
                    style = TypoStyle(OldColors.Streaks.MAIN, FontSize.XLARGE, FontFamily.Display),
                )
            }
        }
    }
}

@Composable
private fun StreakItemComposable(streakViewModel: StreakViewModel, streak: StreakExtendable.StreakItem) {
    val screens = LocalScreens.current
    Column(
        Modifier
            .setMaxTabletWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Theme.surfaceContainer, RoundedCornerShape(30.dp))
            .rippleClick{
                screens.openStreak(streak)
            }
            .border(1.dp, Theme.outlineVariant, RoundedCornerShape(30.dp))
            .padding(horizontal = 20.dp, vertical = 15.dp)
        ,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .background(streak.target.bgColor.makeContrasty(Theme.surfaceContainer), MaterialShapes.Sunny.toShape())
                    .padding(7.dp)
                ,
                contentAlignment = Alignment.Center
            ) {
               Icon(
                   painterResource( streak.target.icon),
                   null,
                   Modifier.fillMaxSize(),
                   streak.target.iconColor
               )
            }
            Text(streak.title, style = TypoStyle(Theme.primary, FontSize.MEDIUM), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val timestampraw by remember(streak) {
            streakViewModel.dayFlow.map {
                if(streak.timespan == StreakExtendable.Timespan.Day) it.toEpochDay() else it.asUnixWeek().week
            }
        }.collectAsState(null)
        val timestamp = timestampraw
        val current = if(timestamp != null) {
            val currentRaw by streakViewModel.getCurrentStreakValue(streak, timestamp)
                .collectAsState()
            currentRaw
        } else null
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if(timestamp != null) {
                    val currentStreak by streakViewModel.getCurrentStreak(streak, timestamp).collectAsState()
                    val previousRaw by streakViewModel.getCurrentStreak(streak, timestamp - 1).collectAsState()
                    val previous = previousRaw ?: 0
                    val current = currentStreak ?: 0
                    val color = if(current == 0 && previous != 0)
                        OldColors.Streaks.NOT_YET_REACHED
                    else if(current > previous)
                        OldColors.Streaks.MAIN
                    else
                        OldColors.Streaks.ENDED
                    Text(
                        (if(current != 0) current else previous).toString(),
                        style = TypoStyle(
                            color,
                            FontSize.DISPLAY,
                            FontFamily.Display
                        )
                    )
                    Text(
                        streak.timespan.displayName+current.plural(streak.timespan.plural),
                        style = TypoStyle(color, FontSize.XLARGE, FontFamily.Display)
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
            ) {
                Text("${streak.timespan.now}:", style = TypoStyle(Theme.secondary, FontSize.SMALLM))
                Row(
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        if(current!=null) streak.target.format(current) else "",
                        style = TypoStyle(
                            streak.target.bgColor.makeContrasty(Theme.surfaceContainer),
                            FontSize.XLARGE, FontFamily.Display)
                    )
                    Text(
                        " / ${streak.formatThreshold()}",
                        style = TypoStyle(Theme.primary, FontSize.MEDIUM)
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val anim = remember { Animatable(0f) }
            LaunchedEffect(current) {
                anim.animateTo(current?.toFloat()?:0f, chartBasedAnimation)
            }
            val currentWidth = anim.value.takeIf { current != null }
            Box(
                Modifier
                    .height(30.dp)
                    .fillMaxWidth()
                    .background(Theme.surfaceContainerHighest, CircleShape)
                    .clip(CircleShape)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(currentWidth?.div(streak.threshold)?.coerceIn(0f, 1f)?:0f)
                        .fillMaxHeight()
                        .background(streak.target.bgColor.makeContrasty(Theme.surfaceContainer), CircleShape)
                )
            }
        }
    }
}