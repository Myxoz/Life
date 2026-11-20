package com.myxoz.life

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
interface WritableState<T> : StateFlow<T> {
    fun set(value: T)
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
fun <T> StateFlow<T>.setter(
    scope: CoroutineScope,
    setter: suspend (T) -> Unit,
): WritableState<T> {
    val state = this.stateIn(
        scope,
        SharingStarted.WhileSubscribed(5000),
        initialValue = this.value
    )

    return object : WritableState<T>, StateFlow<T> by state {

        override fun set(value: T) {
            scope.launch {
                setter(value)
            }
        }
    }
}
