package com.myxoz.life.storage.caches

import com.myxoz.life.core.UnixWeek
import com.myxoz.life.core.atEndAsMillis
import com.myxoz.life.core.atStartAsMillis
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

internal class RangedCache<K: Any, V: Any>(
    private val fetchScope: CoroutineScope,
    private val fetchKey: suspend (key: K) -> V,
    private val nativeFetchRange: suspend (Rangable<K>) -> List<V>,
    private val keyBy: (V) -> K
): PerformantCache<K, V>(fetchScope, fetchKey){
    suspend fun fetchRange(range: Rangable<K>): List<V> {
        val between = range.between
        // All days already cached
        if(between.all { internalValueMap.containsKey(it) }) return range.between
            .mapNotNull { getCached(it) }
        val defered = between.associateWith { CompletableDeferred<V>() }
        currentlyFetching += defered

        val fetchedRange = nativeFetchRange(range)
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
    fun flowByRange(range: Rangable<K>): Flow<List<V>> {
        fetchScope.launch {
            fetchRange(range)
        }
        return content.mapNotNull { map ->
            buildList {
                val set = mutableSetOf<K>()
                for (key in range.between) {
                    // Return if caches are not ready yet
                    val item = map[key] ?: return@mapNotNull null
                    if(set.add(key)) {
                        add(item)
                    }
                }
            }
        }
    }
    internal data class Rangable<T>(val from: T, val to: T, val between: Set<T>) {
        companion object {
            fun <T> ofSingle(single: T) = Rangable(single, single, setOf(single))
            fun LocalDate.rangableTo(other: LocalDate): Rangable<LocalDate> = Rangable(this, other,
                generateSequence(this) { it.plusDays(1) }
                    .takeWhile { !it.isAfter(other) }
                    .toSet()
            )
            fun UnixWeek.toRange() = start.rangableTo(end)
            fun Rangable<LocalDate>.startAndEndInMs(zone: ZoneId) = from.atStartAsMillis(zone) to to.atEndAsMillis(zone)
        }
    }
}