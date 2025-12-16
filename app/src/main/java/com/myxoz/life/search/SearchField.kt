package com.myxoz.life.search

import com.myxoz.life.events.DigSocEvent
import com.myxoz.life.events.ProposedEvent
import com.myxoz.life.events.TravelEvent
import com.myxoz.life.events.additionals.DetailsEvent
import com.myxoz.life.events.additionals.TagEvent
import com.myxoz.life.events.additionals.TitleEvent
import kotlinx.coroutines.flow.MutableStateFlow

class SearchField {
    val lastUpdated = MutableStateFlow(System.currentTimeMillis())
    val textInputValue = MutableStateFlow<String?>(null)
    val mode = MutableStateFlow(SearchMode.Text)
    fun setText(text: String?){
        val old = textInputValue.value
        textInputValue.value = text?.takeIf { it.isNotEmpty()  }
        if(old != textInputValue.value) {
            lastUpdated.value = System.currentTimeMillis()
        }
    }
    fun isSearched(event: ProposedEvent): Boolean {
        if(mode.value == SearchMode.Text) {
            val text = textInputValue.value?.lowercase() ?: return true
            return (event is TagEvent && event.containsTagLike(text)) ||
                    (event is TitleEvent && event.title.contains(text, true)) ||
                    (event is DetailsEvent && event.details?.contains(text, true) == true) ||
                    (event is DigSocEvent && event.digSocEntries.any{it.type.name.contains(text, true)}) ||
                    (event is TravelEvent && event.vehicles.any{it.type.name.contains(text, true)})
        } else  {
            return true
        }
    }
    enum class SearchMode{
        Target,
        Text
    }
}