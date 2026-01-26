package com.myxoz.life.viewmodels

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.android.autodetect.AutoDetect
import com.myxoz.life.api.jsonObjArray
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.dbwrapper.BankingEntity
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.events.ProposedEvent
import com.myxoz.life.screens.feed.instantevents.InstantEvent
import com.myxoz.life.screens.feed.search.SearchField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.time.LocalDate

class CalendarViewModel(private val settings: Settings, private val storage: StorageManager) : ViewModel() {
    val days = mutableStateListOf<LocalDate>()
    val search = SearchField()
    val dayCache = mutableMapOf<LocalDate, List<SyncedEvent>>()
    val instantEventCache = mutableMapOf<LocalDate, List<InstantEvent.Companion.InstantEventGroup>>()
    val dayAmount = MutableStateFlow(2)
    var selectDayPopup = MutableStateFlow(false)
    val proposedEvents = mutableStateListOf<ProposedEvent>()
    val futureBankEntries = getFututreBankEntries()
    val minuteFlow = flow {
        emit(System.currentTimeMillis())
        while (true){
            delay(60*1000L-System.currentTimeMillis()%(60*1000L))
            emit(System.currentTimeMillis())
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        System.currentTimeMillis()
    )

    var lastEventUpdateTs = MutableStateFlow(0L)
    private var initialized = false

    fun getFututreBankEntries(): List<BankingEntity>{
        return JSONArray(storage.prefs.getString("payments", null) ?: "[]").jsonObjArray.map {
            BankingEntity(
                "",
                true,
                true, // Semantic value, but works
                -it.getInt("amount"),
                "EUR",
                "",
                it.getLong("timestamp"),
                "Unbekannt",
                "",
                "",
                "",
                0,
                it.getLong("timestamp"),
                it.getLong("timestamp")
            )
        }
    }
    fun initialize(context: Context) {
        if (initialized) return
        initialized = true

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                proposedEvents.addAll(AutoDetect.autoDetectEvents(context, settings, storage))
            }
        }
    }

    fun removeProposedEvent(event: ProposedEvent, context: Context) {
        proposedEvents.remove(event)
        event.ignoreProposed(context)
    }

    fun refreshEvents() {
        lastEventUpdateTs.value = System.currentTimeMillis()
    }

    fun setDay(selectedDay: LocalDate) {
        days.clear()
        days.add(selectedDay)
    }
}