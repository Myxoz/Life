package com.myxoz.life.screens.streaks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myxoz.life.LocalScreens
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.api.extensions.StreakExtendable
import com.myxoz.life.repositories.utils.Cached
import com.myxoz.life.screens.feed.dayoverview.edgeToEdgeGradient
import com.myxoz.life.screens.feed.dayoverview.getMonthByCalendarMonth
import com.myxoz.life.screens.feed.dayoverview.getWeekDayByInt
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.ui.theme.makeContrasty
import com.myxoz.life.utils.LayeredCircularProgressIndicator
import com.myxoz.life.utils.MaterialShapes
import com.myxoz.life.utils.UnixWeek
import com.myxoz.life.utils.collectAsMutableState
import com.myxoz.life.utils.def
import com.myxoz.life.utils.matchInstrinsicHeight
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.utils.toShape
import com.myxoz.life.utils.toSp
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.StreakViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun StreakFullScreen(streakViewModel: StreakViewModel, id: Long) {
    val streakRaw by streakViewModel.getStreak(id).collectAsState(null)
    val streakR = streakRaw
    if(streakR == Cached.Null) {
        Text("No Streak with this ID")
        return
    }
    if(streakR == null || streakR.value == null) {
        return
    }
    val streak = streakR.value
    var days by streakViewModel.days.collectAsMutableState()
    val today by streakViewModel.dayFlow.collectAsState(LocalDate.now())
    LaunchedEffect(Unit) {
        launch {
            streakViewModel.dayFlow.collect {
                val now = streak.timespan.now()
                if(now !in days) days = listOf(now) + days
            }
        }
        snapshotFlow { streakViewModel.scrollStateSubScreen.layoutInfo }
            .collect { layoutInfo ->
                val totalItems = layoutInfo.totalItemsCount
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@collect

                if (lastVisible >= totalItems - 5) {
                    val lastDay = days.lastOrNull() ?: return@collect
                    days += (lastDay.minus(6)..lastDay.minus(1)).reversed()
                }
            }
    }
    val dayHeight = getDaySize()
    Box(
        Modifier
            .background(Theme.background)
        ,
        contentAlignment = Alignment.BottomCenter
    ) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .edgeToEdgeGradient(Theme.background, windowPadding)
            ,
            state = streakViewModel.scrollStateSubScreen,
            horizontalAlignment = Alignment.CenterHorizontally,
            reverseLayout = true
        ) {
            item {
                Spacer(
                    Modifier.height(
                        windowPadding.calculateBottomPadding() + FontSize.XLARGE.size.toDp() + 10.dp + 15.dp
                    )
                )
            }
            items(days, {it}) { timestamp ->
                when(streak.timespan) {
                    StreakExtendable.Timespan.Day -> {
                        Row(
                            Modifier
                                .setMaxTabletWidth()
                            ,
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SingleDayComposable(LocalDate.ofEpochDay(timestamp), today)
                            val value by streakViewModel.getCurrentStreakValue(streak, timestamp).collectAsState()
                            val maxWidth by streakViewModel.getMaxValueFor(streak).collectAsState()
                            Box(
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val fontSize = FontSize.SMALL.size.toDp()
                                val progress = (value?.toFloat().def(0f) / maxWidth).coerceIn(0f, 1f)
                                val contrastBg = streak.target.bgColor.makeContrasty(Theme.surfaceContainer)
                                Box(
                                    Modifier
                                        .fillMaxWidth(progress)
                                        .background(contrastBg, CircleShape)
                                        .height(fontSize)
                                        .align(Alignment.CenterStart)
                                )
                                Box(
                                    Modifier.layout { measurable, constraints ->
                                        val parentWidth = constraints.maxWidth
                                        val textPlaceable = measurable.measure(constraints)
                                        val textWidth = textPlaceable.width
                                        val clampedX = (progress * parentWidth - textWidth / 2f)
                                            .coerceIn(0f, (parentWidth - textWidth).toFloat())
                                        layout(parentWidth, textPlaceable.height) {
                                            textPlaceable.place(clampedX.toInt(), 0)   // vertical position handled by parent's CenterStart
                                        }
                                    }
                                ) {
                                    Text(
                                        streak.target.format(value.def(0)),
                                        style = TypoStyle(streak.target.iconColor, FontSize.SMALL),
                                        modifier = Modifier
                                            .background(contrastBg, CircleShape)
                                            .padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(dayHeight)
                                ,
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                val currentStreak by streakViewModel.getCurrentStreak(streak, timestamp).collectAsState()
                                Row(
                                    Modifier
                                        .padding(vertical = 5.dp)
                                        .background(Theme.surfaceContainer, RoundedCornerShape(10.dp))
                                        .padding(start = 10.dp)
                                    ,
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val currentValueR by streakViewModel.getCurrentStreakValue(streak, timestamp).collectAsState()
                                    val currentValue = currentValueR.def(0)
                                    val displayVal = abs(streak.threshold - currentValue)
                                    val goalAchieved = streak.target.continuesStreak(currentValue, streak.threshold, streak.atLeast)
                                    val displayText = when {
                                        goalAchieved && streak.atLeast -> "Erreicht"
                                        !goalAchieved && streak.atLeast -> "Noch ${streak.target.format(displayVal)} nötig"
                                        goalAchieved && !streak.atLeast -> "${streak.target.format(displayVal)} verbleibend"
                                        else -> "${streak.target.format(displayVal)} zu viel"
                                    }
                                    Text(
                                        displayText,
                                        Modifier
                                            .weight(1f)
                                        ,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = streak.target.bgColor,
                                        fontSize = (dayHeight / 3.5f).toSp(),
                                        fontFamily = if(goalAchieved) FontFamily.Display.family else null,
                                    )
                                    Box(
                                        Modifier
                                            .size(dayHeight)
                                    )
                                }
                                Box(
                                    Modifier
                                        .height(dayHeight)
                                        .padding(end = dayHeight / 2)
                                        .width(3.dp)
                                        .background(Theme.outlineVariant)
                                )
                                Box(
                                    Modifier
                                        .size(dayHeight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if(currentStreak.def(0) > 0){
                                        StreakExtendable.StreakItem.StreakFlameActive(dayHeight / 5)
                                        Text(
                                            currentStreak.toString(),
                                            Modifier
                                                .offset(y = dayHeight / 15),
                                            color = if(currentStreak.def(0) > 0) OldColors.Streaks.BURNED else Theme.onPrimaryContainer,
                                            fontSize = 20.dp.toSp(),
                                            fontFamily = FontFamily.Display.family
                                        )
                                    } else {
                                        Icon(
                                            painterResource(R.drawable.streak_icon),
                                            "Ended Streak",
                                            Modifier.fillMaxSize(.8f),
                                            Theme.primaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                    StreakExtendable.Timespan.Week -> {
                        Row(
                            Modifier
                                .setMaxTabletWidth()
                                .height(IntrinsicSize.Min)
                            ,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val week = UnixWeek(timestamp).containedDays()
                            val values = week.map {
                                streakViewModel.getCurrentStreakValue(streak, it.toEpochDay(), StreakExtendable.Timespan.Day).collectAsState()
                            }
                            Column {
                                week.forEach {
                                    SingleDayComposable(it, today)
                                }
                            }
                            val maxValue by streakViewModel.getMaxValueFor(streak, StreakExtendable.Timespan.Day).collectAsState()
                            val textMeasurer = rememberTextMeasurer(7)
                            Spacer(Modifier.width(5.dp))
                            Canvas(
                                Modifier
                                    .height(dayHeight*7)
                                    .weight(1f)
                            ) {
                                val width = size.width
                                val height = size.height
                                val oneHeightUnit = height / 14
                                val unit = width/maxValue
                                fun centered(index: Int) = values.getOrNull(index)?.value.def(0).def(0)*unit to oneHeightUnit*(index*2+1)
                                drawPath(
                                    Path()
                                        .apply {
                                            moveTo(centered(0).first, oneHeightUnit)
                                            values.forEachIndexed { index, state ->
                                                val centerd = centered(index)
                                                lineTo(centerd.first, centerd.second)
                                                moveTo(centerd.first, centerd.second)
                                            }
                                        },
                                    streak.target.bgColor,
                                    style = Stroke(6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                                val horizontalPaddingPx = 10.dp.toPx()
                                val verticalPaddingPx = 3.dp.toPx()
                                values.forEachIndexed { index, state ->
                                    val value = state.value
                                    if(value != null &&  value != 0L){
                                        val measure = textMeasurer.measure(
                                            streak.target.format(value),
                                            TypoStyle(streak.target.iconColor, FontSize.SMALL)
                                        )
                                        val centerd = centered(index)
                                        val textWidth = measure.size.width.toFloat()
                                        val textHeight = measure.size.height.toFloat()

                                        val bgWidth = textWidth + horizontalPaddingPx * 2
                                        val bgHeight = textHeight + verticalPaddingPx * 2

                                        val bgTopLeft = Offset(
                                            (centerd.first - bgWidth / 2).coerceIn(0f, width - bgWidth),
                                            (centerd.second - bgHeight / 2).coerceIn(0f, height - bgHeight)
                                        )
                                        drawRoundRect(
                                            streak.target.bgColor,
                                            bgTopLeft,
                                            Size(bgWidth, bgHeight),
                                            CornerRadius(bgHeight / 2, bgHeight / 2)
                                        )
                                        drawText(
                                            measure,
                                            topLeft = Offset(
                                                bgTopLeft.x + horizontalPaddingPx,
                                                bgTopLeft.y + verticalPaddingPx
                                            )
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(5.dp))
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    Modifier
                                        .background(Theme.surfaceContainer, RoundedCornerShape(20.dp))
                                        .width(100.dp + 70.dp + 20.dp /* Horpadding */)
                                        .height(100.dp + 40.dp /* Vertpadding */)
                                )
                                Box(
                                    Modifier
                                        .align(Alignment.CenterEnd)
                                        .matchInstrinsicHeight()
                                        .padding(end = 45.dp)
                                        .width(3.dp)
                                        .background(Theme.outlineVariant)
                                )
                                Row(
                                    Modifier
                                        .padding(horizontal = 10.dp,  vertical = 20.dp)
                                    ,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val currentStreak by streakViewModel.getCurrentStreak(streak, timestamp).collectAsState()
                                    val currentValue by streakViewModel.getCurrentStreakValue(streak, timestamp).collectAsState()
                                    Box(
                                        Modifier
                                            .size(100.dp)
                                    ) {
                                        val strokeWidth = 10.dp
                                        LayeredCircularProgressIndicator(
                                            currentValue.def(0).toFloat() / streak.threshold,
                                            Modifier
                                                .fillMaxSize(),
                                            streak.target.bgColor.makeContrasty(Theme.surfaceContainer),
                                            Theme.primaryContainer,
                                            strokeWidth,
                                            strokeWidth / 2,
                                        )
                                        Column(
                                            Modifier
                                                .fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                streak.target.format(currentValue.def(0)),
                                                style = TypoStyle(
                                                    streak.target.bgColor.makeContrasty(Theme.surfaceContainer),
                                                    FontSize.MEDIUM,
                                                    FontFamily.Display
                                                )
                                            )
                                            Text(
                                                " / "+streak.formatThreshold(),
                                                style = TypoStyle(
                                                    Theme.primary,
                                                    FontSize.MEDIUMM
                                                )
                                            )
                                        }
                                    }
                                    Box(
                                        Modifier
                                            .size(70.dp)
                                        ,
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if(currentStreak.def(0) > 0){
                                            StreakExtendable.StreakItem.StreakFlameActive(10.dp)
                                            Text(
                                                currentStreak.toString(),
                                                Modifier
                                                    .offset(y = dayHeight / 15),
                                                color = if(currentStreak.def(0) > 0) OldColors.Streaks.BURNED else Theme.onPrimaryContainer,
                                                fontSize = 25.dp.toSp(),
                                                fontFamily = FontFamily.Display.family
                                            )
                                        } else {
                                            Icon(
                                                painterResource(R.drawable.streak_icon),
                                                "Ended Streak",
                                                Modifier.fillMaxSize(.8f),
                                                Theme.primaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(
                    Modifier.height(windowPadding.calculateTopPadding())
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .height(15.dp)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Theme.background.copy(.75f)
                            )
                        )
                    )
            )
            Row(
                Modifier
                    .background(Theme.background.copy(.75f))
                    .setMaxTabletWidth()
                ,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    Modifier
                        .size(FontSize.XLARGE.size.toDp() + 10.dp)
                        .background(
                            streak.target.bgColor.makeContrasty(Theme.surfaceContainer),
                            MaterialShapes.Sunny.toShape()
                        )
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(streak.target.icon),
                        null,
                        Modifier.fillMaxSize(),
                        streak.target.iconColor
                    )
                }
                Text(streak.title, Modifier.weight(1f), style = TypoStyle(Theme.primary, FontSize.MEDIUM))
                val screens = LocalScreens.current
                Box(
                    Modifier
                        .size(FontSize.XLARGE.size.toDp() + 10.dp)
                        .clip(MaterialShapes.Cookie12Sided.toShape())
                        .background(Theme.primaryContainer)
                        .rippleClick{
                            screens.openStreakEditing(streak)
                        }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.edit),
                        null,
                        Modifier.fillMaxSize(),
                        Theme.onPrimaryContainer
                    )
                }
            }
            Box(
                Modifier
                    .height(windowPadding.calculateBottomPadding())
                    .fillMaxWidth()
                    .background(
                        Theme.background.copy(.75f)
                    )
            )
        }
    }
}

@Composable
fun getDaySize() = (FontSize.MLARGE.size.value + FontSize.SMALLM.size.value).sp.times(1.5f).toDp()
@Composable
private fun SingleDayComposable(date: LocalDate, today: LocalDate) {
    val textColor = if(date == today) Theme.onPrimary else Theme.secondary
    val background = if(date == today) Theme.primary else Color.Transparent
    val screens = LocalScreens.current
    val shape = MaterialShapes.Slanted.toShape()
    Column(
        Modifier
            .size(getDaySize())
            .background(background, shape)
            .clip(shape)
            .rippleClick {
                screens.openCalendarAt(date, false)
            }
        ,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = TypoStyle(
                textColor,
                FontSize.MLARGE,
                FontFamily.Display
            )
        )
        Text(
            "${getWeekDayByInt(date.dayOfWeek.value - 1)}${if(date.dayOfMonth == 1) " / ${getMonthByCalendarMonth(date.monthValue - 1)}" else ""}",
            style = TypoStyle(textColor, FontSize.SMALLM)
        )
    }
}