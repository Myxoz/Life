package com.myxoz.life.ui.feed.dayoverview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.storage.interfaces.BankingRepo
import com.myxoz.life.storage.interfaces.utils.subscribeToColdFlow
import com.myxoz.life.ui.NavPath
import com.myxoz.life.utils.getDate

class DayOverviewTransactionModel(
    val dbInterface: BankingRepo,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    val date = savedStateHandle.getDate(NavPath.DayOverview.TRANSACTIONS)
    val transactions = dbInterface.getSortedTransactionsAt(date).subscribeToColdFlow(viewModelScope, listOf())
}