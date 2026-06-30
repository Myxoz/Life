package com.myxoz.life.ui.alarm

import android.app.AlarmManager
import android.content.Context
import com.myxoz.life.android.alarm.AlarmScheduler
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.utils.subscribeToColdFlow
import com.myxoz.life.utils.syncNullableToPrefs
import com.myxoz.life.utils.syncToPrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class AlarmRepo(val dbInterface: DatabaseInterface) {
    var hasWarnedFullSound = false
    val nextScheduled = MutableStateFlow(dbInterface.prefs.getLong("nextAlarmTs", -1)).apply{
        syncToPrefs(dbInterface.appScope, dbInterface.prefs, "nextAlarmTs", Long::class)
    }
    val alarmSound = MutableStateFlow(AlarmScheduler.AlarmSound.fromPrefs(dbInterface.prefs)).apply {
        map { it?.toJson() }.syncNullableToPrefs(dbInterface.appScope, dbInterface.prefs, "alarmSound", String::class)
    }
    val nextEvent = MutableStateFlow<SyncedEvent?>(null)
    val minutesToGetReady = MutableStateFlow(dbInterface.prefs.getLong("minutesToGetReady", 30L)).apply {
        syncToPrefs(dbInterface.appScope, dbInterface.prefs, "minutesToGetReady", Long::class)
    }
    val minuteFlow = flow {
        emit(System.currentTimeMillis())
        while (true) {
            delay(60 * 1000L - System.currentTimeMillis() % (60 * 1000L))
            emit(System.currentTimeMillis())
        }
    }.subscribeToColdFlow(dbInterface.appScope, System.currentTimeMillis())
    suspend fun refresh(){
        // This is a bit of a Band-Aid fix, this will refresh the nextAlarmTs on screen opening,
        // this might be problematic due to snooze not notifying the viewmodel. FIXME
        nextScheduled.value = dbInterface.prefs.getLong("nextAlarmTs", -1L)
        nextEvent.value = dbInterface.calendarInterface.getNonSleepEventAfter(System.currentTimeMillis())
    }
    fun setAlarm(eventTs: Long){
        val alarmTs = eventTs - minutesToGetReady.value * 1000L * 60
        AlarmScheduler.setAlarm(alarmTs, dbInterface.context)
        nextScheduled.value = alarmTs
    }
    fun removeAlarm(){
        val alarmManager = dbInterface.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(AlarmScheduler.alarmIntent(dbInterface.context))
        nextScheduled.value = -1L
    }
    init {
        AlarmScheduler.ensureChannelExists(dbInterface.context)
    }
}