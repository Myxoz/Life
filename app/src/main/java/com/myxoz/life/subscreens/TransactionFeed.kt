package com.myxoz.life.subscreens

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
import androidx.compose.material3.Scaffold
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
import com.myxoz.life.LocalNavController
import com.myxoz.life.dbwrapper.BankingEntity
import com.myxoz.life.dbwrapper.BankingSidecarEntity
import com.myxoz.life.dbwrapper.formatCents
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.viewmodels.CalendarViewModel
import com.myxoz.life.viewmodels.TransactionFeedViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate

@Composable
fun TransactionFeed(
    state: TransactionFeedViewModel,
    calendarViewModel: CalendarViewModel,
) {
    val nav = LocalNavController.current

    val transactionsByDate by state.transactionsByDate.collectAsState()
    val visibleDates by state.visibleDates.collectAsState()
    val listState = state.lazyListState

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisible ->
                state.onLastVisibleIndexChanged(lastVisible)
            }
    }

    Scaffold(
        Modifier.fillMaxSize(),
        containerColor = Colors.BACKGROUND
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .edgeToEdgeGradient(Colors.BACKGROUND, innerPadding)
                .fillMaxSize(),
            state = listState,
        ) {
            item {
                Spacer(Modifier.height(8.dp + innerPadding.calculateTopPadding()))
            }

            items(
                items = visibleDates,
                key = { it.toEpochDay() },
                contentType = { "date_group" }
            ) { date ->
                val transactions = transactionsByDate[date] ?: emptyList()
                if (transactions.isNotEmpty()) {
                    DateTransactionGroup(
                        date = date,
                        transactions = transactions,
                        calendarViewModel,
                        onTransactionClick = { tx ->
                            nav.navigate("bank/transaction/${tx.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DateTransactionGroup(
    date: LocalDate,
    transactions: List<Pair<BankingEntity, BankingSidecarEntity?>>,
    calendarViewModel: CalendarViewModel,
    onTransactionClick: (BankingEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(top = 10.dp, bottom = 30.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth(.95f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val nav = LocalNavController.current
            Text(
                text = "${date.dayOfMonth.toString().padStart(2, '0')}.${
                    date.monthValue.toString().padStart(2, '0')
                }.${date.year}",
                style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM),
                modifier = Modifier
                    .clip(CircleShape)
                    .rippleClick{
                        calendarViewModel.setDay(date)
                        nav.popBackStack("home", false)
                    }
            )
            val sum = transactions.sumOf { it.first.amountCents }
            Text(
                text = sum.formatCents(true),
                fontSize = FontSize.MEDIUM.size,
                color = if (sum < 0) Colors.Transactions.MINUS else Colors.Transactions.PLUS
            )
        }

        transactions.forEach { tx ->
            key(tx.first.id) {  // Ensure stable composition
                BankingEntryComposable(tx, onClick = { onTransactionClick(tx.first) })
            }
        }
    }
}

