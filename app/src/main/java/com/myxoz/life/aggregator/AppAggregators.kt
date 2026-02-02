package com.myxoz.life.aggregator

import com.myxoz.life.repositories.AppRepositories

class AppAggregators(
    repos: AppRepositories
) {
    val peopleAggregator = PeopleAggregator(repos)
    val daySummaryAggregator = DaySummaryAggregator(repos)
    val calendarAggregator = CalendarAggregator(repos)
}