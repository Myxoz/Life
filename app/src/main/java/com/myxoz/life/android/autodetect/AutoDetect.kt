package com.myxoz.life.android.autodetect

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.myxoz.life.dbwrapper.people.ReadPeopleDao
import com.myxoz.life.events.LocalEvent
import com.myxoz.life.utils.SharedPrefsUtils.edit
import com.myxoz.life.viewmodels.Settings
import org.json.JSONObject

object AutoDetect {
    private data class AutoDetectFromSensors(val spk: String, val feature: Settings.Feature, val getAdditionalSessions: suspend (prefs: SharedPreferences, context: Context, db: ReadPeopleDao)->List<LocalEvent>)

    const val SESSIONS = "sessions"
    private val autodetects = arrayOf(
        AutoDetectFromSensors(
            AutoDetectSleep.SPK,
            Settings.Feature.AutoDetectSleep,
        ) { prefs, context, _ ->
            AutoDetectSleep.getSessions(prefs, context).map { LocalEvent.deterministicId(it) }
        },
        AutoDetectFromSensors(
            AutoDetectCall.SPK,
            Settings.Feature.AutoDetectCalls,
        ) { prefs, context, db ->
            AutoDetectCall.getSessions(prefs, context, db)
                .map { LocalEvent.deterministicId(it) }
        }
    )

    /** Always call from IO Dispatcher, running on main will cause lag */
    suspend fun autoDetectEvents(context: Context, settings: Settings.CompositionSettings, db: ReadPeopleDao): List<LocalEvent> {
        val prefs = context.getSharedPreferences(AUTODETECT_PREFS, MODE_PRIVATE)
        val newSessions = mutableListOf<LocalEvent>()
        for (autodetectable in autodetects) {
            if(!settings.hasAssured(autodetectable.feature)) continue
            val detectedSessions = autodetectable.getAdditionalSessions(prefs, context, db).sortedBy { it.raw.start }
            newSessions.addAll(detectedSessions)
        }

        val oldSessionsRaw = (prefs.getStringSet(SESSIONS, setOf()) ?: setOf())
        val oldSessions = oldSessionsRaw
            .mapNotNull { LocalEvent.fromJSON( JSONObject(it)) }
            .toMutableList()

        val merged = (newSessions + oldSessions)
        if(newSessions.isNotEmpty()) {
            prefs.edit {
                putStringSet(SESSIONS, oldSessionsRaw + newSessions.map { it.specificsToJson().toString() })
            }
        }

        return merged
    }
    interface AutoDetectEvent
    const val AUTODETECT_PREFS = "autodetect"
}