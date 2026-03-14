package com.myxoz.life.viewmodels

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
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.repositories.utils.subscribeToColdFlow
import com.myxoz.life.screens.alarm.AlarmReceiver
import com.myxoz.life.utils.syncLongToPrefs
import com.myxoz.life.utils.syncStringToPrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject

class AlarmViewModel(val repos: AppRepositories): ViewModel() {
    var hasWarnedFullSound = false
    val nextScheduled = MutableStateFlow(repos.prefs.getLong("nextAlarmTs", -1)).apply {
        viewModelScope.launch {
            this@apply.collect {
                repos.prefs.edit {
                    putLong("nextAlarmTs", it)
                }
            }
        }
    }
    val alarmSound = MutableStateFlow(AlarmSound.fromPrefs(repos.prefs)).apply {
        map { it?.toJson() }.syncStringToPrefs(viewModelScope, repos.prefs, "alarmSound")
    }
    val nextEvent = MutableStateFlow<SyncedEvent?>(null)
    val minutesToGetReady = MutableStateFlow(repos.prefs.getLong("minutesToGetReady", 30L)).apply {
        syncLongToPrefs(viewModelScope, repos.prefs, "minutesToGetReady")
    }
    val minuteFlow = flow {
        emit(System.currentTimeMillis())
        while (true){
            delay(60*1000L-System.currentTimeMillis()%(60*1000L))
            emit(System.currentTimeMillis())
        }
    }.subscribeToColdFlow(viewModelScope, System.currentTimeMillis())
    suspend fun refresh(){
        // This is a bit of a Band-Aid fix, this will refresh the nextAlarmTs on screen opening,
        // this might be problematic due to snooze not notifying the viewmodel
        nextScheduled.value = repos.prefs.getLong("nextAlarmTs", -1L)
        nextEvent.value = repos.calendarRepo.getNextEventAfter(System.currentTimeMillis())
    }
    fun setAlarm(settings: Settings, eventTs: Long){
        if(!settings.features.lifeAlarmClock.hasAssured()) return
        val alarmTs = eventTs - minutesToGetReady.value * 1000L * 60
        setAlarm(alarmTs, repos.context)
        nextScheduled.value = alarmTs
    }
    fun removeAlarm(){
        val alarmManager = repos.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(alarmIntent(repos.context))
        nextScheduled.value = -1L
    }
    companion object {
        data class AlarmSound(val name: String, val uri: Uri){
            fun toJson() = JSONObject().put("name", name).put("uri", uri.toString()).toString()
            companion object {
                fun fromJSON(json: JSONObject) = AlarmSound(json.getString("name"), json.getString("uri").toUri())
                fun fromPrefs(prefs: SharedPreferences): AlarmSound? = prefs.getString("alarmSound", null)?.let { fromJSON( JSONObject(it)) }
            }
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
    }
    init {
        val channel = NotificationChannel(
            "alarm_channel",
            "Alarm",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.setSound(null, null) // we control sound manually
        channel.enableVibration(true)

        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

        val manager = repos.context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}