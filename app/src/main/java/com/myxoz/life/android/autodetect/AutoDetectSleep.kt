package com.myxoz.life.android.autodetect

import android.content.Context
import android.content.SharedPreferences
import android.icu.util.Calendar
import com.myxoz.life.events.SleepEvent
import com.myxoz.life.ui.options.getUsageDataSessions
import com.myxoz.life.utils.SharedPrefsUtils.edit
import com.myxoz.life.utils.roundToNearest15Min

object AutoDetectSleep{
    const val SPK = "declined_sleep"
    fun getSessions(prefs: SharedPreferences, context: Context): List<SleepEvent> {
        val after = prefs.getLong(SPK+"_after", 0)
        val sessions = getUsageDataSessions(context, after, System.currentTimeMillis())
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
        resultList.lastOrNull()?.let {
            prefs.edit {
                putLong(SPK+"_after", it.end)
            }
        }

        return resultList // We sort this in AutoDetect
    }
}