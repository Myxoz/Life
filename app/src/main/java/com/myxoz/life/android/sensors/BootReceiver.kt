package com.myxoz.life.android.sensors

import android.Manifest
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.myxoz.life.repositories.MainApplication
import com.myxoz.life.viewmodels.AlarmViewModel


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                context.startForegroundService(Intent(context, StepsService::class.java))
            }
            val notificationAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED else false
            val alarmAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms() else  true
            val nextAlarmTs = context.getSharedPreferences(MainApplication.SPK, MODE_PRIVATE).getLong("nextAlarmTs", -1L)
            if(notificationAccess && alarmAccess && nextAlarmTs > System.currentTimeMillis() /* RIP if the alarm was while the system was off */) {
                AlarmViewModel.setAlarm(nextAlarmTs, context)
            }
        }
    }
}
