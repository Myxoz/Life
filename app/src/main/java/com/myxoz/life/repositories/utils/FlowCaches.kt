package com.myxoz.life.repositories.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class FlowCache<K, V>(
    private val generator: (K) -> Flow<V>
){
    private val map = mutableMapOf<K, Flow<V>>()
    fun get(key: K): Flow<V> {
        val item = map[key]
        if(item!=null) return item
        val new = generator(key)
        map[key] = new
        return new
    }
}
class StateFlowCache<K, V>(
    private val generator: (K) -> StateFlow<V>
){
    private val map = mutableMapOf<K, StateFlow<V>>()
    fun get(key: K): StateFlow<V> {
        val item = map[key]
        if(item!=null) return item
        val new = generator(key)
        map[key] = new
        return new
    }
}