package com.myxoz.life.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.api.SyncedEvent
import com.myxoz.life.autodetect.autoDetectEvents
import com.myxoz.life.dbwrapper.BankingEntity
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.events.ProposedEvent
import com.myxoz.life.search.SearchField
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.time.LocalDate

class CalendarViewModel(private val settings: Settings, private val storage: StorageManager) : ViewModel() {
    val days = mutableStateListOf<LocalDate>()
    val search = SearchField()
    val dayCache = mutableMapOf<LocalDate, List<SyncedEvent>>()
    val bankingEntityCache = mutableMapOf<LocalDate, List<BankingEntity>>()
    val dayAmount = MutableStateFlow(2)
    var selectDayPopup = MutableStateFlow(false)
    val proposedEvents = mutableStateListOf<ProposedEvent>()
    val futureBankEntries = mutableStateListOf<BankingEntity>()
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

    var lastEventUpdateTs by mutableLongStateOf(0L)
        private set

    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true

        viewModelScope.launch {
            // Load proposed events
            proposedEvents.addAll(autoDetectEvents(context, settings))

            // Load future bank entries
            val payments = JSONArray(storage.prefs.getString("payments", null) ?: "[]")
            val entries = mutableListOf<BankingEntity>()
            for (i in 0 until payments.length()) {
                val it = payments.getJSONObject(i)
                entries.add(
                    BankingEntity(
                        "",
                        false,
                        false,
                        -it.getInt("amount"),
                        "EUR",
                        "",
                        it.getLong("timestamp"),
                        it.getString("to"),
                        "",
                        "",
                        "",
                        0,
                        it.getLong("timestamp"),
                        it.getLong("timestamp")
                    )
                )
            }
            futureBankEntries.addAll(entries)
        }
    }

    fun removeProposedEvent(event: ProposedEvent, context: Context) {
        proposedEvents.remove(event)
        event.ignoreProposed(storage, context)
    }

    fun refreshEvents() {
        lastEventUpdateTs = System.currentTimeMillis()
    }

    fun setDay(selectedDay: LocalDate) {
        days.clear()
        days.add(selectedDay)
    }
}