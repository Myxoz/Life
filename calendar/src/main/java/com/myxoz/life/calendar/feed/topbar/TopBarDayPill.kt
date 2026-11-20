package com.myxoz.life.calendar.feed.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myxoz.life.calendar.feed.events.daySummaryHeight
import com.myxoz.life.lifeui.providers.Theme
import com.myxoz.life.lifeui.theme.FontSize
import com.myxoz.life.lifeui.theme.OldColors
import com.myxoz.life.lifeui.theme.TypoStyle

@Composable
internal fun DayPill(imageVector: Painter, text: String, progress: Double, color: Color){
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
                    .fillMaxWidth(progress.toFloat())
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
            Text(text, style = TypoStyle(Theme.primary, FontSize.SMALL).copy(fontWeight = FontWeight.Bold))
        }
    }
}
