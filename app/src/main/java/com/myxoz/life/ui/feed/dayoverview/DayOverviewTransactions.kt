package com.myxoz.life.ui.feed.dayoverview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.myxoz.life.Theme
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.transactions.BankingEntryComposable
import com.myxoz.life.utils.windowPadding

@Composable
fun TransactionList(dayOverviewTransactionModel: DayOverviewTransactionModel) {
    val bankingEntries by dayOverviewTransactionModel.transactions.collectAsState()
    val innerPadding = windowPadding
    Column(
        Modifier
            .background(Theme.background)
            .edgeToEdgeGradient(Theme.background, innerPadding)
            .fillMaxSize()
        ,
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(innerPadding.calculateTopPadding()))
        bankingEntries.forEachIndexed { i, it ->
            Box(
                Modifier
                    .setMaxTabletWidth()
            ) {
                BankingEntryComposable(it, i == 0, i == bankingEntries.size-1)
            }
        }
        Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
    }
}