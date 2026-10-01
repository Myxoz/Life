package com.myxoz.life.storage.interfaces

import android.app.AlarmManager
import android.content.Context
import com.myxoz.life.core.UnixWeek
import com.myxoz.life.core.toInstant
import com.myxoz.life.storage.LocalEvent
import com.myxoz.life.storage.api.API
import com.myxoz.life.storage.api.helper.SyncHelper
import com.myxoz.life.storage.api.helper.SyncHelper.Companion.deleteAndDoNotSync
import com.myxoz.life.storage.api.helper.SyncHelper.Companion.deleteAndSync
import com.myxoz.life.storage.api.helper.SyncHelper.Companion.saveAndRequestSync
import com.myxoz.life.storage.api.syncables.SyncedEvent
import com.myxoz.life.storage.caches.InterlockedCache
import com.myxoz.life.storage.caches.InterlockedCache.Companion.overwrite
import com.myxoz.life.storage.caches.InterlockedCache.Companion.remove
import com.myxoz.life.storage.caches.PerformantCache
import com.myxoz.life.storage.caches.RangedCache.Rangable.Companion.rangableTo
import com.myxoz.life.storage.caches.RangedCache.Rangable.Companion.startAndEndInMs
import com.myxoz.life.storage.caches.RangedCache.Rangable.Companion.toRange
import com.myxoz.life.storage.convertion.SavePersistent.saveToDB
import com.myxoz.life.storage.convertion.SyncableFromEntity.fromEntity
import com.myxoz.life.storage.convertion.events.EventsFromDatabase
import com.myxoz.life.storage.dbwrapper.events.ReadEventDetailsDao
import com.myxoz.life.storage.dbwrapper.localevent.LocalEventsDao
import com.myxoz.life.storage.events.RawEvent
import com.myxoz.life.storage.events.additionals.PeopleEvent
import com.myxoz.life.storage.interfaces.utils.Cached
import com.myxoz.life.storage.interfaces.utils.Cached.Companion.cached
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.milliseconds

