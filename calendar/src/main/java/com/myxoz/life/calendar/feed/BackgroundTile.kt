package com.myxoz.life.calendar.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myxoz.life.calendar.CalendarSuper
import com.myxoz.life.core.plus
import com.myxoz.life.lifeui.providers.Theme
import java.time.Instant
import kotlin.time.Duration.Companion.hours

@Composable
internal fun BackgroundTiles(
    calendarSuper: CalendarSuper,
    hoursToday: Int,
    startOfDay: Instant,
) {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(hoursToday){ hourInDay ->
            Box(
                Modifier
                    .padding(vertical = 2.dp)
                    .background(Theme.background, RoundedCornerShape(25))
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(null, null) {
                        calendarSuper.updateStartTs(startOfDay + hourInDay.hours)
                    }
            )
        }
    }
}

