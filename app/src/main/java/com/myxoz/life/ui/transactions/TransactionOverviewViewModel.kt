package com.myxoz.life.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.storage.interfaces.BankingRepo.BankingDisplayEntity
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.utils.subscribeToColdFlow
import com.myxoz.life.ui.NavPath

class TransactionOverviewViewModel(
    handle: SavedStateHandle,
    val dbInterface: DatabaseInterface
): ViewModel() {
    private val key = BankingDisplayEntity.unparcel(
        handle.get<String>(NavPath.Transaction.DETAILS.parameterName) ?: error("Cannot unparcel route to string in banking")
    )
    val inspectedTransaction = dbInterface.bankingRepo.getTransaction(key)
        .subscribeToColdFlow(viewModelScope, null)
}