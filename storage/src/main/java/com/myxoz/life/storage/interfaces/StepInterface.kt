package com.myxoz.life.storage.interfaces

import com.myxoz.life.storage.caches.PerformantCache
import com.myxoz.life.storage.caches.PerformantCache.Companion.overwrite
import com.myxoz.life.storage.dbwrapper.days.ProposedStepsDao
import com.myxoz.life.storage.dbwrapper.days.ProposedStepsEntity
import com.myxoz.life.storage.dbwrapper.state.KeyValueStore
import com.myxoz.life.storage.dbwrapper.state.KeyValueStore.StoreScope.Companion.edit
import com.myxoz.life.storage.dbwrapper.state.KeyValueStorePrefix
import com.myxoz.life.storage.dbwrapper.state.KeyValueType
import com.myxoz.life.storage.interfaces.utils.Cached
import com.myxoz.life.storage.interfaces.utils.Cached.Companion.cached
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

class StepInterface internal constructor(
    private val dao: ProposedStepsDao,
    private val keyValueStore: KeyValueStore,
    private val appScope: CoroutineScope
) {
    private val _steps = MutableStateFlow(0L)
    private var lastStepsAmount = keyValueStore.get(lastStepsAmountKey, 0)
    private val insertedSteps = PerformantCache<Long, Cached<Int>>(appScope) {
        dao.getStepsByDay(it)?.steps.cached
    }
    fun getStepsFor(day: LocalDate) = insertedSteps.flowByKey(day.toEpochDay())
    fun getStepsCached(day: LocalDate) = insertedSteps.getCached(day.toEpochDay())
    private val stepsAtMidnight = MutableStateFlow(keyValueStore.get(stepsAtMidnightKey, 0))
    private var lastDateSaved = keyValueStore.get(lastStepsDateKey,LocalDate.now().minusDays(1))
    val steps = _steps.combine(stepsAtMidnight) { steps, atMidnight ->  steps - atMidnight }
    fun initialSteps() = _steps.value - stepsAtMidnight.value
    /** Inserts steps as the last date where steps were saved */
    private suspend fun resetStepsAsOldDay(totalSteps: Long? = null, midnightSteps: Long? = null) {
        val today = LocalDate.now()
        val stepsToInsert = (lastStepsAmount - stepsAtMidnight.value).coerceAtLeast(0).toInt()
        insertedSteps.overwrite(lastDateSaved.toEpochDay(), stepsToInsert)
        dao.insertSteps(ProposedStepsEntity(lastDateSaved.toEpochDay(), stepsToInsert))

        // ----------

        lastDateSaved = today
        stepsAtMidnight.value = midnightSteps ?: totalSteps ?: lastStepsAmount
        lastStepsAmount = totalSteps ?: lastStepsAmount
        keyValueStore.edit {
            set(lastStepsDateKey, lastDateSaved)
            set(stepsAtMidnightKey, stepsAtMidnight.value)
            set(lastStepsAmountKey, lastStepsAmount)
        }
        _steps.value = lastStepsAmount
    }
    fun insertYesterdayIfNeeded(today: LocalDate) {
        if(lastDateSaved == today.minusDays(1)) {
            appScope.launch {
                resetStepsAsOldDay(lastStepsAmount)
            }
        }
    }
    fun updateSteps(totalSensorSteps: Long){
        appScope.launch {
            val today = LocalDate.now()
            // Reboot (or random sensor reset FUCK OEMs)
            // Fuck Samsung again because they sometimes seam to remove a small amount of steps and don't fully reset the sensor,
            // I will log this and TODO look at the logs and understand how the sensor works
            // Logs: Max Dif: 1      Yes but just WHYYYY?
            /*
From 149087 to 149086 at 1788522403350 (04.09.2026 13:46:43)
From 144820 to 144819 at 1788434111568 (03.09.2026 13:15:11)
From 136735 to 136734 at 1788356177733 (02.09.2026 15:36:17)
From 42971 to 42970 at 1783355725998 (06.07.2026 18:35:25)
From 4574 to 4573 at 1783007984264 (02.07.2026 17:59:44)
From 70633 to 70632 at 1781364966550 (13.06.2026 17:36:06)
From 52801 to 52800 at 1781174249920 (11.06.2026 12:37:29)
From 1956 to 1955 at 1780046794492 (29.05.2026 11:26:34)
From 2590 to 2589 at 1779874841548 (27.05.2026 11:40:41)
From 1867 to 1866 at 1779869193267 (27.05.2026 10:06:33)
From 374 to 1 at 1779866989205 (27.05.2026 09:29:49)
From 70543 to 70542 at 1778379346303 (10.05.2026 04:15:46)
From 112911 to 112910 at 1777204063958 (26.04.2026 13:47:43)
From 106040 to 106039 at 1777039235630 (24.04.2026 16:00:35)
From 46309 to 46308 at 1775135661976 (02.04.2026 15:14:21)
From 10823 to 10822 at 1774192727221 (22.03.2026 16:18:47)
From 30681 to 30680 at 1773570464845 (15.03.2026 11:27:44)
            */
            if(totalSensorSteps+1 < lastStepsAmount){
                keyValueStore.map(stepsResetLogKey) {
                    val mutable = it?.toMutableList()
                    mutable?.add("$lastStepsAmount;$totalSensorSteps;${System.currentTimeMillis()}")
                    mutable?:listOf()
                }
                // To new day (edge case e.x. reboot at midnight)
                if(today > lastDateSaved) {
                    resetStepsAsOldDay(totalSensorSteps, 0)
                } else { // Step drop durring the day
                    val atMid = -lastStepsAmount + stepsAtMidnight.value
                    keyValueStore.edit {
                        set(stepsAtMidnightKey, atMid)
                        set(lastStepsAmountKey, totalSensorSteps)
                    }
                    stepsAtMidnight.value = atMid
                    lastStepsAmount = totalSensorSteps
                    _steps.value = totalSensorSteps
                }
            } else if (today > lastDateSaved) {
                resetStepsAsOldDay(totalSensorSteps)
            }


            val delta = totalSensorSteps - lastStepsAmount
            _steps.value = totalSensorSteps
            if (delta >= 10) {
                lastStepsAmount = totalSensorSteps
                lastDateSaved = today
                savePersistentSteps(today)
            }
        }
    }
    private fun savePersistentSteps(date: LocalDate) {
        keyValueStore.edit {
            set(lastStepsAmountKey, lastStepsAmount)
            set(lastStepsDateKey, date)
        }
    }
    // internal fun debugGetRawSteps() = _steps
    internal companion object {
        val keyValueStorePrefix = KeyValueStorePrefix("steps")
        val lastStepsDateKey = keyValueStorePrefix.keyOfType("lastStepsDate", KeyValueType.LocalDateType)
        val lastStepsAmountKey = keyValueStorePrefix.keyOfType("lastStepsAmount", KeyValueType.LongType)
        val stepsAtMidnightKey = keyValueStorePrefix.keyOfType("stepsAtMidnight", KeyValueType.LongType)
        val stepsResetLogKey = keyValueStorePrefix.keyOfType("debugLog", KeyValueType.StringListType)
    }
}