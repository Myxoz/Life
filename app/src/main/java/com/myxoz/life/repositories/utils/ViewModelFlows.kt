package com.myxoz.life.repositories.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

fun <T> Flow<T>.subscribeToColdFlow(viewModelScope: CoroutineScope, initial: T) =
    this.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)