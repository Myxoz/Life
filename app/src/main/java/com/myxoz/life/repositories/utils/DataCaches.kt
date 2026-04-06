package com.myxoz.life.repositories.utils

import com.myxoz.life.repositories.utils.Cached.Companion.cached
import com.myxoz.life.utils.daysUntil
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

class PerformantCache<K: Any, V: Any>(
    val fetchScope: CoroutineScope,
    val fetchKey: suspend (key: K) -> V
) {
    private val currentlyFetching = ConcurrentHashMap<K, Deferred<V>>()
    private val internalValueMap = ConcurrentHashMap<K, V>()
    private val flow = MutableStateFlow<Map<K, V>>(emptyMap())
    val content: Flow<Map<K, V>> = flow
    private fun needsFetching(key: K) = !hasCached(key) && !currentlyFetching.containsKey(key)
    private suspend fun fetchForKeyAndCommit(key: K): V {
        val fetched = getDeferred(key).await()
        internalValueMap[key] = fetched
        cleanUpDefered(key)
        commitMap()
        return fetched
    }
    @Suppress("DeferredResultUnused")
    private fun cleanUpDefered(key: K) {
        currentlyFetching.remove(key)
    }
    private suspend fun getCachedOrFetch(key: K): V{
        val cached = internalValueMap[key]
        if(cached != null) return cached
        val cachedDeferred = currentlyFetching[key]
        if(cachedDeferred != null) return cachedDeferred.await()
        return getDeferred(key).await()
    }
    private suspend fun getCachedOrFetchAndCommit(key: K): V{
        val writeToMap = needsFetching(key)
        val new = getCachedOrFetch(key)
        if(writeToMap) {
            internalValueMap[key] = new
            cleanUpDefered(key)
            commitMap()
        }
        return new
    }
    private fun getDeferred(key: K): Deferred<V> {
        val completable = CompletableDeferred<V>()
        currentlyFetching[key] = completable
        fetchScope.launch {
            val fetched = fetchKey(key)
            completable.complete(fetched)
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
                    cleanUpDefered(it.second)
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
        internalValueMap += new
        needFetching.forEach { key -> cleanUpDefered(key) }
        commitMap()
        return new
    }
    fun assureIsFetchingOrCached(key: K){
        if(needsFetching(key)) fetchScope.launch {
            fetchForKeyAndCommit(key)
        }
    }

    fun assureIsFetchingOrCachedAll(keys: List<K>){
         fetchScope.launch {
            getAndCommitUnfetchedValues(keys)
        }
    }

    private fun commitMap(){
        flow.update { internalValueMap.toMap() }
    }
    suspend fun update(key: K, runWith: (V)->V) {
        val old = getCachedOrFetch(key)
        internalValueMap[key] = runWith(old)
        cleanUpDefered(key)
        commitMap()
    }
    suspend fun updateAll(keys: List<K>, runWith: (V)->V) {
        val old = getCachedOrFetchAll(keys)
        internalValueMap += old.map { it.first to runWith(it.second) }
        commitMap()
    }
    fun overwrite(key: K, value: V) {
        internalValueMap += key to value
        commitMap()
    }
    fun hasCached(key: K) = internalValueMap.containsKey(key)
    fun getCached(key: K) = internalValueMap[key]
    suspend fun getValue(key: K): V = getCachedOrFetchAndCommit(key)
    fun overwriteAll(values: List<Pair<K,V>>) {
        internalValueMap += values
        commitMap()
    }
    fun flowByKey(key: K): Flow<V?> {
        assureIsFetchingOrCached(key)
        return content.map {
            it[key]
        }
    }
    val allValuesFlow = content.map { it.values.toList() }

    fun flowsByKey(keys: List<K>): Flow<List<V>> {
        assureIsFetchingOrCachedAll(keys)
        return content.map { map ->
            keys.mapNotNull { key -> map[key] }
        }
    }

    companion object {
        fun <K: Any, V: Any> PerformantCache<K, Cached<V>>.overwrite(key: K, value: V) {
            overwrite(key, value.cached)
        }
    }
}

class PerformantInterlockedCache<K: Any, I: Any, V: Any, L: Any> private constructor(
    private val fetchScope: CoroutineScope,
    private val toInterlockedRange: (element: V) -> List<I>,
    private val isEqual: (first: V, other: L) -> Boolean,
    private val fetchSingle: suspend (K) -> V,
    private val fetchRangeNative: suspend (from: I, to: I) -> List<Pair<K, V>>,
    private val converter: (L) -> V,
    private val toListItem: (V) -> L?,
    private val update: ((key: K, new: V) -> Unit)? = null,
) {
    private val itemCache = PerformantCache<K, V>(fetchScope){ fetchSingle(it) }
    private val interlockedContent = ConcurrentHashMap<I, List<L>>()
    private val flow = MutableStateFlow<Map<I, List<L>>>(emptyMap())
    private val isFetching = ConcurrentHashMap.newKeySet<I>()
    private fun commitMap(){
        flow.update { interlockedContent.toMap() }
    }
    private var allLoaded = false
    /** Calling this function will mark all entries as loaded.
     *  There will be no more calls to [fetchRange].
     *  This is the same as saying: The whole database-table is uploaded to the repo. No more requests need to be made.
     *  This is an advanced call only to be used if all entries of a database where fetched.
     **/
    fun markAllEntriesAsLoaded(){
        allLoaded = true
    }
    suspend fun fetchRange(from: I, to: I): List<L> {
        val fetched = fetchRangeNative(from, to)
        itemCache.overwriteAll(fetched)
        return fetched.mapNotNull { toListItem(it.second) ?: return@mapNotNull null }
    }
    suspend fun fetchOrGetInterlocked(interlocked: I): List<L> {
        if(!interlockedContent.containsKey(interlocked)) {
            if (allLoaded) {
                interlockedContent[interlocked] = listOf()
            } else {
                interlockedContent[interlocked] = fetchRange(interlocked, interlocked)
            }
            commitMap()
        }
        return interlockedContent.getValue(interlocked)
    }
    private fun requireInterlocked(interlocked: I) {
        if(!interlockedContent.containsKey(interlocked)) {
            if (allLoaded) {
                interlockedContent[interlocked] = listOf()
                commitMap()
            } else {
                if (!isFetching.add(interlocked)) return
                fetchScope.launch {
                    interlockedContent[interlocked] = fetchRange(interlocked, interlocked)
                    commitMap()
                }
            }
        }
    }
    fun getInterlockedFlowFor(interlocked: I): Flow<List<L>?> {
        requireInterlocked(interlocked)
        return flow.map { it[interlocked] }
    }
    private fun getCached(interlocked: I) = interlockedContent[interlocked]
    private fun overwriteSingleNoCommit(key: K, value: V) {
        val oldItem = itemCache.getCached(key)
        val oldInterlocked = if(oldItem != null) toInterlockedRange(oldItem).toSet() else emptySet()
        val newInterlocked = toInterlockedRange(value).toSet()
        val asListItem = toListItem(value)
        // ("$key ($value): $oldItem -> $value ($asListItem). ${oldInterlocked - newInterlocked} -> ${newInterlocked - oldInterlocked} (FROM $oldInterlocked; TO $newInterlocked)")
        if(oldInterlocked == newInterlocked) {
            // Just update the item
            for (interlocked in newInterlocked) {
                val old = getCached(interlocked) ?: continue
                interlockedContent[interlocked] = old.mapNotNull { if(isEqual(value, it)) asListItem else it }
            }
            return
        }
        if(asListItem != null) {
            val addedTo = newInterlocked - oldInterlocked
            for (date in addedTo) {
                val old = getCached(date) ?: continue
                if(old.any { isEqual(value, it) }) {
                    // Is already added -> Map
                    interlockedContent[date] = old.map { if(isEqual(value, it)) asListItem else it }
                } else {
                    // Is new -> Add new
                    interlockedContent[date] = old + listOf(asListItem)
                }
            }
        }
        if(oldItem != null) {
            val removeFrom = oldInterlocked - newInterlocked
            for (date in removeFrom) {
                val old = getCached(date)
                if(old != null) interlockedContent[date] = old.filter { !isEqual(oldItem, it) }
            }
        }
        update?.invoke(key, value)
    }
    fun overwrite(key: K, value: V) {
        overwriteSingleNoCommit(key, value)
        itemCache.overwrite(key, value)
        commitMap()
    }
    fun overwriteAll(values: List<Pair<K, V>>) {
        for ((key, value) in values) {
            overwriteSingleNoCommit(key, value)
        }
        itemCache.overwriteAll(values)
        commitMap()
    }
    suspend fun getContent(key: K): V = itemCache.getValue(key)
    val allValuesFlow = itemCache.allValuesFlow
    val everythingFlow: Flow<Map<I, List<L>>> = flow
    fun flowFor(key: K) = itemCache.flowByKey(key)
    fun flowsFor(keys: List<K>) = itemCache.flowsByKey(keys)
    fun getCachedContent(key: K) = itemCache.getCached(key)

    companion object {
        suspend fun <K: Any, V: Any, L: Any> PerformantInterlockedCache<K, LocalDate, V, L>.loadDays(from: LocalDate, to: LocalDate): Map<LocalDate, List<L>> {
            if(allLoaded) return mapOf()
            val between = from.daysUntil(to)

            // All days already cached
            if(between.all { interlockedContent.containsKey(it) }) {
                return between.associateWith {
                    interlockedContent.getValue(it)
                }
            }

            val dayed = mutableMapOf<LocalDate, MutableList<L>>()
            fetchRange(from, to).forEach { elem ->
                toInterlockedRange(converter(elem)).forEach { date ->
                    val old = dayed.getOrPut(date) { mutableListOf() }
                    old += elem
                }
            }
            interlockedContent += dayed
            commitMap()
            return dayed
        }
        fun <K: Any, I: Any, V: Any> PerformantInterlockedCache<K, I, Cached<V>, V>.overwrite(key: K, value: V) {
            overwrite(key, value.cached)
        }
        fun <K: Any, I: Any, V: Any> PerformantInterlockedCache<K, I, Cached<V>, V>.remove(key: K) {
            overwrite(key, Cached.Null)
        }
        fun <K: Any, V: Any> dayedSame(
            fetchScope: CoroutineScope,
            toDateRange: (element: V) -> List<LocalDate>,
            isEqual: (first: V, other: V) -> Boolean,
            fetchSingle: suspend (K) -> V,
            fetchDays: suspend (from: LocalDate, to: LocalDate) -> List<Pair<K, V>>,
            update: ((key: K, new: V) -> Unit)? = null
        ) = PerformantInterlockedCache(
            fetchScope,
            toDateRange,
            isEqual,
            fetchSingle,
            fetchDays,
            { it },
            { it },
            update
        )
        fun <K: Any, V: Any> dayedCached(
            fetchScope: CoroutineScope,
            toDateRange: (element: Cached<V>) -> List<LocalDate>,
            isEqual: (first: Cached<V>, other: V) -> Boolean,
            fetchSingle: suspend (K) -> Cached<V>,
            fetchDays: suspend (from: LocalDate, to: LocalDate) -> List<Pair<K, Cached<V>>>,
            update: ((key: K, new: Cached<V>) -> Unit)? = null
        ) = PerformantInterlockedCache<K, LocalDate, Cached<V>, V>(
            fetchScope,
            toDateRange,
            isEqual,
            fetchSingle,
            fetchDays,
            { it.cached },
            { it.value },
            update
        )
        fun <K: Any, I: Any, V: Any> same(
            fetchScope: CoroutineScope,
            toInterlockedRange: (element: V) -> List<I>,
            isEqual: (first: V, other: V) -> Boolean,
            fetchSingle: suspend (K) -> V,
            fetchDays: suspend (from: I, to: I) -> List<Pair<K, V>>,
            update: ((key: K, new: V) -> Unit)? = null
        ) = PerformantInterlockedCache(
            fetchScope,
            toInterlockedRange,
            isEqual,
            fetchSingle,
            fetchDays,
            { it },
            { it },
            update
        )
        fun <K: Any, I: Any, V: Any> cached(
            fetchScope: CoroutineScope,
            toInterlockedRange: (element: Cached<V>) -> List<I>,
            isEqual: (first: Cached<V>, other: V) -> Boolean,
            fetchSingle: suspend (K) -> Cached<V>,
            fetchDays: suspend (from: I, to: I) -> List<Pair<K, Cached<V>>>,
            update: ((key: K, new: Cached<V>) -> Unit)? = null
        ) = PerformantInterlockedCache(
            fetchScope,
            toInterlockedRange,
            isEqual,
            fetchSingle,
            fetchDays,
            { it.cached },
            { it.value },
            update
        )
    }
}

sealed class Cached<out T: Any>(val value: T?) {
    data class Value<T: Any>(val content: T) : Cached<T>(content)
    object Null: Cached<Nothing>(null)
    companion object {
        fun <T> by(it: T?) = if(it != null) Value(it) else Null
        val <T: Any> T?.cached: Cached<T>
            get() = if(this != null) Value(this) else Null
    }
}