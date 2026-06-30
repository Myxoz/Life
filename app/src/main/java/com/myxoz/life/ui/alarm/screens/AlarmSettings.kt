package com.myxoz.life.ui.alarm.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.myxoz.life.Theme
import com.myxoz.life.repositories.supermodule.CrossRepoSuper
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.ui.AlarmUI
import com.myxoz.life.ui.alarm.AlarmRepo
import com.myxoz.life.ui.alarm.screens.composables.NextEventInformation
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.windowPadding

@Composable
fun AlarmUI.AlarmingScreen(repos: DatabaseInterface, dimiss: ()->Unit, snooze: ()->Unit, crossRepoSuper: CrossRepoSuper){
    val alarmRepo: AlarmRepo = remember { AlarmRepo(repos) }
    LaunchedEffect(Unit) {
        alarmRepo.refresh()
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Theme.primary)
            .padding(windowPadding)
        ,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Spacer(Modifier.weight(1f))
        Text("Guten Morgen!", style = TypoStyle(Theme.onPrimary, FontSize.XLARGE, FontFamily.Display))
        Spacer(Modifier.weight(1f))
        val nextRawEvent by alarmRepo.nextEvent.collectAsState()
        val nextEvent = nextRawEvent
        if(nextEvent != null) {
            Box(
                Modifier
                    .setMaxTabletWidth()
                    .background(Theme.primaryContainer, RoundedCornerShape(30.dp))
                    .padding(20.dp)
                ,
            )  {
                NextEventInformation(alarmRepo, nextEvent, null, crossRepoSuper)
            }
        }
        Spacer(Modifier.weight(3f))
        Box(
            Modifier
                .setMaxTabletWidth()
                .background(Theme.primaryContainer, RoundedCornerShape(30.dp))
                .clip(RoundedCornerShape(30.dp))
                .rippleClick{ dimiss() }
                .padding(vertical = 30.dp)
            ,
            contentAlignment = Alignment.Center
        ) {
            Text("Alarm beenden", style = TypoStyle(Theme.onPrimaryContainer, FontSize.LARGE))
        }
        Spacer(Modifier.weight(.5f))
        Box(
            Modifier
                .setMaxTabletWidth()
                .background(Theme.primaryContainer, RoundedCornerShape(30.dp))
                .clip(RoundedCornerShape(30.dp))
                .rippleClick{ snooze() }
                .padding(vertical = 60.dp)
            ,
            contentAlignment = Alignment.Center
        ) {
            Text("5m Snooze", style = TypoStyle(Color.White, FontSize.XXLARGE, FontFamily.Display))
        }
        Spacer(Modifier.weight(.5f))
    }
}
