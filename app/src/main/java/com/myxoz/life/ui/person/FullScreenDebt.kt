package com.myxoz.life.ui.person

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.myxoz.life.Theme
import com.myxoz.life.ui.feed.dayoverview.edgeToEdgeGradient
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.transactions.BankingEntryComposable
import com.myxoz.life.utils.windowPadding

@Composable
fun FullScreenDebt(viewModel: PersonalDebtViewModel) {
    val resolvedTransactionsRaw by viewModel.debtTransactions.collectAsState()
    val resolvedTransactions = resolvedTransactionsRaw?: return
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