package com.myxoz.life.lifecore.flows

import kotlinx.coroutines.flow.StateFlow

class StateFlowCache<K, V>(private val generator: (K) -> StateFlow<V>){
    private val map = mutableMapOf<K, StateFlow<V>>()
    fun get(key: K): StateFlow<V> = map.computeIfAbsent(key, generator)
}