class CalendarInterface internal constructor(
    private val eventDetailsDao: ReadEventDetailsDao,
    private val localEventsDao: LocalEventsDao,
    private val context: Context,
    private val appScope: CoroutineScope,
    private val syncHelper: SyncHelper,
) {
    val firstEventTimestamp = MutableStateFlow<Instant?>(null)
    val firstEventTimesampHook = object : CalendarHook {
        override fun eventUpdated(old: SyncedEvent?, new: SyncedEvent) {
            if(old != null && old.raw.eventTime.start == firstEventTimestamp.value) {
                updateFirstEventTS()
            }
        }
        override fun eventRemoved(old: SyncedEvent) {
            if(old.raw.eventTime.start == firstEventTimestamp.value) {
                updateFirstEventTS()
            }
        }
        override fun eventAdded(new: SyncedEvent) {
            val other = firstEventTimestamp.value ?: return
            if(new.raw.eventTime.start <= other) {
                updateFirstEventTS()
            }
        }
    }
    fun updateFirstEventTS(){
        appScope.launch {
            firstEventTimestamp.value = eventDetailsDao.getFirstEventStart().toInstant()
        }
    }
    val nextAlarmClockTs = MutableStateFlow<AlarmManager.AlarmClockInfo?>(null)
    private val hooks = mutableSetOf<CalendarHook>(firstEventTimesampHook)
    fun addHook(hook: CalendarHook) {
        hooks.add(hook)
    }
    fun refetchAlarmClockTs() {
        val service: AlarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        nextAlarmClockTs.value = service.nextAlarmClock
    }

    private val zone: ZoneId = ZoneId.systemDefault()
    private val lastUpdateForPersonCache =
        PerformantCache<Long, Long>(appScope) { System.currentTimeMillis() }

    fun interactedWithPerson(person: Long) = lastUpdateForPersonCache.flowByKey(person)
    @OptIn(FlowPreview::class)
    val debouncedInteractedWithAnyPerson = lastUpdateForPersonCache.allValuesFlow.debounce(100.milliseconds)
    private val _cachedEvents = InterlockedCache.dayedCached<Long, SyncedEvent>(
        appScope,
        { first, other ->
            first.value?.id == other.id
        },
        {
            it.value?.raw?.eventTime?.stetchedDays(zone) ?: emptyList()
        },
        { it.id },
        { id ->
            EventsFromDatabase.getEventFromDatabaseUnbatched(id, eventDetailsDao).cached
        },
        { range ->
            val (from, to) = range.startAndEndInMs(zone)
            val events = eventDetailsDao.getEventsBetween(from, to)
            EventsFromDatabase.getBatchedEvents(events, eventDetailsDao)
        },
    ) { _, raw ->
        val new = raw.value
        if (new?.raw is PeopleEvent) {
            lastUpdateForPersonCache.overwriteAll(new.raw.people.map { it to System.currentTimeMillis() })
        }
    }

    fun eventsForDay(date: LocalDate): Flow<List<SyncedEvent>?> {
        return _cachedEvents.getInterlockedFlowFor(date)
    }

    fun eventById(eventId: Long) = _cachedEvents.flowFor(eventId)

    suspend fun prefetchDays(from: LocalDate, to: LocalDate) = _cachedEvents.fetchRange(from.rangableTo(to))
    fun deleteSyncedEventFromCache(id: Long) {
        _cachedEvents.remove(id)
    }

    suspend fun removeSyncedEvent(event: SyncedEvent) {
        event.deleteAndSync(syncHelper)
        hooks.forEach{ it.eventRemoved(event) }
        deleteSyncedEventFromCache(event.id)
    }

    suspend fun updateOrCreateSyncedEvent(rawEvent: RawEvent, previousId: Long?) {
        val old = previousId?.let {
            _cachedEvents.getContent(it).value
                ?: error("Tried to create synced event with previous id $it, but it was not present")
        }
        val new = if(old != null) {
            SyncedEvent(old.id, old.created, Instant.now(), rawEvent)
                .also {
                    old.deleteAndDoNotSync(
                        syncHelper,
                        // Thoroughly deletes if the calendar was switched e.x. spont -> hobby
                        old.raw::class != it.raw::class
                    )
                }
        } else SyncedEvent(API.generateId(), Instant.now(), null, rawEvent)
        new.saveAndRequestSync(syncHelper)
        updateSyncedEventCached(new)
        if(old != null)
            hooks.forEach { it.eventUpdated(old,new) }
        else
            hooks.forEach { it.eventAdded(new) }
    }

    fun updateSyncedEventCached(event: SyncedEvent) {
        _cachedEvents.overwrite(event.id, event)
    }

    private val _rawEventsDayed = InterlockedCache.dayedCached(
        appScope,
        { first, other -> first.value?.localId == other.localId },
        { it.value?.raw?.eventTime?.stetchedDays(zone) ?: listOf() },
        { it.localId },
        { localEventsDao.getLocalEventById(it)?.fromEntity().cached },
        { range ->
            val (start, end) = range.startAndEndInMs(ZoneId.systemDefault())
            localEventsDao.getLocalEventsBetween(start, end).mapNotNull { it.fromEntity() }
        },
    )

    fun getLocalEventsAt(date: LocalDate) = _rawEventsDayed.getInterlockedFlowFor(date)
    suspend fun updateLocalEvents(list: List<LocalEvent>) {
        updateLocalEventsRaw(list.map { it.localId to it.cached })
    }
    suspend fun removeLocalEvent(id: Long) {
        localEventsDao.deleteLocalEvent(id)
        _rawEventsDayed.remove(id)
    }

    internal suspend fun updateLocalEventsRaw(list: List<Pair<Long, Cached<LocalEvent>>>) {
        list.forEach {
            val cached = it.second.value
            if(cached == null)
                localEventsDao.deleteLocalEvent(it.first)
            else
                cached.saveToDB(localEventsDao)
        }
        _rawEventsDayed.overwriteAll(list)
    }

    suspend fun getNonSleepEventAfter(ts: Long): SyncedEvent? =
        EventsFromDatabase.getEventFromDatabaseUnbatched(
            eventDetailsDao.getNonSleepEventAfter(ts)?.id?: return null,
            eventDetailsDao
        )

    fun eventsForWeek(week: UnixWeek) = _cachedEvents.flowByRange(week.toRange())
    interface CalendarHook{
        fun eventUpdated(old: SyncedEvent?, new: SyncedEvent)
        fun eventRemoved(old: SyncedEvent)
        fun eventAdded(new: SyncedEvent)
    }

    init {
        updateFirstEventTS()
    }
}