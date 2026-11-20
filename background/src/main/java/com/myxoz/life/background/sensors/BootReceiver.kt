package com.myxoz.life.background.sensors

import android.Manifest
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.myxoz.life.background.ContainsAlarmScheduler
import java.time.Instant


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                context.startForegroundService(Intent(context, StepsService::class.java))
            }
            val notificationAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED else false
            val alarmAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms() else  true
            val alarmScheduler = (context.applicationContext as ContainsAlarmScheduler).alarmScheduler
            alarmScheduler.ensureChannelExists(context)
            val nextAlarmTs = alarmScheduler.getNextAlarm()
            if(notificationAccess && alarmAccess && nextAlarmTs != null && nextAlarmTs > Instant.now() /* RIP if the alarm was while the system was off */) {
                alarmScheduler.setAlarm(nextAlarmTs, context)
            }
        }
    }
}
