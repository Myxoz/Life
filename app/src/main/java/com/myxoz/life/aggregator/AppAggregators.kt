package com.myxoz.life.aggregator

import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.aggregator.PeopleEventAggregator

class AppAggregators internal constructor(
    repos: DatabaseInterface
) {
    val peopleAggregator = repos.aggregators.
    val calendarAggregator = CalendarAggregator(repos)
    val streakAggregator = StreakAggregator(repos)
}