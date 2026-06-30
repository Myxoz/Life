package com.myxoz.life.android.alarm

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import org.json.JSONObject

object AlarmScheduler {
    fun ensureChannelExists(context: Context){
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
    fun setAlarm(alarmTs: Long, context: Context){
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if(!alarmManager.canScheduleExactAlarms()){
                return
            }
        }
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            alarmTs ,
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
    data class AlarmSound(val name: String, val uri: Uri){
        fun toJson() = JSONObject().put("name", name).put("uri", uri.toString()).toString()
        companion object {
            fun fromJSON(json: JSONObject) = AlarmSound(json.getString("name"), json.getString("uri").toUri())
            fun fromPrefs(prefs: SharedPreferences): AlarmSound? = prefs.getString("alarmSound", null)?.let { fromJSON(
                JSONObject(it)
            ) }
        }
    }
}