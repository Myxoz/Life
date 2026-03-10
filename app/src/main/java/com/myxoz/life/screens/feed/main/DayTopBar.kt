package com.myxoz.life.screens.feed.main

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myxoz.life.LocalScreens
import com.myxoz.life.LocalSettings
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.screens.feed.dayoverview.getWeekDayByInt
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.ui.theme.TypoStyleOld
import com.myxoz.life.utils.MaterialShapes
import com.myxoz.life.utils.def
import com.myxoz.life.utils.formatMsToDuration
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toShape
import com.myxoz.life.viewmodels.CalendarViewModel
import java.time.LocalDate

@Composable
fun DayTopBar(
    calendarViewModel: CalendarViewModel,
    date: LocalDate,
){
    val settings = LocalSettings.current
    val screens = LocalScreens.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .rippleClick {
                screens.openDayOverview(date)
            }
    ) {
        val today by calendarViewModel.todayFlow.collectAsState()
        val isToday = today == date
        Box(
            Modifier
                .height(dateBarHeight)
                .fillMaxWidth()
            ,
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .then(
                        if (isToday)
                            Modifier
                                .aspectRatio(1f)
                                .background(
                                    Theme.primary,
                                    MaterialShapes.Slanted.toShape()
                                )
                        else
                            Modifier
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    date.dayOfMonth.toString(),
                    style = TypoStyle(
                        if (isToday) Theme.onPrimary else Theme.secondary,
                        FontSize.MLARGE,
                        FontFamily.Display
                    )
                        .run { this.copy(fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal) }
                )
                Text(
                    getWeekDayByInt(date.dayOfWeek.value - 1),
                    style = TypoStyle(
                        if (isToday) Theme.onPrimary else Theme.secondary,
                        FontSize.SMALLM
                    )
                        .run { this.copy(fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }
        Box(
            Modifier
                .height(paddingBetweenSummaryAndDate)
        )
        Row(
            Modifier
                .height(daySummaryHeight)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val daysSummaries by calendarViewModel.getDaySummary(date).collectAsState()
            val birthdayPeople by calendarViewModel.getPeopleWithBirthdayAt(date).collectAsState()
            repeat(birthdayPeople.size) {
                Icon(
                    painterResource(R.drawable.birthday),
                    "Birthday",
                    Modifier.fillMaxHeight(),
                    Theme.secondary
                )
            }
            val screentimePermission by settings.features.screentime.has.collectAsState()
            val stepsPermission by settings.features.stepCounting.has.collectAsState()
            val listenedScreentime by calendarViewModel.getScreentime(date).collectAsStateWithLifecycle(0L)

            if (screentimePermission && (isToday || daysSummaries!=null)) {
                DayPill(
                    painterResource(R.drawable.screentime),
                    listenedScreentime.formatMsToDuration(true),
                    (listenedScreentime / screenTimeGoal).coerceIn(0f, 1f),
                    OldColors.SCREENTIME
                )
            }
            val steps by calendarViewModel.steps.collectAsState()
            val displayedSteps = (if(isToday) steps else daysSummaries?.steps?.toLong()).def(0L)
            if (stepsPermission && (daysSummaries != null || isToday)) {
                DayPill(
                    painterResource(R.drawable.shoe),
                    displayedSteps.toString(),
                    (displayedSteps / stepsGoal).coerceIn(0f, 1f),
                    OldColors.STEPS
                )
            }
        }
    }
}
@Composable
private fun DayPill(imageVector: Painter, text: String, progress: Float, color: Color){
    Box(
        Modifier
            .height(daySummaryHeight)
            .background(OldColors.DAYPILLBG, CircleShape)
            .widthIn(40.dp)
            .clip(CircleShape)
    ) {
        Box(
            Modifier
                .matchParentSize()
        ) {
            Box(
                Modifier
                    .background(color, CircleShape)
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
            )
        }
        Row(
            Modifier
                .fillMaxSize()
                .widthIn(40.dp)
                .padding(horizontal = 2.dp)
                .padding(end = 5.dp)
            ,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(imageVector, null,
                Modifier
                    .size(daySummaryHeight)
                    .padding(2.dp),
                OldColors.PRIMARYFONT
            )
            Spacer(Modifier.width(5.dp))
            Text(text, style = TypoStyleOld(FontColor.PRIMARY, FontSize.SMALL).copy(fontWeight = FontWeight.Bold))
        }
    }
}
