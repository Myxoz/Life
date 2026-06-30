package com.myxoz.life.ui.alarm.screens.composables

import android.icu.util.Calendar
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myxoz.life.Theme
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.repositories.supermodule.CrossRepoSuper
import com.myxoz.life.ui.AlarmUI
import com.myxoz.life.ui.LocalScreensProvider
import com.myxoz.life.ui.alarm.AlarmRepo
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.diagrams.chartBasedAnimation
import com.myxoz.life.utils.formatDayTime
import com.myxoz.life.utils.formatMsToDuration

@Composable
fun AlarmUI.NextEventInformation(alarmRepo: AlarmRepo, nextEvent: SyncedEvent, screens: LocalScreensProvider?, crossRepoSuper: CrossRepoSuper){
    Column(
        Modifier
            .fillMaxWidth()
        ,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        val calendar = remember { Calendar.getInstance() }
        val currentTime by alarmRepo.minuteFlow.collectAsState()
        Spacer(Modifier)
        Text("Nächstes Event:", style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
        RenderEventPreview(nextEvent, screens, crossRepoSuper)
        Text("Um ${nextEvent.raw.start.formatDayTime(calendar)} in", style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
        val animatedTime = remember { Animatable(0f) }
        LaunchedEffect(currentTime, nextEvent.raw.start) {
            animatedTime.animateTo((nextEvent.raw.start - currentTime).toFloat(), chartBasedAnimation)
        }
        Text(animatedTime.value.toLong().formatMsToDuration(true), style = TypoStyle(Theme.primary, FontSize.XXLARGE, FontFamily.Display))
    }
}
