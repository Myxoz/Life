package com.myxoz.life.ui.person

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.utils.subscribeToColdFlow
import com.myxoz.life.ui.NavPath
import com.myxoz.life.utils.def
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapMerge

class PersonalDebtViewModel(
    val dbInterface: DatabaseInterface,
    val handle: SavedStateHandle
): ViewModel() {
    val personId = handle.get<Long>(NavPath.Menu.Contacts.DEBT_DISPLAY.parameterName) ?: error("Failed to parse personaldebt path $handle")
    private val debt = dbInterface.bankingRepo.getDebtFor(personId)

    @OptIn(ExperimentalCoroutinesApi::class)
    val debtTransactions = debt.flatMapMerge { list ->
        combine(
            list.def(emptyList()).map { syncable ->
            dbInterface.bankingRepo.getTransaction(syncable.key)
        }){ array ->
            array
                .filterNotNull()
                .sortedByDescending { it.timestamp }
        }
    }.subscribeToColdFlow(viewModelScope, null)
    var debtListState = LazyListState()
}