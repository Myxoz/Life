package com.myxoz.life.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalScreens
import com.myxoz.life.Theme
import com.myxoz.life.dbwrapper.banking.formatCents
import com.myxoz.life.repositories.BankingRepo
import com.myxoz.life.screens.feed.dayoverview.edgeToEdgeGradient
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.TransactionViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate

@Composable
fun TransactionFeed(
    transactionViewModel: TransactionViewModel
) {
    val visibleDates by transactionViewModel.visibleDates.collectAsState()
    val listState = transactionViewModel.lazyListState

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisible ->
                transactionViewModel.onLastVisibleIndexChanged(lastVisible)
            }
    }

    val innerPadding = windowPadding
    LazyColumn(
        modifier = Modifier
            .background(Theme.background)
            .edgeToEdgeGradient(Theme.background, innerPadding)
            .fillMaxSize(),
        state = listState,
        reverseLayout = true,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
        }
        items(
            items = visibleDates,
            key = { it.toEpochDay() },
            contentType = { "date_group" }
        ) { date ->
            val transactions by transactionViewModel.getOnDay(date).collectAsState()
            if(transactions.isEmpty()) Spacer(Modifier.height(1.dp))
            val screens = LocalScreens.current
            if (transactions.isNotEmpty()) {
                DateTransactionGroup(
                    date = date,
                    transactions = transactions,
                    onTransactionClick = { tx ->
                        screens.openTransaction(tx)
                    }
                )
            }
        }
    }
}

@Composable
private fun DateTransactionGroup(
    date: LocalDate,
    transactions: List<BankingRepo.BankingDisplayEntity>,
    onTransactionClick: (BankingRepo.BankingDisplayEntity) -> Unit
) {
    val screens = LocalScreens.current
    Column(
        modifier = Modifier
            .padding(top = 20.dp, bottom = 10.dp)
            .setMaxTabletWidth()
        ,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        transactions.forEachIndexed { i, tx ->
            key(tx.entity.id) {  // Ensure stable composition
                BankingEntryComposable(tx, i == 0, i == transactions.size-1) {
                    onTransactionClick(tx)
                }
            }
        }
        Row(
            Modifier
                .border(2.dp, Theme.outlineVariant, CircleShape)
                // .background(Theme.surfaceContainer, RoundedCornerShape(20.dp))
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp)
            ,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${date.dayOfMonth.toString().padStart(2, '0')}.${
                    date.monthValue.toString().padStart(2, '0')
                }.${date.year}",
                style = TypoStyle(Theme.secondary, FontSize.MEDIUM),
                modifier = Modifier
                    .clip(CircleShape)
                    .rippleClick{
                        screens.openCalendarAt(date)
                    }
            )
            val sum = transactions.sumOf { it.entity.amountCents }
            Text(
                text = sum.formatCents(true),
                fontSize = FontSize.MEDIUM.size,
                color = if (sum < 0) OldColors.Transactions.MINUS else OldColors.Transactions.PLUS
            )
        }
    }
}

