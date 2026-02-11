package com.myxoz.life.repositories

import android.content.Context
import android.content.SharedPreferences
import com.myxoz.life.aggregator.AppAggregators
import com.myxoz.life.api.API

class AppRepositories(
    val calendarRepo: CalendarRepo,
    val daySummaryRepo: DaySummaryRepo,
    val stepRepo: StepRepo,
    val peopleRepo: PeopleRepo,
    val commitsRepo: CommitsRepo,
    val contactRepo: ContactRepo,
    val bankingRepo: BankingRepo,
    val locationRepo: LocationRepo,
    val aiPredictionRepo: AIPredictionRepo,
    val api: API,
    val readSyncableDaos: API.ReadSyncableDaos,
    val prefs: SharedPreferences,
    val context: Context
) {
    val aggregators = AppAggregators(this)
}