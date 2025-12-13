package com.myxoz.life.autodetect

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.content.edit
import com.myxoz.life.events.ProposedEvent
import com.myxoz.life.viewmodels.Settings

fun autoDetectEvents(context: Context, settings: Settings): List<ProposedEvent>{
    val prefs = context.getSharedPreferences("autodetect", MODE_PRIVATE)
    val events = mutableListOf<ProposedEvent>()
    if(settings.features.autoDetectSleep.has.value) {
        val ignoredSleepSessions = prefs.getStringSet("declined_sleep", null)
            ?.toMutableList()?.apply { sortedBy { it.split(";")[0].toLong() } } ?: listOf<String>()
        val detectedSleepSessions = getAutoDetectedSleepSession(context)
        val newSleepSessions = detectedSleepSessions.filter { session ->
            ignoredSleepSessions.none {
                val (start, end) = it.split(";")
                start.toLong() == session.start && end.toLong() == session.end
            }
        }
        prefs.edit {
            putStringSet(
                "declined_sleep",
                ignoredSleepSessions
                    .filter {
                        it.split(";")[0].toLong() >= (detectedSleepSessions.getOrNull(0)?.start ?: 0L)
                    }.toSet()
            )
        }
        events.addAll(newSleepSessions)
    }
    return events
}