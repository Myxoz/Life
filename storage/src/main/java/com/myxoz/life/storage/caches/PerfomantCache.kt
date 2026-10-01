package com.myxoz.life.storage.caches

import com.myxoz.life.storage.interfaces.utils.Cached
import com.myxoz.life.storage.interfaces.utils.Cached.Companion.cached
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

internal open class PerformantCache<K: Any, V: Any>(
    private val fetchScope: CoroutineScope,
    private val fetchKey: suspend (key: K) -> V
) {
    protected val currentlyFetching = ConcurrentHashMap<K, Deferred<V>>()
    protected val internalValueMap = ConcurrentHashMap<K, V>()
    private val flow = MutableStateFlow<Map<K, V>>(emptyMap())
    val content: Flow<Map<K, V>> = flow
    private fun needsFetching(key: K) = !hasCached(key) && !currentlyFetching.containsKey(key)
    private suspend fun getCachedOrFetch(key: K): V{
        val cached = internalValueMap[key]
        if(cached != null) return cached
        return currentlyFetching.computeIfAbsent(key) {
            getDeferred(it)
        }.await()
    }
    private suspend fun getCachedOrFetchAndCommit(key: K): V{
        val writeToMap = needsFetching(key)
        val new = getCachedOrFetch(key)
        if(writeToMap) commitMap()
        return new
    }
    private fun getDeferred(key: K): Deferred<V> {
        val completable = CompletableDeferred<V>()
        fetchScope.launch {
            val fetched = fetchKey(key)
            internalValueMap[key] = fetched
            completable.complete(fetched)

            @Suppress("DeferredResultUnused")
            currentlyFetching.remove(key)
        }
        return completable
    }
    private suspend fun getCachedOrFetchAll(keys: List<K>): List<Pair<K, V>>{
        val valueMap = mutableMapOf<K, V>()
        val remainingKeys = keys.toMutableSet()
        apply {
            remainingKeys.toSet().forEach { key ->
                val cached = internalValueMap[key]
                if(cached != null) {
                    valueMap[key] = cached
                    remainingKeys.remove(key)
                }
            }
            if(remainingKeys.isEmpty()) return@apply
            remainingKeys
                .map {
                    currentlyFetching[it] ?: getDeferred(it)
                }
                .awaitAll()
                .zip(remainingKeys)
                .forEach {
                    valueMap[it.second] = it.first
                }
        }
        return valueMap.entries.map { it.key to it.value }
    }
    private suspend fun getAndCommitUnfetchedValues(keys: List<K>): List<Pair<K, V>> {
        val needFetching = keys.filter { needsFetching(it) }
        if(needFetching.isEmpty()) return emptyList()
        val new = needFetching
            .zip(
                needFetching
                    .map { getDeferred(it) }
                    .awaitAll()
            )
        commitMap()
        return new
    }
    fun assureIsFetchingOrCached(key: K){
        if(needsFetching(key)) fetchScope.launch {
            getDeferred(key).await()
            commitMap()
        }
    }

    fun assureIsFetchingOrCachedAll(keys: List<K>){
        fetchScope.launch {
            getAndCommitUnfetchedValues(keys)
        }
    }

    protected fun commitMap(){
        flow.update { internalValueMap.toMap() }
    }
    suspend fun update(key: K, runWith: (V)->V) {
        val old = getCachedOrFetch(key)
        internalValueMap[key] = runWith(old)
        commitMap()
    }
    suspend fun updateAll(keys: List<K>, runWith: (V)->V) {
        val old = getCachedOrFetchAll(keys)
        internalValueMap += old.map { it.first to runWith(it.second) }
        commitMap()
    }
    fun overwrite(key: K, value: V) {
        internalValueMap[key] = value
        commitMap()
    }
    fun hasCached(key: K) = internalValueMap.containsKey(key)
    fun getCached(key: K) = internalValueMap[key]
    suspend fun getValue(key: K): V = getCachedOrFetchAndCommit(key)
    fun overwriteAll(values: List<Pair<K,V>>) {
        internalValueMap += values
        commitMap()
    }
    // Added the two distinctUntilChanged here. Might lead to cache inconsistencies
    fun flowByKey(key: K): Flow<V?> {
        assureIsFetchingOrCached(key)
        return content.map {
            it[key]
        }.distinctUntilChanged()
    }
    val allValuesFlow = content.map { it.values.toList() }
    fun flowsByKey(keys: List<K>): Flow<List<V>> {
        assureIsFetchingOrCachedAll(keys)
        return content.map { map ->
            keys.mapNotNull { key -> map[key] }
        }.distinctUntilChanged()
    }

    companion object {
        fun <K: Any, V: Any> PerformantCache<K, Cached<V>>.overwrite(key: K, value: V) {
            overwrite(key, value.cached)
        }
    }
}