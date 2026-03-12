package com.myxoz.life.screens.alarm

import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.edit
import com.myxoz.life.LocalColors
import com.myxoz.life.R
import com.myxoz.life.repositories.MainApplication
import com.myxoz.life.utils.systemColorScheme
import com.myxoz.life.viewmodels.AlarmViewModel

class AlarmActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // wake the screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )

        startAlarmSound()

        setContent {
            val repos = (this.application as? MainApplication)?.repositories ?: return@setContent
            val colorScheme = systemColorScheme()
            CompositionLocalProvider(
                LocalColors provides colorScheme
            ) {
                AlarmingScreen(
                    repos,
                    { stopAlarm() },
                    { snoozeAlarm() }
                )
            }
        }
    }

    private fun startAlarmSound() {
        // BIG TODO THIS IS NOT LEGAL; SOOORY;  THISIS TESTING
        // https://pixabay.com/sound-effects/search/alarm%20clock%20music/
        // https://pixabay.com/sound-effects/musical-star-dust-alarm-clock-114194/
        // Big shoutout: https://pixabay.com/users/lesiakower-25701529/
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

        mediaPlayer = MediaPlayer.create(this, R.raw.wakeup).apply {
            isLooping = true
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            start()
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        killNotification()
        val repos = (this.application as? MainApplication)?.repositories ?: return
        repos.prefs.edit {
            putLong("nextAlarmTs", -1L)
        }
        finish()
    }

    private fun snoozeAlarm() {
        stopAlarm()
        killNotification()

        val snoozeTime = System.currentTimeMillis() + 2 * 60 * 1000
        AlarmViewModel.setAlarm(snoozeTime, this)

        val repos = (this.application as? MainApplication)?.repositories ?: return
        repos.prefs.edit {
            putLong("nextAlarmTs", snoozeTime)
        }
        finish()
    }

    private fun killNotification(){
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(161)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
