package com.myxoz.life.autodetect

import android.content.Context
import android.icu.util.Calendar
import com.myxoz.life.events.SleepEvent
import com.myxoz.life.options.getUsageDataSessions

fun getAutoDetectedSleepSession(context: Context): List<SleepEvent> {
    val sessions = getUsageDataSessions(context, 0L, System.currentTimeMillis())
    if(sessions.isEmpty()) return listOf()
    var lastTimestamp = sessions.first().end
    val calendar = Calendar.getInstance()
    val resultList = mutableListOf<SleepEvent>()
    for(session in sessions) {
        if(session.start - lastTimestamp > 4*3600*1000L) { // Detect sleep after 4h inactivity
            val sleepFrom = lastTimestamp
            val sleepTo = session.start
            calendar.timeInMillis = sleepFrom
            if((calendar.get(Calendar.HOUR_OF_DAY) + 12) % 24 > 8 /* After 20 o'clock */) {
                calendar.timeInMillis = sleepTo
                if((calendar.get(Calendar.HOUR_OF_DAY)) < 14 /* Before 14 o'clock */) {
                    resultList.add(SleepEvent(sleepFrom.roundToNearest15Min(), sleepTo.roundToNearest15Min(), false, usl = false))
                }
            }
        }
        lastTimestamp = session.end
    }
    return resultList.sortedBy { it.start }
}
fun Long.roundToNearest15Min(): Long {
    val block = 15 * 60 * 1000L // 15 minutes in ms
    return ((this + block / 2) / block) * block
}