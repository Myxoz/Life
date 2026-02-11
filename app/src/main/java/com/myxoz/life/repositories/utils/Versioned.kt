package com.myxoz.life.repositories.utils

import com.myxoz.life.repositories.utils.Versioned.Companion.nextOrInit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Versioned<T>(val version: Long, val data: T) {
    val nextVersion: Long get () = version+1
    fun next(data: T): Versioned<T> = Versioned(nextVersion, data)

    companion object {
        fun <T> Versioned<T>?.nextOrInit(data: T): Versioned<T> = Versioned(this?.nextVersion ?: 0, data)
    }
}

val <T> T.newVer: Versioned<T>
    get() = Versioned(0, this)
/**
 * @param nativeFetchKey If data gets requested use this lambda to provide it. This will be called once or, if the values gets inserted with e.x. overwrite, never
 * @param onValueUpdate A lambda that will be called on each value update for the specific key, use this to update other caches.
 * This is not garanteed to update after or before the actual cache does. Always use the provided arguments, never the real cache.
 */
class VersionedCache<K, T>(private val nativeFetchKey: suspend (K) -> T, private val onValueUpdate: (suspend (key: K, old:T?, new: T) -> Unit)? = null){
    private val _flow = MutableStateFlow<Map<K, Versioned<T>>>(mapOf())
    // Privated until .flowByKey is not enough
    private val flow: StateFlow<Map<K, Versioned<T>>> = _flow

    /** Caches key to prepare for future cache hits */
    suspend fun cacheDataForKey(key: K){
        if(key !in _flow.value) {
            fetchForKey(key)
        }
    }
    fun prepare(scope: CoroutineScope, key: K) {
        if(key !in _flow.value) {
            scope.launch {
                fetchForKey(key)
            }
        }
    }
    /** Handles initial data flow and returns a maped flow for the key */
    fun flowByKey(scope: CoroutineScope, key: K): Flow<Versioned<T>?> {
        prepare(scope, key)
        return flow.map { it[key] }
    }
    fun flowByKeys(scope: CoroutineScope, keys: List<K>): Flow<List<Versioned<T>?>> {
        keys.forEach { prepare(scope, it) }
        return flow.map { map -> keys.map { map[it] } }
    }
    suspend fun fetchForKey(key: K): Versioned<T> {
        val fetched = nativeFetchKey(key).newVer
        _flow.update { it + (key to fetched) }
        onValueUpdate?.invoke(key, null, fetched.data)
        return fetched
    }
    suspend fun get(key: K): Versioned<T>{
        return _flow.value[key] ?: fetchForKey(key)
    }
    /** Checks if this key already exists */
    fun hasKey(key: K) = key in _flow.value
    /** Updates a single key using lambda and value. For batched changes use [updateKeysWith] */
    suspend fun updateWith(key: K, runWith: suspend (value: T)->T) {
        val old = get(key)
        val new = old.next(runWith(old.data))
        _flow.update { it + (key to new)}
        onValueUpdate?.invoke(key, old.data, new.data)
    }
    /** Batches updates based on list of keys using lambda and value. For single changes use [updateWith] */
    suspend fun updateKeysWith(keys: List<K>, runWith: suspend (value: T)->T) {
        val oldValues = keys.map { it to get(it) }
        val newValues = oldValues.map {
            val key = it.first
            val old = it.second
            val new = old.next(runWith(it.second.data))
            onValueUpdate?.invoke(key, old.data, new.data)
            key to new
        }
        _flow.update { it + newValues }
    }
    /** Puts the provided value in the map. Nativefetch will never be called */
    suspend fun overwrite(key: K, value: T) {
        val old = _flow.value[key]
        val new = old.nextOrInit(value)
        _flow.update { it + (key to new) }
        onValueUpdate?.invoke(key, old?.data, new.data)
    }
    suspend fun overwriteAll(new: List<Pair<K, T>>) {
        val newValues = new.map {
            val key = it.first
            val old = _flow.value[key]
            val new = old.nextOrInit(it.second)
            onValueUpdate?.invoke(key, old?.data, new.data)
            key to new
        }
        _flow.update { it + newValues }
    }

    fun allValuesFlow() = _flow.map { it.values.toList() }
    fun allMapedFlows() = _flow.map { allMapped -> allMapped.mapValues { it.value.data } }
    fun getCached(it: K) = _flow.value[it]
}
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