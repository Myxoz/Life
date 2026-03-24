package com.myxoz.life.repositories

import android.content.SharedPreferences
import androidx.core.content.edit
import com.myxoz.life.dbwrapper.days.ProposedStepsDao
import com.myxoz.life.dbwrapper.days.ProposedStepsEntity
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
    val steps = _steps.combine(stepsAtMidnight){ steps, atMidnight ->  steps - atMidnight }
        .subscribeToColdFlow(appScope, _steps.value - stepsAtMidnight.value)
    init {
    }
    /** Inserts steps as the last date where steps were saved */
    private suspend fun resetStepsAsOldDay(totalSteps: Long? = null, midnightSteps: Long? = null) {
        val today = LocalDate.now().toEpochDay()
        val stepsToInsert = (lastSavedSteps - stepsAtMidnight.value).coerceAtLeast(0).toInt()
        dao.insertSteps(ProposedStepsEntity(lastDateSaved, stepsToInsert))
        _lastInsertedSteps.value = stepsToInsert

        lastDateSaved = today
        stepsAtMidnight.value = midnightSteps ?: totalSteps ?: lastSavedSteps
        lastSavedSteps = totalSteps ?: lastSavedSteps
        stepsPrefs.edit {
            putLong("last_steps_date", lastDateSaved)
            putLong("steps_at_midnight", stepsAtMidnight.value)
            putLong("saved_steps", lastSavedSteps)
            apply()
        }
        _steps.value = lastSavedSteps
    }
    fun updateSteps(totalSensorSteps: Long){
        appScope.launch {
            val today = LocalDate.now().toEpochDay()
            // Reboot (or random sensor reset FUCK OEMs)
            // Fuck Samsung again because they sometimes seam to remove a small amount of steps and don't fully reset the sensor,
            // I will log this and TODO look at the logs and understand how the sensor works
            // Logs: Max Dif: 1
            /*
From 10823 to 10822 at 1774192727221 (22.03.2026 16:18:47)
From 49160 to 0 at 1774050986159 (21.03.2026 00:56:26)
From 30681 to 30680 at 1773570464845 (15.03.2026 11:27:44)
            */
            if(totalSensorSteps < lastSavedSteps){
                stepsPrefs.edit {
                    val stepLog = (stepsPrefs.getStringSet("step_reset_log",setOf()) ?: setOf())
                        .toMutableSet().apply { add(
                            "$lastSavedSteps;$totalSensorSteps;${System.currentTimeMillis()}"
                        )}
                    putStringSet("step_reset_log", stepLog)
                }
                // To new day (edge case e.x. reboot at midnight)
                if(today > lastDateSaved) {
                    resetStepsAsOldDay(totalSensorSteps, 0)
                } else { // Step drop durring the day
                    val atMid = -lastSavedSteps + stepsAtMidnight.value
                    stepsPrefs.edit{
                        putLong("saved_steps", totalSensorSteps)
                        putLong("steps_at_midnight", atMid)
                    }
                    stepsAtMidnight.value = atMid
                    lastSavedSteps = totalSensorSteps
                    _steps.value = totalSensorSteps
                }
            } else if (today > lastDateSaved) {
                resetStepsAsOldDay(totalSensorSteps)
            }


            val delta = totalSensorSteps - lastSavedSteps
            _steps.value = totalSensorSteps
            if (delta >= 10) {
                lastSavedSteps = totalSensorSteps
                lastDateSaved = today
                savePersistentSteps(today)
            }
        }
    }
    private fun savePersistentSteps(day: Long) {
        stepsPrefs.edit {
            putLong("saved_steps", lastSavedSteps)
            putLong("last_steps_date", day)
            apply()
        }
    }
    fun debugGetRawSteps() = _steps
    init {
        appScope.launch {
            _lastInsertedSteps.value = dao.getStepsByDay(LocalDate.now().minusDays(1).toEpochDay())?.steps ?: 0
        }
    }
}