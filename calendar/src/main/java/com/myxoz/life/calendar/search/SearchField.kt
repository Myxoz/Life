package com.myxoz.life.calendar.search

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.myxoz.life.calendar.feed.events.prerendered.PrerenderedEvent
import com.myxoz.life.presentation.DigSocPlatformFormatting
import com.myxoz.life.presentation.EventTagFormatting
import com.myxoz.life.presentation.TimewastePlatformFormatting
import com.myxoz.life.presentation.VehicleFormatting
import com.myxoz.life.presentation.events.FormattableEvent
import com.myxoz.life.presentation.events.FormattableEvent.Companion.formattable
import com.myxoz.life.storage.api.syncables.LocationSyncable
import com.myxoz.life.storage.api.syncables.PersonSyncable
import com.myxoz.life.storage.events.DetailsEvent
import com.myxoz.life.storage.events.DigSocEvent
import com.myxoz.life.storage.events.SocialEvent
import com.myxoz.life.storage.events.TagEvent
import com.myxoz.life.storage.events.TimewasteEvent
import com.myxoz.life.storage.events.TitleEvent
import com.myxoz.life.storage.events.TravelEvent
import com.myxoz.life.storage.events.additionals.DigSocPlatform
import com.myxoz.life.storage.events.additionals.EventTag
import com.myxoz.life.storage.events.additionals.TimewastePlatform
import com.myxoz.life.storage.events.additionals.Vehicle
import com.myxoz.life.storage.interfaces.LocationInterface
import com.myxoz.life.storage.interfaces.PeopleInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class SearchField(
    peopleInterface: PeopleInterface,
    locationInterface: LocationInterface,
    viewModelScope: CoroutineScope
) {
    internal sealed interface SearchMode {
        fun isSearched(prerenderedEvent: PrerenderedEvent): Boolean
        object NotSearching: SearchMode {
            override fun isSearched(prerenderedEvent: PrerenderedEvent) = true
        }
        class Text(
            private val searchAssist: SearchAssist
        ): SearchMode {
            var query by mutableStateOf("")
            private val precompiled by derivedStateOf {
                // Will rerun on searchAssist.allPeople and allLocation update,
                // because state reads happen inside precompile
                searchAssist.precompile(query)
            }
            override fun isSearched(prerenderedEvent: PrerenderedEvent): Boolean =
                precompiled.matches(prerenderedEvent)

        }
        class Target: SearchMode {
            var titleQuery by mutableStateOf("")
                private set
            var detailsQuery by mutableStateOf("")
                private set
            var ignoreCase by mutableStateOf(true)
            var regexMode by mutableStateOf(false)

            var selectedPeople by mutableStateOf(setOf<Long>())
            var selectedEventTypes by mutableStateOf(setOf<FormattableEvent>())
            var selectedTags by mutableStateOf(setOf<EventTag>())
                private set
            var locationFrom by mutableStateOf(setOf<Long>())
                private set
            var locationTo by mutableStateOf(setOf<Long>())
                private set
            var selectedVehicles by mutableStateOf(setOf<Vehicle>())
                private set
            var digsocPlatforms by mutableStateOf(setOf<DigSocPlatform>())
                private set
            var timewastePlatform by mutableStateOf(setOf<TimewastePlatform>())
                private set
            private val precompiled by derivedStateOf {
                PrecompiledSearch(
                    selectedPeople,
                    locationFrom,
                    locationTo,
                    selectedTags,
                    selectedEventTypes,
                    selectedVehicles,
                    digsocPlatforms,
                    timewastePlatform,
                    TextSearch.getTextSearch(titleQuery, ignoreCase, regexMode),
                    TextSearch.getTextSearch(detailsQuery, ignoreCase, regexMode),
                    true
                )
            }
            fun setTitleQuery(newTitle: String) {
                titleQuery = newTitle
                selectedEventTypes = selectedEventTypes.intersect(FormattableEvent.TitleEvents)
            }
            fun setDetailsQuery(details: String) {
                detailsQuery = details
                selectedEventTypes = selectedEventTypes.intersect(FormattableEvent.DetailsEvents)
            }
            fun setTags(tags: Set<EventTag>) {
                selectedTags = tags
                selectedEventTypes = selectedEventTypes.intersect(FormattableEvent.TagEvents)
            }
            fun setLocationFrom(locationsFrom: Set<Long>) {
                locationFrom = locationsFrom
                selectedEventTypes = setOf(FormattableEvent.Travel)
            }
            fun setLocationTo(locationsTo: Set<Long>) {
                locationTo = locationsTo
                selectedEventTypes = setOf(FormattableEvent.Travel)
            }
            fun setVehicles(vehicles: Set<Vehicle>) {
                selectedVehicles = vehicles
                selectedEventTypes = setOf(FormattableEvent.Travel)
            }
            fun setTimewastePlatforms(timewastePlatforms: Set<TimewastePlatform>) {
                timewastePlatform = timewastePlatforms
                selectedEventTypes = setOf(FormattableEvent.Timewaste)
            }
            fun setDigSocPlatforms(digSocPlatforms: Set<DigSocPlatform>) {
                digsocPlatforms = digSocPlatforms
                selectedEventTypes = setOf(FormattableEvent.DigSoc)
            }
            override fun isSearched(prerenderedEvent: PrerenderedEvent): Boolean =
                precompiled.matches(prerenderedEvent)
        }
    }
    internal class SearchAssist {
        var allPeople by mutableStateOf<List<PersonSyncable>>(emptyList())
        var allLocations by mutableStateOf<List<LocationSyncable>>(emptyList())
        fun precompile(query: String): PrecompiledSearch {
            val possiblePeople = allPeople.filter { it.name.contains(query, true) }.map { it.id }.toSet()
            val possibleLocations = allLocations.filter { it.name.contains(query, true) }.map { it.id }.toSet()
            val possibleTags = EventTagFormatting.entries.filter { it.matches(query) }.map { it.tag }.toSet()
            val possibleEventTypes = FormattableEvent.entries.filter { it.displayName.contains(query, true) }.toSet()
            val possibleVehicles = VehicleFormatting.entries.filter { it.displayName.contains(query, true) }.map { it.vehicle }.toSet()
            val possibleDigSocPlatforms = DigSocPlatformFormatting.entries.filter { it.displayName.contains(query, true) }.map { it.platform }.toSet()
            val possibleTimewastePlatforms = TimewastePlatformFormatting.entries.filter { it.displayName.contains(query, true) }.map { it.platform }.toSet()
            return PrecompiledSearch(
                possiblePeople,
                possibleLocations,
                possibleLocations,
                possibleTags,
                possibleEventTypes,
                possibleVehicles,
                possibleDigSocPlatforms,
                possibleTimewastePlatforms,
                TextSearch.TextMode(query, true),
                TextSearch.TextMode(query, true),
                false
            )
        }
    }
    internal class PrecompiledSearch(
        val possiblePeople: Set<Long>,
        val possibleFromLocations: Set<Long>,
        val possibleToLocations: Set<Long>,
        val possibleTags: Set<EventTag>,
        val possibleEventTypes: Set<FormattableEvent>,
        val possibleVehicles: Set<Vehicle>,
        val possibleDigSocPlatforms: Set<DigSocPlatform>,
        val possibleTimewastePlatforms: Set<TimewastePlatform>,
        val titleSearch: TextSearch,
        val detailsSearch: TextSearch,
        val allNeedToMatch: Boolean
    ) {
        fun matches(prerenderedEvent: PrerenderedEvent): Boolean {
            val raw = prerenderedEvent.event.raw

            val eventTypeMatches = raw.formattable() in possibleEventTypes
            if(!eventTypeMatches && allNeedToMatch) return false

            val cantMatchPeople = allNeedToMatch && possiblePeople.isEmpty()

            var anyMatches = if(raw is SocialEvent) {
                val socialEventMatches = raw.people.any { it in possiblePeople }
                val peopleMatches = cantMatchPeople || socialEventMatches
                if(!peopleMatches && allNeedToMatch) return false
                 peopleMatches
            } else {
                cantMatchPeople || prerenderedEvent.nextToPeople.any { it in possiblePeople }
            }

            if(raw is TravelEvent) {
                val cantMatchFromLocation = allNeedToMatch && possibleFromLocations.isEmpty()
                val travelEventFromMatches = raw.from in possibleFromLocations
                val travelFromMatches = cantMatchFromLocation || travelEventFromMatches
                if(!travelFromMatches && allNeedToMatch) return false
                anyMatches = anyMatches || travelFromMatches

                val cantMatchToLocation = allNeedToMatch && possibleToLocations.isEmpty()
                val travelEventToMatches = raw.to in possibleToLocations
                val travelToMatches = cantMatchToLocation || travelEventToMatches
                if(!travelToMatches && allNeedToMatch) return false
                anyMatches = anyMatches || travelToMatches

                val cantMatchVehicles = allNeedToMatch && possibleVehicles.isEmpty()
                val travelEventVehicleMatches = raw.vehicles.any { it.type in possibleVehicles }
                val vehicleMatches = cantMatchVehicles || travelEventVehicleMatches
                if(!vehicleMatches && allNeedToMatch) return false
                anyMatches = anyMatches || vehicleMatches
            }

            if(raw is TagEvent) {
                val cantMatchTags = allNeedToMatch && possibleTags.isEmpty()
                val tagEventMatches = raw.eventTags.any { it in possibleTags }
                val tagMatches = cantMatchTags || tagEventMatches
                if(!tagMatches && allNeedToMatch) return false
                anyMatches = anyMatches || tagMatches
            }

            if(raw is DigSocEvent) {
                val cantMatchDigSoc = allNeedToMatch && possibleDigSocPlatforms.isEmpty()
                val digSocPlatformEventMatches = raw.digSocEntries.any { it.type in possibleDigSocPlatforms }
                val digSocMatches = cantMatchDigSoc || digSocPlatformEventMatches
                if(!digSocMatches && allNeedToMatch) return false
                anyMatches = anyMatches || digSocMatches
            }

            if(raw is TimewasteEvent) {
                val cantMatchTimewastePlatform = allNeedToMatch && possibleTimewastePlatforms.isEmpty()
                val timewastePlatformEventMatches = raw.timewastePlatforms.any { it.type in possibleTimewastePlatforms }
                val timewastePlatformMatches = cantMatchTimewastePlatform || timewastePlatformEventMatches
                if(!timewastePlatformMatches && allNeedToMatch) return false
                anyMatches = anyMatches || timewastePlatformMatches
            }

            if(raw is TitleEvent) {
                val title = raw.title
                val cantMatchTitle = allNeedToMatch && title.isEmpty()
                val eventTitleMatches = title.isNotEmpty() && titleSearch.matches(title)
                val titleMatches = cantMatchTitle || eventTitleMatches
                if(!titleMatches && allNeedToMatch) return false
                anyMatches = anyMatches || titleMatches
            }

            if(raw is DetailsEvent) {
                val eventDetails = raw.details
                val cantMatchDetails = allNeedToMatch && eventDetails.isNullOrEmpty()
                val eventDetailsMatches = !eventDetails.isNullOrEmpty() && detailsSearch.matches(eventDetails)
                val detailsMatches = cantMatchDetails || eventDetailsMatches
                if(!detailsMatches && allNeedToMatch) return false
                anyMatches = anyMatches || detailsMatches
            }

            return allNeedToMatch || anyMatches
        }
    }
    sealed interface TextSearch {
        fun matches(searchString: String): Boolean
        class TextMode internal constructor(val query: String, val ignoreCase: Boolean): TextSearch {
            override fun matches(searchString: String): Boolean =
                searchString.contains(query, ignoreCase)
        }
        class RegexMode internal constructor(val query: String, val ignoreCase: Boolean): TextSearch {
            val regex = try {
                if(ignoreCase) query.toRegex(RegexOption.IGNORE_CASE) else query.toRegex()
            } catch (_: Throwable) {
                null
            }
            override fun matches(searchString: String): Boolean =
                regex?.containsMatchIn(searchString) ?: searchString.contains(query, ignoreCase)
        }
        companion object {
            fun getTextSearch(query: String, ignoreCase: Boolean, regexMode: Boolean) =
                if(regexMode) RegexMode(query, ignoreCase) else TextMode(query, ignoreCase)
        }
    }
    var query by mutableStateOf<SearchMode>(SearchMode.NotSearching)
    private val searchAssist = SearchAssist()
    val isSearching by derivedStateOf {
        query !is SearchMode.NotSearching
    }
    fun setText(enteredQuery: String) {
        val newQuery = query as? SearchMode.Text ?: SearchMode.Text(searchAssist)
        newQuery.query = enteredQuery
        query = newQuery
    }
    fun reset() {
        query = SearchMode.NotSearching
    }
    fun openCalendarWithSearch(applied: SearchMode.Target.()->Unit){
        val newQuery = query as? SearchMode.Target ?: SearchMode.Target()
        newQuery.applied()
        query = newQuery
    }
    init {
        viewModelScope.launch {
            launch {
                peopleInterface.getAllPeople().collect {
                    searchAssist.allPeople = it
                }
            }
            launch {
                locationInterface.getAllLocations().collect {
                    searchAssist.allLocations = it
                }
            }
        }
    }
}