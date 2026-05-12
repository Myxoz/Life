package com.myxoz.life.repositories

import android.content.Context
import android.content.SharedPreferences
import com.myxoz.life.aggregator.AppAggregators
import com.myxoz.life.api.API
import com.myxoz.life.api.UpdateHooks
import com.myxoz.life.viewmodels.Settings
import kotlinx.coroutines.CoroutineScope

class AppRepositories(
    val hooks: UpdateHooks,
    val calendarRepo: CalendarRepo,
    val daySummaryRepo: DaySummaryRepo,
    val stepRepo: StepRepo,
    val peopleRepo: PeopleRepo,
    val commitsRepo: CommitsRepo,
    val contactRepo: ContactRepo,
    val bankingRepo: BankingRepo,
    val locationRepo: LocationRepo,
    val aiPredictionRepo: AIPredictionRepo,
    val todoRepo: TodoRepo,
    val extensionRepo: ExtensionRepo,
    val api: API,
    val readSyncableDaos: API.ReadSyncableDaos,
    val prefs: SharedPreferences,
    val permissionChecker: Settings.Permission.PermissionChecker,
    val context: Context,
    val appScope: CoroutineScope
) {
    val aggregators = AppAggregators(this)
}