package com.myxoz.life.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.storage.interfaces.BankingInterface.MixedBankingEntity
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.subscribeToColdFlow
import com.myxoz.life.ui.NavPath

class TransactionOverviewViewModel(
    handle: SavedStateHandle,
    val dbInterface: DatabaseInterface
): ViewModel() {
    private val key = MixedBankingEntity.unparcel(
        handle.get<String>(NavPath.Transaction.DETAILS.parameterName) ?: error("Cannot unparcel route to string in banking")
    )
    val inspectedTransaction = dbInterface.bankingInterface.getTransaction(key)
        .subscribeToColdFlow(viewModelScope, null)
}