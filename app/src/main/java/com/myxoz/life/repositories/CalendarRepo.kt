package com.myxoz.life.repositories

import android.app.AlarmManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.myxoz.life.aggregator.PeopleAggregator
import com.myxoz.life.android.autodetect.AutoDetect
import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.DeleteEntry
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.dbwrapper.WaitingSyncDao
import com.myxoz.life.dbwrapper.WaitingSyncEntity
import com.myxoz.life.events.ProposedEvent
import com.myxoz.life.events.additionals.PeopleEvent
import com.myxoz.life.repositories.utils.Cached
import com.myxoz.life.repositories.utils.Cached.Companion.cached
import com.myxoz.life.repositories.utils.PerformantCache
import com.myxoz.life.repositories.utils.PerformantInterlockedCache
import com.myxoz.life.repositories.utils.PerformantInterlockedCache.Companion.loadDays
import com.myxoz.life.repositories.utils.PerformantInterlockedCache.Companion.overwrite
import com.myxoz.life.repositories.utils.PerformantInterlockedCache.Companion.remove
import com.myxoz.life.utils.atEndAsMillis
import com.myxoz.life.utils.atStartAsMillis
import com.myxoz.life.viewmodels.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class CalendarRepo(
    private val readSyncableDaos: API.ReadSyncableDaos,
    private val writeSyncableDaos: API.WriteSyncableDaos,
    private val waitingSyncDao: WaitingSyncDao,
    private val context: Context,
    private val appScope: CoroutineScope,
) {
    val nextAlarmClockTs = MutableStateFlow<AlarmManager.AlarmClockInfo?>(null)
    fun refetchAlarmClockTs(){
        val service: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        nextAlarmClockTs.value = service.nextAlarmClock
    }
    private val autoDetectPrefs: SharedPreferences = context.getSharedPreferences(AutoDetect.AUTODETECT_PREFS, MODE_PRIVATE)
    private val zone: ZoneId = ZoneId.systemDefault()
    val todayFlow: Flow<LocalDate> = flow {
        emit(LocalDate.now())
        while (currentCoroutineContext().isActive) {
            delay(timeUntilNextMidnight())
            emit(LocalDate.now())
        }
    }
    private val interactedWithPersonCache = PerformantCache<Long, PeopleAggregator.NeverEqual>(appScope){
        PeopleAggregator.NeverEqual()
    }
    fun interactedWithPerson(person: Long) = interactedWithPersonCache.flowByKey(person)
    val interactedWithAnyPerson = interactedWithPersonCache.allValuesFlow
    private val _cachedEvents = PerformantInterlockedCache.dayedCached(
        appScope,
        {
            it.value?.proposed?.getAllStrechedDays(zone) ?: emptyList()
        },
        { first, other ->
            first.value?.id == other.id
        },
        { id ->
            SyncedEvent.from(
                ProposedEvent.PreparedEventContent.prepareContentFor(id, readSyncableDaos.eventDetailsDao)
                    ?: return@dayedCached Cached.Null
            ).cached
        },
        { from, to ->
            val events = readSyncableDaos.eventDetailsDao.getEventsBetween(
                from.atStartAsMillis(zone),
                to.atEndAsMillis(zone)
            )
            val preparedEventContent = ProposedEvent.PreparedEventContent.prepareContentFor(events, readSyncableDaos.eventDetailsDao)
            preparedEventContent.map{ it.event.id to SyncedEvent.from(it).cached }
        },
    ) { _, raw ->
        val new = raw.value
        if(new?.proposed is PeopleEvent) {
            interactedWithPersonCache.overwriteAll(new.proposed.people.map { it to PeopleAggregator.NeverEqual() })
        }
    }
    fun eventsForDay(date: LocalDate): Flow<List<SyncedEvent>?> {
        return _cachedEvents.getInterlockedFlowFor(date)
    }
    suspend fun prefetchDays(from: LocalDate, to: LocalDate) = _cachedEvents.loadDays(from, to)
    fun deleteSyncedEventFromCache(id: Long) {
        _cachedEvents.remove(id)
    }
    suspend fun removeSyncedEvent(event: SyncedEvent) {
        DeleteEntry.requestSyncDelete(waitingSyncDao, event)
        event.proposed.eraseFromDB(writeSyncableDaos.eventDetailsDao, event.id)
        _cachedEvents.remove(event.id)
    }
    suspend fun updateOrCreateSyncedEvent(event: SyncedEvent) {
        val ev = if (event.isSynced()) { // Edited
            val old = _cachedEvents.getContent(event.id).value // This should be cached
            old?.proposed?.eraseFromDB(writeSyncableDaos.eventDetailsDao, event.id)
            event.copy(edited = System.currentTimeMillis())
        } else {
            event.makeSynced()
        }
        ev.saveToDB(writeSyncableDaos)
        waitingSyncDao.insertWaitingSync(WaitingSyncEntity(ev.id, ev.calendarId, System.currentTimeMillis()))
        _cachedEvents.overwrite(ev.id, ev)
    }
    fun updateSyncedEventCached(event: SyncedEvent) {
        _cachedEvents.overwrite(event.id, event)
    }
    private val initalProposedEvents = MutableStateFlow<List<ProposedEvent>>(listOf())
    private val _proposedEventsDayed = PerformantCache<LocalDate, List<ProposedEvent>>(appScope) { key ->
        initalProposedEvents.first().filter { key in it.getAllStrechedDays(zone) }
    }

    fun getProposedEventsAt(date: LocalDate) = _proposedEventsDayed.flowByKey(date)
    suspend fun removeProposedEvent(event: ProposedEvent){
        _proposedEventsDayed.updateAll(event.getAllStrechedDays(zone)) { list ->
            list.filterNot { it == event }
        }
        event.ignoreProposed(autoDetectPrefs)
    }
    fun saveProposedNotYetSyncedEvent(event: ProposedEvent) {
        appScope.launch {
            val syncedEvent = SyncedEvent(
                -1,
                System.currentTimeMillis(),
                null,
                event
            )
            updateOrCreateSyncedEvent(syncedEvent)
            event.ignoreProposed(autoDetectPrefs)
        }
    }
    private fun updateProposedEvents(list: List<ProposedEvent>){
        val dayMap = mutableMapOf<LocalDate, MutableList<ProposedEvent>>()
        list.forEach { event ->
            event.getAllStrechedDays(zone).forEach { date ->
                dayMap.getOrPut(date) {mutableListOf()}
                dayMap[date]?.add(event)
            }
        }
        _proposedEventsDayed.overwriteAll(dayMap.map { it.key to it.value })
    }

    fun fetchAutoDetectEvents(settings: Settings.CompositionSettings) = appScope.launch {
        updateProposedEvents(
            AutoDetect.autoDetectEvents(context, settings, readSyncableDaos.peopleDao)
        )
    }
    private fun timeUntilNextMidnight(): Long {
        val now = LocalDateTime.now()
        val tomorrow = now.toLocalDate().plusDays(1).atStartOfDay()
        return Duration.between(now, tomorrow).toMillis()
    }

    suspend fun getNonSleepEventAfter(ts: Long): SyncedEvent?{
        return SyncedEvent.from(
            ProposedEvent.PreparedEventContent.prepareContentFor(
                readSyncableDaos.eventDetailsDao.getNonSleepEventAfter(ts) ?: return null,
                readSyncableDaos.eventDetailsDao
            ) ?: return null
        )
    }
}