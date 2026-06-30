package com.myxoz.life.ui.alarm.screens

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.myxoz.life.Theme
import com.myxoz.life.android.alarm.AlarmScheduler
import com.myxoz.life.android.alarm.AlarmScheduler.getSystemAlarms
import com.myxoz.life.ui.AlarmUI
import com.myxoz.life.ui.alarm.AlarmRepo
import com.myxoz.life.ui.feed.dayoverview.edgeToEdgeGradient
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.collectAsMutableState
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.windowPadding

@Composable
fun AlarmUI.AlarmSoundSettings(alarmRepo: AlarmRepo){
    val context = LocalContext.current
    val allSound = remember {
        getSystemAlarms(context)
    }
    var selectedAlarm by alarmRepo.alarmSound.collectAsMutableState()
    val mediaPlayer = remember { MediaPlayer().apply {
        isLooping = true
    } }
    fun previewSound(sound: AlarmScheduler.AlarmSound){
        if(!alarmRepo.hasWarnedFullSound) {
            Toast.makeText(context, "Der Alarm spielt immer auf voller Lautstärke", Toast.LENGTH_LONG).show()
            alarmRepo.hasWarnedFullSound = true
        }
        mediaPlayer.apply {
            reset()
            setDataSource(context, sound.uri)
            prepare()
            start()
        }
    }
    DisposableEffect(Unit) {
        onDispose { mediaPlayer.release() }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Theme.background)
    ) {
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .edgeToEdgeGradient(Theme.background, windowPadding)
            ,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = windowPadding,
        ) {
            items(allSound, {it.uri}){
                Row(
                    Modifier
                        .setMaxTabletWidth()
                        .rippleClick{
                            selectedAlarm = it
                            previewSound(it)
                        }
                    ,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        it.name,
                        Modifier
                            .weight(1f)
                        ,
                        style = TypoStyle(Theme.primary, FontSize.MEDIUM)
                    )
                    RadioButton(
                        selectedAlarm == it,
                        {
                            selectedAlarm = it
                            previewSound(it)
                        },
                        colors = RadioButtonColors(
                            Theme.primary,
                            Theme.outline,
                            Theme.primary,
                            Theme.outline,
                        )
                    )
                }
            }
        }
    }
}