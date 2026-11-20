package com.myxoz.life.lifecore.flows

import kotlinx.coroutines.flow.Flow

class FlowCache<K, V>(private val generator: (K) -> Flow<V>){
    private val map = mutableMapOf<K, Flow<V>>()
    fun get(key: K): Flow<V> = map.computeIfAbsent(key, generator)
}