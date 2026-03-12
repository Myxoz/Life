package com.myxoz.life.viewmodels

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.repositories.utils.subscribeToColdFlow
import com.myxoz.life.screens.alarm.AlarmReceiver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class AlarmViewModel(val repos: AppRepositories): ViewModel() {
    val nextScheduled = MutableStateFlow(repos.prefs.getLong("nextAlarmTs", -1)).apply {
        viewModelScope.launch {
            this@apply.collect {
                repos.prefs.edit {
                    putLong("nextAlarmTs", it)
                }
            }
        }
    }
    val nextEvent = MutableStateFlow<SyncedEvent?>(null)
    val minutesToGetReady = MutableStateFlow(30)
    val minuteFlow = flow {
        emit(System.currentTimeMillis())
        while (true){
            delay(60*1000L-System.currentTimeMillis()%(60*1000L))
            emit(System.currentTimeMillis())
        }
    }.subscribeToColdFlow(viewModelScope, System.currentTimeMillis())
    suspend fun refreshNextEvent(){
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
    }
    init {
        val channel = NotificationChannel(
            "alarm_channel",
            "Alarm",
            NotificationManager.IMPORTANCE_HIGH
        )

        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

        val manager = repos.context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}