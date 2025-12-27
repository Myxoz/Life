package com.myxoz.life.autodetect

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.content.edit
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.events.ProposedEvent
import com.myxoz.life.viewmodels.Settings
import org.json.JSONObject

object AutoDetect {
    private data class AutoDetectType(val spk: String, val feature: Settings.Features.Feature, val getSessions: suspend ()->List<ProposedEvent>)

    /**
     * Always call from IO Dispatcher, running on main will cause lag
     */
    suspend fun autoDetectEvents(context: Context, settings: Settings, db: StorageManager): List<ProposedEvent>{
        val autoDetectList = arrayOf(
            AutoDetectType(
                AutoDetectSleep.SPK,
                settings.features.autoDetectSleep,
            ) {
                AutoDetectSleep.getSessions(context)
            },
            AutoDetectType(
                AutoDetectCall.SPK,
                settings.features.autoDetectCalls,
            ){
                AutoDetectCall.getSessions(context, db)
            }
        )
        val prefs = context.getSharedPreferences("autodetect", MODE_PRIVATE)
        val events = mutableListOf<ProposedEvent>()
        for (autodetectable in autoDetectList) {
            if(!autodetectable.feature.hasAssured()) continue
            val oldSessions = prefs.getStringSet(autodetectable.spk, null)
                ?.mapNotNull{ if(it.startsWith("{")) ProposedEvent.getProposedEventByJson(JSONObject(it)) else null }
                ?.sortedBy { it.start } ?: listOf()
            val detectedSessions = autodetectable.getSessions().sortedBy { it.start }
            val newSessions = detectedSessions
                .filter { session ->
                    oldSessions.none {
                        it.start == session.start && it.end == session.end
                    }
                }
            val first = detectedSessions.firstOrNull()?.start ?: 0L
            prefs.edit {
                putStringSet(
                    autodetectable.spk,
                    oldSessions
                        .filter {
                            it.start >= first // Delete Session from a timeframe outside the detection range
                        }
                        .map {
                            it.toJson().toString()
                        }
                        .toSet()
                )
            }
            events.addAll(newSessions)
        }
        return events
    }
    interface AutoDetectEvent {
        fun ingoreAutoDetectable(event: ProposedEvent, spk: String, context: Context) {
            val prefs = context.getSharedPreferences("autodetect", MODE_PRIVATE)
            val data = (prefs.getStringSet(spk, null) ?: setOf<String>()).toMutableSet()
            data.add(event.toJson().toString())
            prefs.edit {
                putStringSet(spk, data)
            }
        }
    }
}