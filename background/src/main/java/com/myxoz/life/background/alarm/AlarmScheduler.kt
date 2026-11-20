package com.myxoz.life.background.alarm

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import com.myxoz.life.storage.interfaces.aggregator.AlarmAggregator
import com.myxoz.life.storage.stored.AlarmSound
import java.time.Instant

class AlarmScheduler(
    private val alarmAggregator: AlarmAggregator
) {
    fun ensureChannelExists(context: Context) {
        val channel = NotificationChannel(
            "alarm_channel",
            "Alarm",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.setSound(null, null) // we control sound manually
        channel.enableVibration(true)

        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
    fun alarmIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            161,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    fun getNextAlarm() = alarmAggregator.nextAlarmTs
    fun setAlarm(alarmTs: Instant, context: Context){
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if(!alarmManager.canScheduleExactAlarms()){
                return
            }
        }
        alarmAggregator.nextAlarmTs = alarmTs
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            alarmTs.toEpochMilli(),
            alarmIntent(context)
        )
    }
    fun getSystemAlarms(context: Context): List<AlarmSound> {
        val manager = RingtoneManager(context)
        manager.setType(RingtoneManager.TYPE_ALARM)
        val cursor = manager.cursor
        val list = mutableListOf<AlarmSound>()
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = manager.getRingtoneUri(cursor.position)
            list.add(AlarmSound(title, uri))
        }
        cursor.close()
        return list
    }
}