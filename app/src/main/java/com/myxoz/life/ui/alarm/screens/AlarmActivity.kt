package com.myxoz.life.ui.alarm.screens

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.edit
import com.myxoz.life.LocalColors
import com.myxoz.life.android.MainApplication
import com.myxoz.life.android.alarm.AlarmScheduler
import com.myxoz.life.ui.AlarmUI
import com.myxoz.life.utils.systemColorScheme

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

        val repos = (this.application as? MainApplication)?.dbInterface ?: return
        val appRepos = (this.application as? MainApplication)?.appRepos ?: return
        startAlarmSound(AlarmScheduler.AlarmSound.fromPrefs(repos.prefs))

        setContent {
            val colorScheme = systemColorScheme()
            CompositionLocalProvider(
                LocalColors provides colorScheme
            ) {
                AlarmUI.AlarmingScreen(
                    repos,
                    { stopAlarm() },
                    { snoozeAlarm() },
                    appRepos.crossRepoSuper,
                )
            }
        }
    }

    private fun startAlarmSound(preferedSound: AlarmScheduler.AlarmSound?) {
        // Get default alarm URI
        val alertUri = resolveAlarmUri(application, preferedSound?.uri)

        mediaPlayer = MediaPlayer().apply {
            setDataSource(applicationContext, alertUri)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            isLooping = true
            try {
                prepare()
                start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Vibrate the phone
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 500, 500)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }
    fun resolveAlarmUri(context: Context, preferred: Uri?): Uri {
        if (preferred != null) {
            val ringtone = RingtoneManager.getRingtone(context, preferred)
            if (ringtone != null) {
                return preferred
            }
        }

        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrator.cancel() // stop looping vibration
        killNotification()
        val repos = (this.application as? MainApplication)?.dbInterface ?: return
        repos.prefs.edit {
            putLong("nextAlarmTs", -1L)
        }
        finishAndRemoveTask()
    }

    private fun snoozeAlarm() {
        stopAlarm()
        killNotification()

        val snoozeTime = System.currentTimeMillis() + 2 * 60 * 1000
        AlarmScheduler.setAlarm(snoozeTime, this)

        val repos = (this.application as? MainApplication)?.dbInterface ?: return
        repos.prefs.edit {
            putLong("nextAlarmTs", snoozeTime)
        }
        finishAndRemoveTask()
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