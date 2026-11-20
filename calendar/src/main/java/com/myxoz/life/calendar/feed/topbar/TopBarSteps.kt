package com.myxoz.life.calendar.feed.topbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import com.myxoz.life.calendar.feed.stepsGoal
import com.myxoz.life.lifeui.theme.OldColors
import com.myxoz.life.presentation.R
import java.time.LocalDate

@Composable
internal fun TopBarSteps(topBarAggregator: TopBarAggregator, date: LocalDate){
    val steps by topBarAggregator.getSteps(date).collectAsState()
    val stepsSnapshot = steps
    if(stepsSnapshot != null) {
        DayPill(
            painterResource(R.drawable.shoe),
            stepsSnapshot.toString(),
            (stepsSnapshot / stepsGoal).coerceIn(0.0, 1.0),
            OldColors.STEPS
        )
    }
}
