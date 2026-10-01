package com.myxoz.life.aggregator

import com.myxoz.life.storage.interfaces.DatabaseInterface

class AppAggregators internal constructor(
    repos: DatabaseInterface
) {
    val peopleAggregator =
        _root_ide_package_.com.myxoz.life.storage.interfaces.aggregator.PeopleAggregator(repos)
    val calendarAggregator = CalendarAggregator(repos)
    val streakAggregator = StreakAggregator(repos)
}