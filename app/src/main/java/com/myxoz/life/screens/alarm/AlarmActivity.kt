package com.myxoz.life.screens.alarm

import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.core.content.edit
import com.myxoz.life.LocalColors
import com.myxoz.life.repositories.MainApplication
import com.myxoz.life.utils.systemColorScheme
import com.myxoz.life.viewmodels.AlarmViewModel
import com.myxoz.life.viewmodels.ProfileInfoModel

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

        val repos = (this.application as? MainApplication)?.repositories ?: return
        startAlarmSound(AlarmViewModel.Companion.AlarmSound.fromPrefs(repos.prefs))

        setContent {
            val colorScheme = systemColorScheme()
            CompositionLocalProvider(
                LocalColors provides colorScheme
            ) {
                AlarmingScreen(
                    repos,
                    { stopAlarm() },
                    { snoozeAlarm() },
                    remember { ProfileInfoModel(repos) }
                )
            }
        }
    }

    private fun startAlarmSound(preferedSound: AlarmViewModel.Companion.AlarmSound?) {
        // Get default alarm URI
        val alertUri = resolveAlarmUri(application, preferedSound?.uri)

        mediaPlayer = MediaPlayer().apply {
            setDataSource(applicationContext, alertUri)
            setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
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
        vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, 0))
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
        val repos = (this.application as? MainApplication)?.repositories ?: return
        repos.prefs.edit {
            putLong("nextAlarmTs", -1L)
        }
        finishAndRemoveTask()
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
