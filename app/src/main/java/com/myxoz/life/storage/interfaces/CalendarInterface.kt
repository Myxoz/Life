package com.myxoz.life.storage.interfaces

import android.app.AlarmManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.myxoz.life.aggregator.PeopleAggregator
import com.myxoz.life.android.autodetect.AutoDetect
import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.DeleteEntry
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.events.LocalEvent
import com.myxoz.life.events.RawEvent
import com.myxoz.life.events.additionals.PeopleEvent
import com.myxoz.life.storage.dbwrapper.WaitingSyncDao
import com.myxoz.life.storage.dbwrapper.WaitingSyncEntity
import com.myxoz.life.storage.interfaces.utils.Cached
import com.myxoz.life.storage.interfaces.utils.Cached.Companion.cached
import com.myxoz.life.storage.interfaces.utils.PerformantCache
import com.myxoz.life.storage.interfaces.utils.PerformantInterlockedCache
import com.myxoz.life.storage.interfaces.utils.PerformantInterlockedCache.Companion.overwrite
import com.myxoz.life.storage.interfaces.utils.PerformantInterlockedCache.Companion.remove
import com.myxoz.life.utils.SharedPrefsUtils.edit
import com.myxoz.life.utils.UnixWeek
import com.myxoz.life.utils.atEndAsMillis
import com.myxoz.life.utils.atStartAsMillis
import com.myxoz.life.viewmodels.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class CalendarInterface(
    private val readSyncableDaos: API.ReadSyncableDaos,
    private val writeSyncableDaos: API.WriteSyncableDaos,
    private val waitingSyncDao: WaitingSyncDao,
    private val context: Context,
    private val appScope: CoroutineScope,
) {
    val nextAlarmClockTs = MutableStateFlow<AlarmManager.AlarmClockInfo?>(null)
    fun refetchAlarmClockTs() {
        val service: AlarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        nextAlarmClockTs.value = service.nextAlarmClock
    }

    private val autoDetectPrefs: SharedPreferences =
        context.getSharedPreferences(AutoDetect.AUTODETECT_PREFS, MODE_PRIVATE)
    private val zone: ZoneId = ZoneId.systemDefault()
    val todayFlow: Flow<LocalDate> = flow {
        emit(LocalDate.now())
        while (currentCoroutineContext().isActive) {
            delay(timeUntilNextMidnight())
            emit(LocalDate.now())
        }
    }
    private val interactedWithPersonCache =
        PerformantCache<Long, PeopleAggregator.NeverEqual>(appScope) {
            PeopleAggregator.NeverEqual()
        }

    fun interactedWithPerson(person: Long) = interactedWithPersonCache.flowByKey(person)
    val interactedWithAnyPerson = interactedWithPersonCache.allValuesFlow
    private val _cachedEvents = PerformantInterlockedCache.Companion.dayedCached(
        appScope,
        { first, other ->
            first.value?.id == other.id
        },
        {
            it.value?.raw?.getAllStrechedDays(zone) ?: emptyList()
        },
        { it.id },
        { id ->
            SyncedEvent.from(
                RawEvent.PreparedEventContent.prepareContentFor(
                    id,
                    readSyncableDaos.eventDetailsDao
                )
                    ?: return@dayedCached Cached.Null
            ).cached
        },
        { from, to ->
            val events = readSyncableDaos.eventDetailsDao.getEventsBetween(
                from.atStartAsMillis(zone),
                to.atEndAsMillis(zone)
            )
            val preparedEventContent = RawEvent.PreparedEventContent.prepareContentFor(
                events,
                readSyncableDaos.eventDetailsDao
            )
            preparedEventContent.mapNotNull { SyncedEvent.from(it) }
        },
    ) { _, raw ->
        val new = raw.value
        if (new?.raw is PeopleEvent) {
            interactedWithPersonCache.overwriteAll(new.raw.people.map { it to PeopleAggregator.NeverEqual() })
        }
    }

    fun eventsForDay(date: LocalDate): Flow<List<SyncedEvent>?> {
        return _cachedEvents.getInterlockedFlowFor(date)
    }

    fun eventById(eventId: Long) = _cachedEvents.flowFor(eventId)

    suspend fun prefetchDays(from: LocalDate, to: LocalDate) = _cachedEvents.fetchRange(from, to)
    fun deleteSyncedEventFromCache(id: Long) {
        _cachedEvents.remove(id)
    }

    suspend fun removeSyncedEvent(event: SyncedEvent) {
        DeleteEntry.requestSyncDelete(waitingSyncDao, event)
        event.raw.eraseFromDB(writeSyncableDaos.eventDetailsDao, event.id)
        _cachedEvents.remove(event.id)
    }

    suspend fun updateOrCreateSyncedEvent(event: SyncedEvent, wasEdited: Boolean) {
        val ev = if (wasEdited) { // Edited
            val old = _cachedEvents.getContent(event.id).value // This should be cached
            old?.raw?.eraseFromDB(writeSyncableDaos.eventDetailsDao, event.id)
            event.copy(edited = System.currentTimeMillis())
        } else event.makeSynced()
        ev.saveToDB(writeSyncableDaos)
        waitingSyncDao.insertWaitingSync(
            WaitingSyncEntity(
                ev.id,
                ev.calendarId,
                System.currentTimeMillis()
            )
        )
        _cachedEvents.overwrite(ev.id, ev)
    }

    fun updateSyncedEventCached(event: SyncedEvent) {
        _cachedEvents.overwrite(event.id, event)
    }

    private val _rawEventsDayed = PerformantInterlockedCache.Companion.cachedNonRange<Long, LocalDate, LocalEvent>(
        appScope,
        { first, other -> first.value?.localId == other.localId },
        { it.value?.raw?.getAllStrechedDays(zone) ?: listOf() },
        { it.localId },
        { Cached.Null },
        { listOf() },
    ).apply {
        markAllEntriesAsLoaded()
    }

    fun getLocalEventsAt(date: LocalDate) = _rawEventsDayed.getInterlockedFlowFor(date)
    fun removeLocalEventFromPrefsAndCache(event: LocalEvent) {
        val prefs = context.getSharedPreferences(AutoDetect.AUTODETECT_PREFS, MODE_PRIVATE)
        val currentSessions = (prefs.getStringSet(AutoDetect.SESSIONS, setOf()) ?: setOf())
            .filter {
                LocalEvent.fromJSON(JSONObject(it))?.localId != event.localId
            }
        prefs.edit {
            putStringSet(AutoDetect.SESSIONS, currentSessions.toSet())
        }
        _rawEventsDayed.remove(event.localId)
    }

    fun updateLocalEvents(list: List<LocalEvent>) {
        updateLocalEventsRaw(list.map { it.localId to it.cached })
    }

    fun updateLocalEventsRaw(list: List<Pair<Long, Cached<LocalEvent>>>) {
        _rawEventsDayed.overwriteAll(list)
    }

    fun fetchAutoDetectEvents(settings: Settings.CompositionSettings) {
        appScope.launch {
            updateLocalEvents(
                AutoDetect.autoDetectEvents(context, settings, readSyncableDaos.peopleDao)
            )
        }
    }
    private fun timeUntilNextMidnight(): Long {
        val now = LocalDateTime.now()
        val tomorrow = now.toLocalDate().plusDays(1).atStartOfDay()
        // Very generous 10ms for LocalDate.now() to update. Very likely never ever necessary,
        // but the user doesn't notice anything so we do not care
        return Duration.between(now, tomorrow).toMillis() + 10
    }

    suspend fun getNonSleepEventAfter(ts: Long): SyncedEvent?{
        return SyncedEvent.from(
            RawEvent.PreparedEventContent.prepareContentFor(
                readSyncableDaos.eventDetailsDao.getNonSleepEventAfter(ts) ?: return null,
                readSyncableDaos.eventDetailsDao
            ) ?: return null
        )
    }

    fun eventsForWeek(week: UnixWeek) = _cachedEvents.flowByRange(week.start, week.end)
}