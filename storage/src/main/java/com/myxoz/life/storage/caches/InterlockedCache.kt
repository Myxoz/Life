package com.myxoz.life.storage.caches

import com.myxoz.life.core.Rangable
import com.myxoz.life.storage.interfaces.utils.Cached
import com.myxoz.life.storage.interfaces.utils.Cached.Companion.cached
import com.myxoz.life.storage.interfaces.utils.RangeInterlockedCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

internal open class InterlockedCache<K: Any, I: Any, V: Any, L: Any>(
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
        fun <K: Any, I: Any, V: Any> InterlockedCache<K, I, Cached<V>, V>.overwrite(key: K, value: V) {
            overwrite(key, value.cached)
        }
        fun <K: Any, I: Any, V: Any> InterlockedCache<K, I, Cached<V>, V>.remove(key: K) {
            overwrite(key, Cached.Null)
        }
        fun <K: Any, V: Any> dayedSame(
            fetchScope: CoroutineScope,
            isEqual: (first: V, other: V) -> Boolean,
            toInterlockedRange: (element: V) -> List<LocalDate>,
            keyBy: (item: V) -> K,
            fetchSingleContent: suspend (K) -> V,
            nativeFetchRange: suspend (Rangable<LocalDate>) -> List<V>,
            update: ((key: K, new: V) -> Unit)? = null,
        ) = RangeInterlockedCache(
            fetchScope,
            isEqual,
            toInterlockedRange,
            keyBy,
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
            nativeFetchRange: suspend (Rangable<LocalDate>) -> List<V>,
            update: ((key: K, new: Cached<V>) -> Unit)? = null
        ) = RangeInterlockedCache<K, LocalDate, Cached<V>, V>(
            fetchScope,
            isEqual,
            toInterlockedRange,
            keyBy,
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
        ) = InterlockedCache(
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