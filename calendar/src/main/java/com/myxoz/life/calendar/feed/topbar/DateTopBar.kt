package com.myxoz.life.calendar.feed.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.myxoz.life.lifeui.providers.Theme
import com.myxoz.life.lifeui.shapes.MaterialShapes
import com.myxoz.life.lifeui.shapes.toShape
import com.myxoz.life.lifeui.theme.FontFamily
import com.myxoz.life.lifeui.theme.FontSize
import com.myxoz.life.lifeui.theme.TypoStyle
import com.myxoz.life.lifeui.time.updatingToday
import com.myxoz.life.presentation.time.WeekDays
import java.time.LocalDate


@Composable
internal fun DateTopBar(date: LocalDate){
    val today by updatingToday()
    val isToday by remember {
        derivedStateOf { today == date }
    }
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
            WeekDays.getWeekDayByInt(date.dayOfWeek.value - 1),
            style = TypoStyle(
                if (isToday) Theme.onPrimary else Theme.secondary,
                FontSize.SMALLM
            )
                .run { this.copy(fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal) }
        )
    }
}