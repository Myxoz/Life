package com.myxoz.life.screens.feed.search

import androidx.navigation.NavController
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.events.DigSocEvent
import com.myxoz.life.events.ProposedEvent
import com.myxoz.life.events.TravelEvent
import com.myxoz.life.events.additionals.DetailsEvent
import com.myxoz.life.events.additionals.DigSocPlatform
import com.myxoz.life.events.additionals.EventTag
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.events.additionals.PeopleEvent
import com.myxoz.life.events.additionals.TagEvent
import com.myxoz.life.events.additionals.TitleEvent
import com.myxoz.life.events.additionals.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow

class SearchField {
    val lastUpdated = MutableStateFlow(System.currentTimeMillis())
    val textInputValue = MutableStateFlow<String?>(null)
    val mode = MutableStateFlow(SearchMode.Text)
    val selectedPeople = MutableStateFlow(listOf<Long>())
    val selectedEventTypes = MutableStateFlow(listOf<EventType>())
    val ignoreCase = MutableStateFlow(true)
    val regexMode = MutableStateFlow(false)
    val tags = MutableStateFlow(listOf<EventTag>())
    val locationFrom = MutableStateFlow(listOf<Long>())
    val locationTo = MutableStateFlow(listOf<Long>())
    val selectedVehicles = MutableStateFlow(listOf<Vehicle>())
    val detailsQuery = MutableStateFlow("")
    val titleQuery = MutableStateFlow("")
    val digsocPlatforms = MutableStateFlow(listOf<DigSocPlatform>())
    val isSearching = MutableStateFlow(false)
    // TODO ADD TO RESET
    fun setText(text: String?){
        val old = textInputValue.value
        textInputValue.value = text?.takeIf { it.isNotEmpty()  }
        if(old != textInputValue.value) wasUpdated()
    }
    suspend fun isSearched(db: StorageManager, event: ProposedEvent, allEvents: List<SyncedEvent>): Boolean {
        if(mode.value == SearchMode.Text) {
            val text = textInputValue.value?.lowercase() ?: return true
            return (event is TagEvent && event.containsTagLike(text)) ||
                    (event is TitleEvent && event.title.contains(text, true)) ||
                    (event is DetailsEvent && event.details?.contains(text, true) == true) ||
                    (event is DigSocEvent && event.digSocEntries.any{it.type.name.contains(text, true)}) ||
                    (event is TravelEvent && event.vehicles.any{it.type.name.contains(text, true)}) ||
                    (event is DigSocEvent && event.digSocEntries.any{it.type.name.contains(text, true)}) ||
                    (event is TravelEvent && db.location.getLocationsByIds(listOf(event.from, event.to)).any{it.name.contains(text, true)}) ||
                    (event is PeopleEvent && db.people.getPersonsByIds(event.people).any { it.name.contains(text, true) })
        } else  {
            return (
                    selectedPeople.value.isEmpty() ||
                            (event is PeopleEvent && event.people.any { it in selectedPeople.value }) ||
                            (allEvents.filter { it.proposed is PeopleEvent &&  it.proposed.people.any { p -> p in selectedPeople.value }}.any { it.proposed.overlaps(event) })
                    ) &&
                    (detailsQuery.value.isBlank() || (event is DetailsEvent && event.details?.matchesRegexOrSearch(detailsQuery.value, regexMode.value, ignoreCase.value) == true)) &&
                    (titleQuery.value.isBlank() || (event is TitleEvent && event.title.matchesRegexOrSearch(titleQuery.value, regexMode.value, ignoreCase.value))) &&
                    (selectedEventTypes.value.isEmpty() || event.type in selectedEventTypes.value) &&
                    (locationTo.value.isEmpty() || (event is TravelEvent && event.to in locationTo.value)) &&
                    (digsocPlatforms.value.isEmpty() || (event is DigSocEvent && event.digSocEntries.any { it.type in digsocPlatforms.value })) &&
                    (locationFrom.value.isEmpty() || (event is TravelEvent && event.from in locationFrom.value)) &&
                    (selectedVehicles.value.isEmpty() || (event is TravelEvent && selectedVehicles.value.all { it in event.vehicles.map { c -> c.type } })) &&
                    (tags.value.isEmpty() || (event is TagEvent && event.eventTags.any{ it in tags.value}))
        }
    }
    private fun String.matchesRegexOrSearch(query: String, regexEnabled: Boolean, ignoreCase: Boolean): Boolean{
        if(query.isEmpty()) return true
        if(!regexEnabled) return contains(query, ignoreCase)
        return if(ignoreCase)
            matches(query.toRegex(RegexOption.IGNORE_CASE))
        else
            matches(query.toRegex())
    }
    fun reset(){
        textInputValue.value = null
        mode.value = SearchMode.Text
        selectedPeople.value = listOf()
        selectedEventTypes.value = listOf()
        ignoreCase.value = true
        regexMode.value = false
        tags.value = listOf()
        locationFrom.value = listOf()
        locationTo.value = listOf()
        selectedVehicles.value = listOf()
        detailsQuery.value = ""
        titleQuery.value = ""
        digsocPlatforms.value = listOf()
        wasUpdated()
    }
    fun wasUpdated(){
        lastUpdated.value = System.currentTimeMillis()
        if(mode.value == SearchMode.Target){
            if(selectedEventTypes.value.isEmpty() || selectedEventTypes.value.any { it != EventType.DigSoc }) digsocPlatforms.value = listOf()
            if(selectedEventTypes.value.isEmpty() || selectedEventTypes.value.any { !it.isTagEvent() }) tags.value = listOf()
            if(selectedEventTypes.value.isEmpty() || selectedEventTypes.value.any { !it.isTitleEvent() }) titleQuery.value = ""
            if(selectedEventTypes.value.isEmpty() || selectedEventTypes.value.any { it != EventType.Travel }) {
                locationTo.value = listOf()
                locationFrom.value = listOf()
                selectedVehicles.value = listOf()
            }
        }
        isSearching.value = isSearching()
    }
    fun isSearching(): Boolean {
        if (mode.value == SearchMode.Text) return !textInputValue.value.isNullOrEmpty()
        return selectedPeople.value.isNotEmpty() || selectedEventTypes.value.isNotEmpty()
    }
    inline fun openCalendarWithSearch(nav: NavController,  applied: SearchField.()->Unit){
        applied()
        mode.value = SearchMode.Target
        wasUpdated()
        nav.popBackStack("home", false)
    }
    enum class SearchMode{
        Target,
        Text
    }
}