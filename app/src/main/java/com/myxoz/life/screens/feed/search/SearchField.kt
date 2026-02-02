package com.myxoz.life.screens.feed.search

import androidx.navigation.NavController
import com.myxoz.life.events.DigSocEvent
import com.myxoz.life.events.TravelEvent
import com.myxoz.life.events.additionals.DetailsEvent
import com.myxoz.life.events.additionals.DigSocPlatform
import com.myxoz.life.events.additionals.EventTag
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.events.additionals.PeopleEvent
import com.myxoz.life.events.additionals.TagEvent
import com.myxoz.life.events.additionals.TitleEvent
import com.myxoz.life.events.additionals.Vehicle
import com.myxoz.life.screens.feed.main.SegmentedEvent
import com.myxoz.life.viewmodels.CalendarViewModel
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
    fun isSearched(calendarViewModel: CalendarViewModel, event: SegmentedEvent): Boolean {
        val proposed = event.event.proposed
        if(mode.value == SearchMode.Text) {
            val text = textInputValue.value?.lowercase() ?: return true
            return (proposed is TagEvent && proposed.containsTagLike(text)) ||
                    (proposed is TitleEvent && proposed.title.contains(text, true)) ||
                    (proposed is DetailsEvent && proposed.details?.contains(text, true) == true) ||
                    (proposed is DigSocEvent && proposed.digSocEntries.any{it.type.name.contains(text, true)}) ||
                    (proposed is TravelEvent && proposed.vehicles.any{it.type.name.contains(text, true)}) ||
                    (proposed is DigSocEvent && proposed.digSocEntries.any{it.type.name.contains(text, true)}) ||
                    (proposed is TravelEvent && listOf(calendarViewModel.getCachedLocation(proposed.from), calendarViewModel.getCachedLocation(proposed.to))
                        .any{it?.name?.contains(text, true) == true}) ||
                    (proposed is PeopleEvent && calendarViewModel.getCachedPeopleById(proposed.people).any { it.name.contains(text, true) })
        } else  {
            return (
                    selectedPeople.value.isEmpty() ||
                            (proposed is PeopleEvent && proposed.people.any { it in selectedPeople.value }) ||
                            (event.nextToPeople.any { it in selectedPeople.value })
                    ) &&
                    (detailsQuery.value.isBlank() || (proposed is DetailsEvent && proposed.details?.matchesRegexOrSearch(detailsQuery.value, regexMode.value, ignoreCase.value) == true)) &&
                    (titleQuery.value.isBlank() || (proposed is TitleEvent && proposed.title.matchesRegexOrSearch(titleQuery.value, regexMode.value, ignoreCase.value))) &&
                    (selectedEventTypes.value.isEmpty() || proposed.type in selectedEventTypes.value) &&
                    (locationTo.value.isEmpty() || (proposed is TravelEvent && proposed.to in locationTo.value)) &&
                    (digsocPlatforms.value.isEmpty() || (proposed is DigSocEvent && proposed.digSocEntries.any { it.type in digsocPlatforms.value })) &&
                    (locationFrom.value.isEmpty() || (proposed is TravelEvent && proposed.from in locationFrom.value)) &&
                    (selectedVehicles.value.isEmpty() || (proposed is TravelEvent && selectedVehicles.value.all { it in proposed.vehicles.map { c -> c.type } })) &&
                    (tags.value.isEmpty() || (proposed is TagEvent && proposed.eventTags.any{ it in tags.value}))
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