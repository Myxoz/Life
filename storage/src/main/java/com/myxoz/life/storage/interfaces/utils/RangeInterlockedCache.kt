package com.myxoz.life.storage.interfaces.utils

import com.myxoz.life.storage.caches.InterlockedCache
import com.myxoz.life.storage.caches.RangedCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch


// TODO Consider replacing the isEquals with a keyBy == keyBy call
internal class RangeInterlockedCache<K: Any, I: Any, V: Any, L: Any>(
    private val fetchScope: CoroutineScope,
    isEqual: (first: V, other: L) -> Boolean,
    private val toInterlockedRange: (element: V) -> List<I>,
    private val keyBy: (item: L) -> K,
    fetchSingleContent: suspend (K) -> V,
    private val nativeFetchRange: suspend (range: RangedCache.Rangable<I>) -> List<L>,
    private val converter: (L) -> V,
    toListItem: (V) -> L?,
    update: ((key: K, new: V) -> Unit)? = null,
): InterlockedCache<K, I, V, L>(
    fetchScope,
    isEqual,
    toInterlockedRange,
    keyBy,
    fetchSingleContent,
    {nativeFetchRange( RangedCache.Rangable.ofSingle(it)) },
    converter,
    toListItem,
    update
) {
    suspend fun fetchRange(range: RangedCache.Rangable<I>): List<L> {
        val between = range.between
        // All days marked as loaded / All days already cached
        if(allLoaded || between.all { interlockedContent.containsKey(it) }) return between
            .flatMap {
                getCached(it) ?: listOf()
            }
            .distinctBy(keyBy)
        val defered = between.associateWith { CompletableDeferred<List<L>>() }
        currentlyFetching += defered

        val fetchedRange = nativeFetchRange(range)
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
    fun flowByRange(range: RangedCache.Rangable<I>): Flow<List<L>> {
        fetchScope.launch {
            fetchRange(range)
        }
        val between = range.between
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