package com.myxoz.life.storage.interfaces.utils

import com.myxoz.life.storage.interfaces.utils.Cached.Companion.cached
import com.myxoz.life.utils.datesThrough
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

open class PerformantCache<K: Any, V: Any>(
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

class RangedPerformantCache<K: Any, V: Any>(
    private val fetchScope: CoroutineScope,
    private val fetchKey: suspend (key: K) -> V,
    private val rangeToKeys: (from: K, to: K) -> List<K>,
    private val nativeFetchRange: suspend (from: K, to: K) -> List<V>,
    private val keyBy: (V) -> K
): PerformantCache<K, V>(fetchScope, fetchKey){
    suspend fun fetchRange(from: K, to: K): List<V> {
        val between = rangeToKeys(from, to).toSet()
        // All days already cached
        if(between.all { internalValueMap.containsKey(it) }) return between
            .mapNotNull { getCached(it) }
        val defered = between.associateWith { CompletableDeferred<V>() }
        currentlyFetching += defered

        val fetchedRange = nativeFetchRange(from, to)
        val ranged = fetchedRange.associateBy { keyBy(it) }
        ranged.forEach { (interlocked, elements) ->
            defered[interlocked]?.complete(elements)
        }

        internalValueMap += ranged
        commitMap()
        defered.keys.forEach {
            @Suppress("DeferredResultUnused")
            currentlyFetching.remove(it)
        }
        return fetchedRange
    }
    fun flowByRange(from: K, to: K): Flow<List<V>> {
        fetchScope.launch {
            fetchRange(from, to)
        }
        val between = rangeToKeys(from, to).toSet()
        return content.mapNotNull { map ->
            buildList {
                val set = mutableSetOf<K>()
                for (key in between) {
                    // Return if caches are not ready yet
                    val item = map[key] ?: return@mapNotNull null
                    if(set.add(key)) {
                        add(item)
                    }
                }
            }
        }
    }
}


// TODO Consider replacing the isEquals with a keyBy == keyBy call
open class PerformantInterlockedCache<K: Any, I: Any, V: Any, L: Any>(
    private val fetchScope: CoroutineScope,
    private val isEqual: (first: V, other: L) -> Boolean,
    private val toInterlockedRange: (element: V) -> List<I>,
    private val keyBy: (item: L) -> K,
    private val fetchSingleContent: suspend (K) -> V,
    private val fetchSingle: suspend (I) -> List<L>,
    private val converter: (L) -> V,
    private val toListItem: (V) -> L?,
    private val update: ((key: K, new: V) -> Unit)? = null,
) {
    protected val itemCache = PerformantCache(fetchScope, fetchSingleContent)
    protected val interlockedContent = ConcurrentHashMap<I, List<L>>()
    private val flow = MutableStateFlow<Map<I, List<L>>>(emptyMap())
    protected val currentlyFetching = ConcurrentHashMap<I, Deferred<List<L>>>()
    protected fun commitMap(){
        flow.update { interlockedContent.toMap() }
    }
    protected var allLoaded = false
    /** Calling this function will mark all entries as loaded.
     *  There will be no more calls to [fetchSingle].
     *  This is the same as saying: The whole database-table is uploaded to the repo. No more requests need to be made.
     *  This is an advanced call only to be used if all entries of a database where fetched.
     **/
    fun markAllEntriesAsLoaded(){
        allLoaded = true
    }
    protected fun getDeferred(key: I): Deferred<List<L>> {
        return currentlyFetching.computeIfAbsent(key) {
            val completable = CompletableDeferred<List<L>>()
            fetchScope.launch {
                val fetched = fetchSingle(key)
                interlockedContent[key] = fetched
                itemCache.overwriteAll(
                    fetched.map { keyBy(it) to converter(it) }
                )
                completable.complete(fetched)

                @Suppress("DeferredResultUnused")
                currentlyFetching.remove(key)
            }
            completable
        }
    }

    suspend fun fetchOrGetInterlocked(interlocked: I): List<L> {
        val cached = interlockedContent[interlocked]
        if(cached != null) return cached
        if (allLoaded) {
            interlockedContent[interlocked] = listOf()
            commitMap()
            return listOf()
        } else {
            val alreadyFetching = currentlyFetching.containsKey(interlocked)
            val result = getDeferred(interlocked).await()
            if(!alreadyFetching) {
                commitMap()
            }
            return result
        }
    }
    private fun requireInterlocked(interlocked: I) {
        if(!interlockedContent.containsKey(interlocked)) {
            if (allLoaded) {
                interlockedContent[interlocked] = listOf()
                commitMap()
            } else {
                // Already a fetching process going on
                if(currentlyFetching.containsKey(interlocked)) return
                fetchScope.launch {
                    getDeferred(interlocked).await()
                    commitMap()
                }
            }
        }
    }
    fun getInterlockedFlowFor(interlocked: I): Flow<List<L>?> {
        requireInterlocked(interlocked)
        return flow.map { it[interlocked] }.distinctUntilChanged()
    }
    protected fun getCached(interlocked: I) = interlockedContent[interlocked]
    private fun overwriteSingleNoCommit(key: K, value: V) {
        val oldItem = itemCache.getCached(key)
        val oldInterlocked = if(oldItem != null) toInterlockedRange(oldItem).toSet() else emptySet()
        val newInterlocked = toInterlockedRange(value).toSet()
        val asListItem = toListItem(value)
        // println("$key ($value): $oldItem -> $value ($asListItem). ${oldInterlocked - newInterlocked} -> ${newInterlocked - oldInterlocked} (FROM $oldInterlocked; TO $newInterlocked)")
        if(oldInterlocked == newInterlocked) {
            // Just update the item
            for (interlocked in newInterlocked) {
                val old = getCached(interlocked) ?: continue
                interlockedContent[interlocked] = old.mapNotNull { if(isEqual(value, it)) asListItem else it }
            }
            update?.invoke(key, value)
            return
        }
        if(asListItem != null) {
            val addedTo = newInterlocked - oldInterlocked
            for (date in addedTo) {
                // This was a bug: Writing into unloaded parts should be a noop / would poison cache normally
                // But if allLoaded this is required because the cache will never be populated in another way
                val old = getCached(date) ?: if(allLoaded) listOf() else continue
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
        fun <K: Any, I: Any, V: Any> PerformantInterlockedCache<K, I, Cached<V>, V>.overwrite(key: K, value: V) {
            overwrite(key, value.cached)
        }
        fun <K: Any, I: Any, V: Any> PerformantInterlockedCache<K, I, Cached<V>, V>.remove(key: K) {
            overwrite(key, Cached.Null)
        }
        fun <K: Any, V: Any> dayedSame(
            fetchScope: CoroutineScope,
            isEqual: (first: V, other: V) -> Boolean,
            toInterlockedRange: (element: V) -> List<LocalDate>,
            keyBy: (item: V) -> K,
            fetchSingleContent: suspend (K) -> V,
            nativeFetchRange: suspend (from: LocalDate, to: LocalDate) -> List<V>,
            update: ((key: K, new: V) -> Unit)? = null,
        ) = RangeInterlockedCache(
            fetchScope,
            isEqual,
            toInterlockedRange,
            keyBy,
            { from, to -> from.datesThrough(to) },
            fetchSingleContent,
            nativeFetchRange,
            { it },
            { it },
            update
        )
        fun <K: Any, V: Any> dayedCached(
            fetchScope: CoroutineScope,
            isEqual: (first: Cached<V>, other: V) -> Boolean,
            toInterlockedRange: (element: Cached<V>) -> List<LocalDate>,
            keyBy: (item: V) -> K,
            fetchSingleContent: suspend (K) -> Cached<V>,
            nativeFetchRange: suspend (from: LocalDate, to: LocalDate) -> List<V>,
            update: ((key: K, new: Cached<V>) -> Unit)? = null
        ) = RangeInterlockedCache<K, LocalDate, Cached<V>, V>(
            fetchScope,
            isEqual,
            toInterlockedRange,
            keyBy,
            { from, to -> from.datesThrough(to) },
            fetchSingleContent,
            nativeFetchRange,
            { it.cached },
            { it.value },
            update
        )
        fun <K: Any, I: Any, V: Any> cachedNonRange(
            fetchScope: CoroutineScope,
            isEqual: (first: Cached<V>, other: V) -> Boolean,
            toInterlockedRange: (element: Cached<V>) -> List<I>,
            keyBy: (item: V) -> K,
            fetchSingleContent: suspend (K) -> Cached<V>,
            fetchSingle: suspend (key: I) -> List<V>,
            update: ((key: K, new: Cached<V>) -> Unit)? = null
        ) = PerformantInterlockedCache(
            fetchScope,
            isEqual,
            toInterlockedRange,
            keyBy,
            fetchSingleContent,
            fetchSingle,
            { it.cached },
            { it.value },
            update
        )
    }
}

class RangeInterlockedCache<K: Any, I: Any, V: Any, L: Any>(
    private val fetchScope: CoroutineScope,
    private val isEqual: (first: V, other: L) -> Boolean,
    private val toInterlockedRange: (element: V) -> List<I>,
    private val keyBy: (item: L) -> K,
    private val rangeToKeys: (from: I, to: I) -> List<I>,
    private val fetchSingleContent: suspend (K) -> V,
    private val nativeFetchRange: suspend (from: I, to: I) -> List<L>,
    private val converter: (L) -> V,
    private val toListItem: (V) -> L?,
    private val update: ((key: K, new: V) -> Unit)? = null,
): PerformantInterlockedCache<K, I, V, L>(
    fetchScope, isEqual, toInterlockedRange, keyBy, fetchSingleContent, {nativeFetchRange(it, it)}, converter, toListItem, update
) {
    suspend fun fetchRange(from: I, to: I): List<L> {
        val between = rangeToKeys(from, to).toSet()
        // All days marked as loaded / All days already cached
        if(allLoaded || between.all { interlockedContent.containsKey(it) }) return between
            .flatMap {
                getCached(it) ?: listOf()
            }
            .distinctBy(keyBy)
        val defered = between.associateWith { CompletableDeferred<List<L>>() }
        currentlyFetching += defered

        val fetchedRange = nativeFetchRange(from, to)
        val ranged = between.associateWith { mutableListOf<L>() }
        fetchedRange.forEach { elem ->
            toInterlockedRange(converter(elem)).forEach { interlocked ->
                ranged[interlocked]?.add(elem)
            }
        }
        itemCache.overwriteAll(
            fetchedRange.map {
                elem -> keyBy(elem) to converter(elem)
            }
        )
        ranged.forEach { (interlocked, elements) ->
            defered[interlocked]?.complete(elements)
        }


        interlockedContent += ranged
        commitMap()
        defered.keys.forEach {
            @Suppress("DeferredResultUnused")
            currentlyFetching.remove(it)

        }
        return fetchedRange
    }
    fun flowByRange(from: I, to: I): Flow<List<L>> {
        fetchScope.launch {
            fetchRange(from, to)
        }
        val between = rangeToKeys(from, to).toSet()
        return everythingFlow.mapNotNull { map ->
            val set = mutableSetOf<K>()
            buildList {
                for (key in between) {
                    // Return if caches are not ready yet
                    (map[key] ?: return@mapNotNull null).forEach {
                        if(set.add(keyBy(it))) {
                            add(it)
                        }
                    }
                }
            }
        }
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