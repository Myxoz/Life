package com.myxoz.life.ui.feed.instantevents

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.myxoz.life.repositories.TransportRepo
import com.myxoz.life.ui.NavPath

class InstantEventsViewModel(
    savedStateHandle: SavedStateHandle,
    val transport: TransportRepo
): ViewModel() {
    val instantEvents = transport.instantEvents.consume(
        savedStateHandle.get<Long>(NavPath.INSTANT_EVENT_SELECTION.parameterName) ?: error("Opened without an id")
    )
}