package com.myxoz.life.calendar.feed.topbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import com.myxoz.life.calendar.feed.screenTimeGoal
import com.myxoz.life.lifeui.theme.OldColors
import com.myxoz.life.presentation.R
import com.myxoz.life.presentation.time.formatMsToDuration
import java.time.LocalDate


@Composable
internal fun TopBarScreenTime(topBarAggregator: TopBarAggregator, date: LocalDate) {
    val screenTime by topBarAggregator.getScreentime(date).collectAsState()
    val screenTimeSnapshot = screenTime
    if (screenTimeSnapshot != null) {
        DayPill(
            painterResource(R.drawable.screentime),
            screenTimeSnapshot.formatMsToDuration(true),
            (screenTimeSnapshot / screenTimeGoal).coerceIn(0.0, 1.0),
            OldColors.SCREENTIME
        )
    }
}