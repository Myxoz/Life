package com.myxoz.life.viewmodels

import androidx.lifecycle.ViewModel
import com.myxoz.life.screens.feed.instantevents.InstantEvent
import kotlinx.coroutines.flow.MutableStateFlow

class InstantEventsViewModel: ViewModel() {
    val lookedAtInstantEvents = MutableStateFlow(listOf<InstantEvent>())
}