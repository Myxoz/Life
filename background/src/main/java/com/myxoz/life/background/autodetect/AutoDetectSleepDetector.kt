package com.myxoz.life.background.autodetect

import android.Manifest
import androidx.annotation.RequiresPermission
import com.myxoz.life.core.roundToNearest15Min
import com.myxoz.life.core.toInstant
import com.myxoz.life.lifecore.android.ScreenTimeCollector
import com.myxoz.life.storage.events.SleepEvent
import com.myxoz.life.storage.events.additionals.EventTime
import java.time.ZoneId

internal object AutoDetectSleepDetector {
    val SPK = AutoDetect.AutoDetectables.AutoDetectSleep
    @RequiresPermission(Manifest.permission.PACKAGE_USAGE_STATS)
    fun getSessions(helper: AutoDetect.AutoDetectHelper): List<SleepEvent> {
        val after = helper.getLastTimestamp(SPK)
        val sessions = ScreenTimeCollector.getRawSessions(helper.context, after?.toEpochMilli() ?: 0L, System.currentTimeMillis())
        if(sessions.isEmpty()) return listOf()
        var lastTimestamp = sessions.first().end
        val resultList = mutableListOf<SleepEvent>()
        val zone = ZoneId.systemDefault()
        for(session in sessions) {
            if(session.start - lastTimestamp > 4*3600*1000L) { // Detect sleep after 4h inactivity
                val from = lastTimestamp.toInstant().atZone(zone)
                if(from.hour > 20 /* After 20 o'clock */) {
                    val to = session.start.toInstant().atZone(zone)
                    if(to.hour < 14 /* Before 14 o'clock */) {
                        resultList.add(
                            SleepEvent(
                                EventTime(
                                    from.toInstant().roundToNearest15Min(),
                                    to.toInstant().roundToNearest15Min(),
                                    unspecifiedStart = false,
                                    unspecifiedLength = false
                                )
                            )
                        )
                    }
                }
            }
            lastTimestamp = session.end
        }
        resultList.lastOrNull()?.let {
            helper.putLastTimestamp(SPK, it.eventTime.end)
        }

        return resultList // We sort this in AutoDetect
    }
}