package com.myxoz.life.screens.person

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.myxoz.life.Theme
import com.myxoz.life.screens.feed.dayoverview.edgeToEdgeGradient
import com.myxoz.life.screens.transactions.BankingEntryComposable
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.ProfileInfoModel
import com.myxoz.life.viewmodels.TransactionViewModel
import kotlinx.coroutines.flow.combine

@Composable
fun FullScreenDebt(viewModel: ProfileInfoModel, transactionViewModel: TransactionViewModel, person: Long) {
    val debtFlow by viewModel.debtFlow(person).collectAsState()
    val transactions = debtFlow?.data ?: emptyList()
    val resolvedTransactions by produceState(emptyList(), debtFlow?.version) {
        if (transactions.isEmpty()) {
            value = emptyList()
            return@produceState
        }
        val flows = transactions.map { syncable ->
            transactionViewModel.getTransaction(syncable.key)
        }

        combine(flows) { array ->
            array
                .filterNotNull()
                .sortedByDescending { it.timestamp }
        }.collect {
            value = it
        }
    }
    Box(
        Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .background(Theme.background)
                .edgeToEdgeGradient(Theme.background, windowPadding)
                .fillMaxSize()
            ,
            state = viewModel.debtListState,
            reverseLayout = true,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(Modifier.height(windowPadding.calculateBottomPadding()))
            }
            itemsIndexed(resolvedTransactions) { index, entry ->
                Box(
                    Modifier
                        .setMaxTabletWidth()
                ) {
                    BankingEntryComposable(
                        entry = entry,
                        isFirst = index == resolvedTransactions.lastIndex,
                        isLast = index == 0
                    )
                }
            }
            item {
                Spacer(Modifier.height(windowPadding.calculateTopPadding()))
            }
        }
    }
}