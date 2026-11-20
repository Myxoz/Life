package com.myxoz.life.background.autodetect

import android.annotation.SuppressLint
import android.content.Context
import com.myxoz.life.lifecore.settings.CompositionSettings
import com.myxoz.life.lifecore.settings.Feature
import com.myxoz.life.storage.AutoDetectLocalEvent
import com.myxoz.life.storage.api.syncables.PersonSyncable
import com.myxoz.life.storage.dbwrapper.state.KeyValueStore
import com.myxoz.life.storage.events.RawEvent
import com.myxoz.life.storage.interfaces.CalendarInterface
import com.myxoz.life.storage.stored.AutoDetectStored
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant

object AutoDetect {
    internal enum class AutoDetectables(
        val stored: AutoDetectStored,
        val feature: Feature,
        val getAdditionalSessions: suspend (helper: AutoDetectHelper)->List<RawEvent>
    ) {
        AutoDetectSleep(
            AutoDetectStored.LastAutoSleep,
            Feature.AutoDetectSleep,
            @SuppressLint("MissingPermission") { helper ->
                AutoDetectSleepDetector.getSessions(helper)
            }
        ),
        AutoDetectPhoneCalls(
            AutoDetectStored.LastAutoDetectPhoneCalls,
            Feature.AutoDetectPhoneCalls,
            { helper ->
                AutoDetectPhoneCallDetector.getSessions(helper)
            }
        )
    }
    internal class AutoDetectHelper(
        val compositionSettings: CompositionSettings,
        private val prefs: KeyValueStore,
        val context: Context,
        val allPeople: List<PersonSyncable>,
    ) {
        fun getLastTimestamp(detectable: AutoDetectables) =
            prefs.get(detectable.stored.key)
        fun putLastTimestamp(detectable: AutoDetectables, ts: Instant) =
            prefs.put(detectable.stored.key, ts)
    }

    const val SESSIONS = "sessions"
    /** Always call from IO Dispatcher, running on main will cause lag */
    internal suspend fun autoDetectEvents(helper: AutoDetectHelper): List<AutoDetectLocalEvent> {
        val detectedSessions = mutableListOf<RawEvent>()
        for (autodetectable in AutoDetectables.entries) {
            if(!helper.compositionSettings.hasAssured(autodetectable.feature)) continue
            // .sortedBy { it.raw.eventTime.start } maybe sort
            detectedSessions.addAll(autodetectable.getAdditionalSessions(helper))
        }
        return detectedSessions.map { AutoDetectLocalEvent.deterministicFrom(it) }
    }

    suspend fun fetchAutoDetectEvents(
        settings: CompositionSettings,
        calendarInterface: CalendarInterface,
        context: Context,
        allPeople: Flow<List<PersonSyncable>>
    ) {
        val autoDetectHelper = AutoDetectHelper(
            settings,
            settings.localSettings,
            context,
            allPeople.first()
        )
        calendarInterface.updateLocalEvents(autoDetectEvents(autoDetectHelper))
    }
}