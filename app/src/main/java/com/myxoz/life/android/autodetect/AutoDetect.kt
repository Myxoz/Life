package com.myxoz.life.android.autodetect

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.core.content.edit
import com.myxoz.life.dbwrapper.people.ReadPeopleDao
import com.myxoz.life.events.ProposedEvent
import com.myxoz.life.utils.def
import com.myxoz.life.viewmodels.Settings
import org.json.JSONObject

object AutoDetect {
    private data class AutoDetectFromSensors(val spk: String, val feature: Settings.Feature, val getSessions: suspend ()->List<ProposedEvent>)
    private data class AutoDetectFromStorage(val spk: String)

    /** Always call from IO Dispatcher, running on main will cause lag */
    suspend fun autoDetectEvents(context: Context, settings: Settings.CompositionSettings, db: ReadPeopleDao): List<ProposedEvent> {
        val autoDetectList = arrayOf(
            AutoDetectFromSensors(
                AutoDetectSleep.SPK,
                Settings.Feature.AutoDetectSleep,
            ) {
                AutoDetectSleep.getSessions(context)
            },
            AutoDetectFromSensors(
                AutoDetectCall.SPK,
                Settings.Feature.AutoDetectCalls,
            ){
                AutoDetectCall.getSessions(context, db)
            }
        )
        val prefs = context.getSharedPreferences(AUTODETECT_PREFS, MODE_PRIVATE)
        val events = mutableListOf<ProposedEvent>()
        for (autodetectable in autoDetectList) {
            if(!settings.hasAssured(autodetectable.feature)) continue
            val oldSessions = prefs.getStringSet(autodetectable.spk, null)
                ?.mapNotNull{ if(it.startsWith("{")) ProposedEvent.fromJSON(JSONObject(it)) else null }
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
        events.addAll(getStoredAutoDetectEvent(prefs))
        return events
    }
    fun getStoredAutoDetectEvent(prefs: SharedPreferences): List<ProposedEvent> {
        val autoDetectList = arrayOf(
            AutoDetectFromStorage(
                getStored(AutoDetectCall.SPK),
            ),
        )
        return autoDetectList.flatMap {
            prefs.getStringSet(it.spk, setOf<String>()).def(setOf<String>()).mapNotNull { json -> ProposedEvent.fromJSON(JSONObject(json)) }
        }
    }
    interface AutoDetectEvent {
        // We might need to construct the ProposedEvent with an "Ignore Proposed"-Switch, defaulting to {}
        // This is a bruteforce approch currently only costing 2x
        fun ingoreAutoDetectable(event: ProposedEvent, spk: String, prefs: SharedPreferences, mightBeStored: Boolean) {
            if(mightBeStored && prefs.contains(getStored(spk))) {
                val set = prefs.getStringSet(getStored(spk), setOf<String>()) ?: setOf<String>()
                var remove: String? = null
                set.any { string ->
                    val proposed = ProposedEvent.fromJSON(JSONObject(string))
                    val hasElement = proposed.end == event.end && proposed.start == event.start && proposed.type == event.type
                    if(hasElement) remove = string
                    hasElement
                }
                if(remove!=null) {
                    val cp = set.toMutableSet()
                    cp.remove(remove)
                    prefs.edit {
                        putStringSet(getStored(spk), cp)
                    }
                    return
                }
            }
            val data = (prefs.getStringSet(spk, null) ?: setOf<String>()).toMutableSet()
            data.add(event.toJson().toString())
            prefs.edit {
                putStringSet(spk, data)
            }
        }
    }
    const val AUTODETECT_PREFS = "autodetect"
    fun getStored(spk: String) = "${spk}_stored"
}