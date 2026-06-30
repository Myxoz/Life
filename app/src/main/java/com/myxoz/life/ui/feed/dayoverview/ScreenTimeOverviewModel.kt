package com.myxoz.life.ui.feed.dayoverview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.utils.subscribeToColdFlow
import com.myxoz.life.ui.NavPath
import com.myxoz.life.utils.getDate

class ScreenTimeOverviewModel(
    dbInterface: DatabaseInterface,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    val date = savedStateHandle.getDate(NavPath.DayOverview.SCREENTIME)

    val daySummary = dbInterface.daySummaryInterface
        .getDaySummary(date)
        .subscribeToColdFlow(viewModelScope, null)

    val liveScreenTime = dbInterface.aggregators.daySummaryAggregator.getScreenTimeOnDayLive(date)
    val screentimeFlow = dbInterface.aggregators.daySummaryAggregator.getScreenTimeForDay(date)
}