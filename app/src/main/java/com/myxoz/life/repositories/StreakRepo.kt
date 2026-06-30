package com.myxoz.life.repositories

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import com.myxoz.life.api.API
import com.myxoz.life.api.extensions.StreakExtendable
import com.myxoz.life.api.syncables.ExtensionSyncable
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.utils.Cached
import com.myxoz.life.storage.interfaces.utils.StateFlowCache
import com.myxoz.life.storage.interfaces.utils.subscribeToColdFlow
import com.myxoz.life.utils.def
import kotlinx.coroutines.flow.MutableStateFlow

class StreakRepo(private val repos: DatabaseInterface): ViewModel() {
    val days = MutableStateFlow(listOf<Long>())
    val allStreaks = repos.aggregators.streakAggregator.streaks
    val scrollState = ScrollState(0)
    val scrollStateSubScreen = LazyListState(0)
    val currentStreakCache = StateFlowCache<Long, Cached<StreakExtendable.StreakItem>?> { id ->
        repos.aggregators.streakAggregator.getStreak(id).subscribeToColdFlow(repos.appScope, null)
    }
    fun getStreak(id: Long) = currentStreakCache.get(id)
    fun getCurrentStreak(streak: StreakExtendable.StreakItem, timestamp: Long) = repos.aggregators.streakAggregator.getCurrentStreak(streak, timestamp)
    fun getCurrentStreakValue(streak: StreakExtendable.StreakItem, timestamp: Long, timespan: StreakExtendable.Timespan = streak.timespan) = repos.aggregators.streakAggregator.getCurrentStreakValue(streak, timespan, timestamp)
    fun getMaxValueFor(streak: StreakExtendable.StreakItem, timespan: StreakExtendable.Timespan = streak.timespan) = repos.aggregators.streakAggregator.getMaxValue(streak, timespan)
    suspend fun saveAndSyncStreak(item: StreakExtendable.StreakItem, delete: Boolean = false) {
        repos.aggregators.streakAggregator.invalidateCacheFor(item)
        repos.extensionInterface.updateAndSyncWith(
            ExtensionSyncable.ExtensionSyncableType.Streak,
        ) { old ->
            StreakExtendable(
                if (item.id > 0)
                    if (delete)
                        old.value?.streaks?.filter { it.id != item.id } ?: listOf()
                    else
                        old.value?.streaks?.map { if (it.id == item.id) item else it } ?: listOf()
                else
                    old.value?.streaks.def(listOf()) + listOf(item.copy(id = API.Companion.generateId()))
            )
        }
    }

    val dayFlow = repos.calendarInterface.todayFlow
    val editingStreak = MutableStateFlow<StreakExtendable.StreakItem?>(null)
}