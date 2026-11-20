package com.myxoz.life.calendar.feed.topbar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.myxoz.life.calendar.feed.events.dateBarHeight
import com.myxoz.life.calendar.feed.events.daySummaryHeight
import com.myxoz.life.calendar.feed.events.paddingBetweenSummaryAndDate
import com.myxoz.life.lifecore.NavPath
import com.myxoz.life.lifeui.providers.LocalNavController
import com.myxoz.life.lifeui.reusedcompose.rippleClick
import java.time.LocalDate

@Composable
internal fun DayTopBar(
    topBarAggregator: TopBarAggregator,
    date: LocalDate,
) {
    val nav = LocalNavController.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .rippleClick {
                nav.navigate(NavPath.FullDay.Overview(date))
            }
    ) {
        Box(
            Modifier
                .height(dateBarHeight)
                .fillMaxWidth()
            ,
            contentAlignment = Alignment.Center
        ) {
            DateTopBar(date)
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
            TopBarBirthday(topBarAggregator, date)
            TopBarScreenTime(topBarAggregator, date)
            TopBarSteps(topBarAggregator, date)
        }
    }
}
