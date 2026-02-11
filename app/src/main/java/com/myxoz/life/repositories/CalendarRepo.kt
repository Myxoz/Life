package com.myxoz.life.repositories

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.myxoz.life.android.autodetect.AutoDetect
import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.dbwrapper.WaitingSyncDao
import com.myxoz.life.dbwrapper.WaitingSyncEntity
import com.myxoz.life.events.ProposedEvent
import com.myxoz.life.events.additionals.PeopleEvent
import com.myxoz.life.repositories.utils.Versioned
import com.myxoz.life.repositories.utils.VersionedCache
import com.myxoz.life.viewmodels.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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
    private val autoDetectPrefs: SharedPreferences = context.getSharedPreferences(AutoDetect.AUTODETECT_PREFS, MODE_PRIVATE)
    private val zone: ZoneId = ZoneId.systemDefault()
    private val _today = MutableStateFlow(LocalDate.now())
    val todayFlow: StateFlow<LocalDate> = _today
    private val prefetchedDays = mutableSetOf<LocalDate>()
    private suspend fun prefetchAllEventsIfNeeded(date: LocalDate) {
        if(date in prefetchedDays) return
        prefetchedDays.add(date)
        val events = readSyncableDaos.eventDetailsDao.getEventsBetween(
            date.atStartOfDay(zone).toInstant().toEpochMilli(),
            date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        ).mapNotNull { SyncedEvent.from(readSyncableDaos.eventDetailsDao, it)}
        _cachedEvents.overwriteAll(events.map { it.id to it })
    }
    private val _cachedEventsDayed = VersionedCache<LocalDate, List<SyncedEvent>>(
        { listOf() }
    )
    fun eventsForDay(date: LocalDate): Flow<Versioned<List<SyncedEvent>>?> {
        appScope.launch { prefetchAllEventsIfNeeded(date) }
        return _cachedEventsDayed.flowByKey(appScope, date)
    }

    private val interactedWithPersonCache = VersionedCache<Long, Int>({ 0 })
    fun interactedWithPerson(person: Long) = interactedWithPersonCache.flowByKey(appScope, person)
    fun interactedWithAnyPerson() = interactedWithPersonCache.allValuesFlow()
    private val _cachedEvents = VersionedCache<Long, SyncedEvent?>(
        { id ->
            SyncedEvent.from(
                readSyncableDaos.eventDetailsDao,
                readSyncableDaos.eventDetailsDao.getEvent(id)?:return@VersionedCache null
            )
        }
    ) { _, old, new ->
        if(old != null && new != null && old.proposed.start == new.proposed.start && old.proposed.end == new.proposed.end) {
            _cachedEventsDayed.updateKeysWith(new.proposed.getAllStrechedDays(zone)) { list ->
                list.toMutableList().apply {
                    val index = indexOf(old)
                    if(index != -1){
                        set(index, new)
                    }
                }
            }
            return@VersionedCache // This is a shortcut route, if we have no changes in times, daycache only needs to update once
        }
        if(old!=null) { // Move or remove happend. Delete from dayed cache
            _cachedEventsDayed.updateKeysWith(old.proposed.getAllStrechedDays(zone)) { list ->
                list.filterNot { it.id == old.id }
            }
        }
        if(new!=null) { // Move or new created
            _cachedEventsDayed.updateKeysWith(new.proposed.getAllStrechedDays(zone)) { list ->
                list + new
            }
        }
        if(new?.proposed is PeopleEvent){
            interactedWithPersonCache.updateKeysWith(
                new.proposed.people
            ){
                it+1
            }
        }
    }
    suspend fun deleteSyncedEventFromCache(id: Long) {
        _cachedEvents.overwrite(id, null)
    }
    suspend fun removeSyncedEvent(event: SyncedEvent) {
        waitingSyncDao.deleteWaitingSync( // If the event is jet to be synced, discard the sync request
            event.id,
            event.proposed.type.id
        )
        waitingSyncDao.insertWaitingSync(
            WaitingSyncEntity(
                event.id,
                -event.proposed.type.id,
                System.currentTimeMillis()
            )
        )
        event.proposed.eraseFromDB(writeSyncableDaos.eventDetailsDao, event.id)
        _cachedEvents.overwrite(event.id, null)
    }
    suspend fun updateOrCreateSyncedEvent(event: SyncedEvent) {
        val ev = if (event.isSynced()) { // Edited
            val old = _cachedEvents.get(event.id).data // This should be cached
            old?.proposed?.eraseFromDB(writeSyncableDaos.eventDetailsDao, event.id)
            event.copy(edited = System.currentTimeMillis())
        } else {
            event.makeSynced()
        }
        ev.saveToDB(writeSyncableDaos)
        waitingSyncDao.insertWaitingSync(WaitingSyncEntity(ev.id, ev.calendarId, System.currentTimeMillis()))
        _cachedEvents.overwrite(ev.id, ev)
    }
    suspend fun updateSyncedEventCached(event: SyncedEvent) {
        _cachedEvents.overwrite(event.id, event)
    }
    private val initalProposedEvents = MutableStateFlow<List<ProposedEvent>>(listOf())
    private val _proposedEventsDayed = VersionedCache<LocalDate, List<ProposedEvent>>(
        { key ->
            initalProposedEvents.first().filter { key in it.getAllStrechedDays(zone)  }
        }
    )
    fun getProposedEventsAt(date: LocalDate) = _proposedEventsDayed.flowByKey(appScope, date)
    suspend fun removeProposedEvent(event: ProposedEvent){
        _proposedEventsDayed.updateKeysWith(event.getAllStrechedDays(zone)) { list ->
            list.filterNot { it == event }
        }
        event.ignoreProposed(autoDetectPrefs)
    }
    fun saveProposedNotYetSyncedEvent(event: ProposedEvent) {
        appScope.launch {
            val syncedEvent = SyncedEvent(
                0,
                System.currentTimeMillis(),
                null,
                event
            )
            updateOrCreateSyncedEvent(syncedEvent)
            event.ignoreProposed(autoDetectPrefs)
        }
    }
    private suspend fun updateProposedEvents(list: List<ProposedEvent>){
        val dayMap = mutableMapOf<LocalDate, MutableList<ProposedEvent>>()
        list.forEach { event ->
            event.getAllStrechedDays(zone).forEach { date ->
                dayMap.getOrPut(date) {mutableListOf()}
                dayMap[date]?.add(event)
            }
        }
        _proposedEventsDayed.overwriteAll(dayMap.map { it.key to it.value })
    }

    fun fetchAutoDetectEvents(settings: Settings) = appScope.launch {
        updateProposedEvents(
            AutoDetect.autoDetectEvents(context, settings, readSyncableDaos.peopleDao)
        )
    }
    init {
        appScope.launch {
            while (isActive) {
                _today.value = LocalDate.now()
                delay(timeUntilNextMidnight())
            }
        }
    }
    private fun timeUntilNextMidnight(): Long {
        val now = LocalDateTime.now()
        val tomorrow = now.toLocalDate().plusDays(1).atStartOfDay()
        return Duration.between(now, tomorrow).toMillis()
    }
}