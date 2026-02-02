package com.myxoz.life.repositories

import android.content.SharedPreferences
import com.myxoz.life.dbwrapper.ProposedStepsDao
import com.myxoz.life.dbwrapper.ProposedStepsEntity
import com.myxoz.life.repositories.utils.subscribeToColdFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

class StepRepo(
    private val dao: ProposedStepsDao,
    private val stepsPrefs: SharedPreferences,
    private val appScope: CoroutineScope
) {
    private val _steps = MutableStateFlow(0L)
    private var lastSavedSteps = stepsPrefs.getLong("saved_steps", 0L)
    private val _lastInsertedSteps = MutableStateFlow(0)
    val lastInsertedSteps: StateFlow<Int> = _lastInsertedSteps
    private val stepsAtMidnight = MutableStateFlow(stepsPrefs.getLong("steps_at_midnight", 0L))
    private var lastDateSaved = stepsPrefs.getLong("last_steps_date", 0L)
    private var lastRebootTs = stepsPrefs.getLong("last_reboot_ts", 0L)
    val steps = _steps.combine(stepsAtMidnight){ steps, atMidnight ->  steps - atMidnight }
        .subscribeToColdFlow(appScope, _steps.value - stepsAtMidnight.value)
    init {
        val currentRebootTs = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()
        if (currentRebootTs - lastRebootTs > 30_000) { // Has rebooted
            appScope.launch {
                resetIfNewDay()
            }
        }
    }
    /** Inserts steps as the last date where steps were saved */
    private suspend fun resetIfNewDay(totalSteps: Long? = null) {
        val nowDay = LocalDate.now().toEpochDay()
        val stepsToInsert = (lastSavedSteps - stepsAtMidnight.value).coerceAtLeast(0).toInt()
        dao.insertSteps(ProposedStepsEntity(lastDateSaved, stepsToInsert))

        lastDateSaved = nowDay
        stepsAtMidnight.value = totalSteps ?: lastSavedSteps
        lastSavedSteps = totalSteps ?: lastSavedSteps
        _lastInsertedSteps.value = stepsToInsert
        stepsPrefs.edit().apply {
            putLong("last_steps_date", lastDateSaved)
            putLong("steps_at_midnight", stepsAtMidnight.value)
            putLong("saved_steps", lastSavedSteps)
            apply()
        }
        _steps.value = lastSavedSteps
    }
    fun updateSteps(totalStepsSinceReboot: Long){
        appScope.launch {
            val nowDay = LocalDate.now().toEpochDay()
            if (nowDay > lastDateSaved) {
                resetIfNewDay(totalStepsSinceReboot)
            }

            val delta = totalStepsSinceReboot - lastSavedSteps
            _steps.value = totalStepsSinceReboot
            if (delta >= 10) {
                lastSavedSteps = totalStepsSinceReboot
                lastDateSaved = nowDay
                savePersistentSteps(nowDay)
            }
        }
    }
    private fun savePersistentSteps(day: Long) {
        stepsPrefs.edit().apply {
            putLong("saved_steps", lastSavedSteps)
            putLong("last_steps_date", day)
            apply()
        }
    }
    init {
        appScope.launch {
            _lastInsertedSteps.value = dao.getStepsByDay(LocalDate.now().minusDays(1).toEpochDay())?.steps ?: 0
        }
    }
}