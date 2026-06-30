package com.myxoz.life.ui.alarm.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalScreens
import com.myxoz.life.LocalSettings
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.ui.AlarmUI
import com.myxoz.life.ui.NavPath
import com.myxoz.life.ui.alarm.AlarmRepo
import com.myxoz.life.ui.alarm.screens.composables.NextEventInformation
import com.myxoz.life.ui.alarm.screens.composables.RenderClockVisual
import com.myxoz.life.ui.feed.fullscreenevent.TimeBasedVisualTransformation
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.collectAsMutableState
import com.myxoz.life.utils.formatMinutesToVisual
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.Settings

@Composable
fun AlarmUI.AlarmScreen(alarmRepo: AlarmRepo) {
    LaunchedEffect(Unit) {
        alarmRepo.refresh()
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Theme.background)
            .padding(windowPadding)
        ,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val nav = LocalNavController.current
        val rawNextEvent by alarmRepo.nextEvent.collectAsState()
        val nextEvent = rawNextEvent
        if(nextEvent == null) {
            Text("Kein nächstes Event", style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
            return@Column
        }
        NextEventInformation(alarmRepo, nextEvent, LocalScreens.current, LocalScreens.current.appRepos.crossRepoSuper)
        Spacer(Modifier.weight(1f))
        AlarmUI.RenderClockVisual(alarmRepo, nextEvent.raw.start)
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .setMaxTabletWidth()
            ,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment =  Alignment.CenterVertically
        ) {
            Text("", style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
        }
        Row(
            Modifier
                .setMaxTabletWidth()
            ,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment =  Alignment.CenterVertically
        ) {
            val alarmSound by alarmRepo.alarmSound.collectAsState()
            Text("Alarmton", style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
            Spacer(Modifier.width(10.dp))
            Row(
                Modifier
                    .background(Theme.secondaryContainer, CircleShape)
                    .clip(CircleShape)
                    .rippleClick{
                        nav.navigate(NavPath.Menu.Alarm.ALARM_SOUND_SETTINGS)
                    }
                    .padding(horizontal = 15.dp, vertical = 10.dp)
                    .weight(1f, false)
                ,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(alarmSound?.name ?: "Systemstandard", Modifier.weight(1f, false), style = TypoStyle(Theme.onSecondaryContainer, FontSize.SMALLM), overflow = TextOverflow.Ellipsis, maxLines = 1)
                Icon(painterResource(R.drawable.settings), "Settings", Modifier.size(FontSize.SMALLM.size.toDp()), Theme.onSecondaryContainer)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .setMaxTabletWidth()
            ,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment =  Alignment.CenterVertically
        ) {
            Text("Zeit zum Aufstehen", style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
            var timeToGetReadyMinutes by alarmRepo.minutesToGetReady.collectAsMutableState()
            val focusManager = LocalFocusManager.current
            val textMeasurer = rememberTextMeasurer()
            var text = timeToGetReadyMinutes.formatMinutesToVisual()
            val textWidth = with(LocalDensity.current){
                textMeasurer.measure(
                    TimeBasedVisualTransformation.Companion.toTransformed(text),
                    style = TypoStyle(Theme.onSecondary, FontSize.MEDIUM),
                ).size.width.toDp()
            }
            BasicTextField(
                text,
                {
                    text = it.trimStart('0')
                    timeToGetReadyMinutes = TimeBasedVisualTransformation.Companion.displayMinutesToMinutes(it).toLong()
                },
                textStyle = TypoStyle(Theme.onSecondary, FontSize.MEDIUM),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions{
                    focusManager.clearFocus(true)
                },
                modifier = Modifier
                    .background(Theme.secondary, RoundedCornerShape(25))
                    .padding(horizontal = 15.dp, vertical = 10.dp)
                    .height(FontSize.MEDIUM.size.toDp())
                    .width(if(textWidth < 1.dp) 1.dp else textWidth),
                cursorBrush = SolidColor(Theme.onSecondary),
                visualTransformation = TimeBasedVisualTransformation(),
            )
        }
        Spacer(Modifier.height(10.dp))
        val settings = LocalSettings.current
        val hasAlarmsPermission by settings.has(Settings.Feature.LifeAlarmClock).collectAsState()
        val nextScheduledTs by alarmRepo.nextScheduled.collectAsState()
        val alarmIsSet = nextScheduledTs > System.currentTimeMillis()
        Box(
            Modifier
                .setMaxTabletWidth()
                .padding(10.dp)
                .background(if(!hasAlarmsPermission) Theme.primary else if(!alarmIsSet) Theme.primary else Theme.secondary, CircleShape)
                .clip(CircleShape)
                .rippleClick{
                    if(!settings.hasAssured(Settings.Feature.LifeAlarmClock)){
                        nav.navigate(NavPath.Menu.More.Settings.PERMISSIONS)
                    } else if(!alarmIsSet) {
                        alarmRepo.setAlarm(nextEvent.raw.start)
                    } else {
                        alarmRepo.removeAlarm()
                    }
                }
                .padding(vertical = 20.dp)
            ,
            contentAlignment = Alignment.Center
        ) {
            Text(if(!hasAlarmsPermission) "Zu Wecker Berechtigung" else if(!alarmIsSet) "Wecker stellen" else "Alarm aufhalten", style = TypoStyle(Theme.onPrimary, FontSize.LARGE, FontFamily.Display))
        }
    }
}