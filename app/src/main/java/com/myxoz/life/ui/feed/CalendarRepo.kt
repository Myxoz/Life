package com.myxoz.life.ui.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.myxoz.life.api.API
import com.myxoz.life.api.Syncable
import com.myxoz.life.api.extensions.RepeatingEventsExtendable
import com.myxoz.life.api.syncables.ExtensionSyncable
import com.myxoz.life.api.syncables.FullDaySyncable
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.events.LocalEvent
import com.myxoz.life.repositories.supermodule.CalendarSuper
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.utils.Cached
import com.myxoz.life.storage.interfaces.utils.StateFlowCache
import com.myxoz.life.storage.interfaces.utils.subscribeToColdFlow
import com.myxoz.life.ui.feed.instantevents.InstantEvent
import com.myxoz.life.ui.feed.main.PrerenderedEvent
import com.myxoz.life.ui.feed.search.SearchField
import com.myxoz.life.utils.toLocalDate
import com.myxoz.life.viewmodels.Settings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

class CalendarRepo(
    private val repos: DatabaseInterface,
    private val calendarSuper: CalendarSuper
) {
    // TODO Propagate to CrossRepoSuper
    val allPeople = repos.peopleInterface.getAllPeople()
    val allLocations = repos.locationInterface.getAllLocations
    val search = SearchField()
    @OptIn(ExperimentalCoroutinesApi::class)
    val yesterdaySummaryAdded = repos.calendarInterface.todayFlow.flatMapConcat {
        getDaySummary(it.minusDays(1))
    }
    val todayFlow = repos.calendarInterface.todayFlow
    val steps = repos.stepInterface.steps
    val repeatingEvents = repos.extensionInterface.flowFor(ExtensionSyncable.ExtensionSyncableType.RepeatingEvents).subscribeToColdFlow(repos.appScope, null)

    private val stepFlowCache = StateFlowCache<LocalDate, Cached<Int>?>{
        repos.stepInterface.getStepsFor(it).subscribeToColdFlow(repos.appScope, null)
    }
    fun getStepsFor(it: LocalDate) = stepFlowCache.get(it)
    val lastAPIResponse = MutableStateFlow<API.SyncingResponse?>(null)
    suspend fun resync() = repos.api.resync().also { lastAPIResponse.value = it }
    suspend fun localEventInteracted(event: LocalEvent, newEventId: Long?) {
        run {
            repos.extensionInterface.updateAndSyncWith(ExtensionSyncable.ExtensionSyncableType.RepeatingEvents) { old ->
                if(old.value?.events?.any { it.id == event.localId } != true) return@run
                val res = RepeatingEventsExtendable(old.value.events.map {
                    if(it.id == event.localId) it.next(newEventId) else it
                })
                repos.hooks.updateRepeatingEvents(res)
                res
            }
            // We are done, event moved!
            createSyncedEventFor(event, newEventId)
            return
        }
        repos.calendarInterface.removeLocalEventFromPrefsAndCache(event)
        createSyncedEventFor(event, newEventId)
        // New event created, save and sync
    }
    private suspend fun createSyncedEventFor(event: LocalEvent, newId: Long?){
        if(newId != null) {
            updateOrCreateSyncedEvent(
                SyncedEvent(newId, System.currentTimeMillis(), null, event.raw),
                false
            )
        }
    }
    suspend fun updateOrCreateSyncedEvent(event: SyncedEvent, wasEdited: Boolean) = repos.calendarInterface.updateOrCreateSyncedEvent(event, wasEdited)

    private val daySummaryFlowCache = StateFlowCache<LocalDate, Cached<FullDaySyncable>?> {
        repos.daySummaryInterface.getDaySummary(it).subscribeToColdFlow(repos.appScope, null)
    }
    fun getDaySummary(date: LocalDate) = daySummaryFlowCache.get(date)
    private val birthDayAtCached = StateFlowCache<LocalDate, List<PersonSyncable>>{
        repos.peopleInterface.getPeopleWithBirthdayAt(it).map { it?:listOf() }.subscribeToColdFlow(repos.appScope, listOf())
    }
    fun getPeopleWithBirthdayAt(date: LocalDate) = birthDayAtCached.get(date)
    private val getLocalEventsAtCache = StateFlowCache<LocalDate, List<LocalEvent>?>{
        repos.calendarInterface.getLocalEventsAt(it).subscribeToColdFlow(repos.appScope, listOf())
    }
    fun getLocalEventsAt(date: LocalDate) = getLocalEventsAtCache.get(date)
    fun requestAutoDetectedEventStart(settings: Settings.CompositionSettings) = repos.calendarInterface.fetchAutoDetectEvents(settings)
    suspend fun testSign() = repos.api.testSign()
    fun getBase64Public() = repos.api.getBase64Public()
    fun requireAllPeople() = repos.peopleInterface.requireAllPeople()
    private val prerenderedEventCache = StateFlowCache<LocalDate, Map<Long, PrerenderedEvent>>{ date ->
        repos.aggregators.calendarAggregator.getPrerenderedEvents(date).subscribeToColdFlow(repos.appScope, mapOf())
    }
    suspend fun loadRepeatingEvents() {
        val repeatingEvents = repos.extensionInterface.getExtension(ExtensionSyncable.ExtensionSyncableType.RepeatingEvents)?.events ?: return
        repos.calendarInterface.updateLocalEvents(
            repeatingEvents.map { LocalEvent(it.id, it.event) }
        )
    }
    fun getSegmentedEvents(date: LocalDate) = prerenderedEventCache.get(date)
    val instantEventsForDayCache = StateFlowCache<LocalDate, List<InstantEvent.InstantEventGroup>>{ date ->
        repos.aggregators.calendarAggregator.getInstantEventsForDay(date).subscribeToColdFlow(repos.appScope, listOf())
    }
    @Composable
    fun getInstantEventsForDay(syncable: Syncable.FeedInstantEventSyncable?, date: LocalDate): State<List<InstantEvent.InstantEventGroup>> {
        val baseEvents by instantEventsForDayCache.get(date).collectAsState()

        val zone = remember { ZoneId.systemDefault() }

        return remember(syncable) {
            derivedStateOf {
                if (syncable != null && syncable.timestamp.toLocalDate(zone) == date) {
                    baseEvents.mapNotNull { group ->
                        val filteredList = group.instantEvents.filter { !it.isEqualTo(syncable) }
                        if (filteredList.isEmpty()) null else InstantEvent.InstantEventGroup(
                            filteredList
                        )
                    }
                } else {
                    baseEvents
                }
            }
        }
    }
    fun getCachedLocation(id: Long) = repos.locationInterface.getCachedLocation(id)
    fun getCachedPeopleById(id: List<Long>) = repos.peopleInterface.getCachedPeopleById(id)
    fun getScreentime(date: LocalDate) = repos.aggregators.daySummaryAggregator.getScreenTimeForDay(date)
    fun refetchAlarmClockTs() = repos.calendarInterface.refetchAlarmClockTs()
    val isEditing = calendarSuper.isEditing
    val event = calendarSuper.event
    val editedSyncable = calendarSuper.editedSyncable
    fun setInspectedEventTo(event: SyncedEvent) = calendarSuper.setInspectedEventTo(event)
    fun setEditedSyncableTo(editedSyncable: Syncable.FeedInstantEventSyncable) = calendarSuper.setEditedSyncableTo(editedSyncable)
    fun setEditing(isNowEditing: Boolean) = calendarSuper.setEditing(isNowEditing)
    fun updateStartTs(ts: Long) = calendarSuper.updateStartTs(ts)
}