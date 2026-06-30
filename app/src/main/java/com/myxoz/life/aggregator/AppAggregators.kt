package com.myxoz.life.aggregator

import com.myxoz.life.storage.interfaces.DatabaseInterface

class AppAggregators(
    repos: DatabaseInterface
) {
    val peopleAggregator = PeopleAggregator(repos)
    val daySummaryAggregator = DaySummaryAggregator(repos)
    val calendarAggregator = CalendarAggregator(repos)
    val streakAggregator = StreakAggregator(repos)
}