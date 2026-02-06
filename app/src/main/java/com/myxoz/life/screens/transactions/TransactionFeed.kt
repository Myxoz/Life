package com.myxoz.life.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalScreens
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.dbwrapper.banking.formatCents
import com.myxoz.life.screens.feed.dayoverview.edgeToEdgeGradient
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.TransactionViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun TransactionFeed(
    transactionViewModel: TransactionViewModel
) {
    val visibleTransactions by transactionViewModel.orderedAllTransactionFlow.collectAsState()
    val listState = transactionViewModel.lazyListState

    var isAtBottom by remember { mutableStateOf(false) }
    var scrollOffset by remember { mutableFloatStateOf(0.0f) }
    val scrollProgress = if(isAtBottom) 0f else scrollOffset
    val innerPadding = windowPadding
    val nestedScrollConnection = remember { object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            scrollOffset = (scrollOffset - available.y / 700).coerceIn(0f, 1f)
            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset { return Offset.Zero }
    } }

    LaunchedEffect(listState) {
        transactionViewModel.onLastVisibleIndexChanged(0)
        launch {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                .distinctUntilChanged()
                .collect { lastVisible ->
                    if(lastVisible == null) return@collect
                    transactionViewModel.onLastVisibleIndexChanged(lastVisible)
                }
        }
        launch {
            snapshotFlow { listState.firstVisibleItemIndex }
                .distinctUntilChanged()
                .collect { lastVisible ->
                    isAtBottom = if(lastVisible <= 0){
                        scrollOffset = 0f
                        true
                    } else {
                        false
                    }
                }
        }
    }
    Box(
        Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .background(Theme.background)
                .edgeToEdgeGradient(Theme.background, innerPadding)
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
            ,
            state = listState,
            reverseLayout = true,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
            }
            visibleTransactions.forEach { (key, value) ->
                item {
                    Box(
                        Modifier
                            .setMaxTabletWidth()
                        ,
                        contentAlignment = Alignment.Center
                    ) {
                        BankingDaySummary(value.sumOf { it.entity.amountCents }, key)
                    }
                }
                value.forEachIndexed { i, transaction ->
                    item {
                        Box(
                            Modifier
                                .setMaxTabletWidth()
                            ,
                            contentAlignment = Alignment.Center
                        ) {
                            BankingEntryComposable(
                                transaction,
                                i == value.size - 1,
                                i == 0,
                            )
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(innerPadding.calculateTopPadding()))
            }
        }
        fun poly(x: Float) = -x*x*2+x+1
        // Yes this is the polynomial -2x^2 + x + 1
        Box(
            Modifier
                .padding(20.dp)
                .size(60.dp)
                .align(Alignment.BottomEnd)
                .alpha(poly(1-scrollProgress))
                .shadow(10.dp)
            ,
            contentAlignment = Alignment.Center
        ) {
            val coroutineScope = rememberCoroutineScope()
            Box(
                Modifier
                    .size(50.dp*(poly(1-scrollProgress)))
                    .background(Theme.primaryContainer, RoundedCornerShape(20))
                    .rippleClick{
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.scroll_to_bottom),
                    "Scroll to bottom",
                    Modifier.size(40.dp),
                    Theme.primary
                )
            }
        }
    }
}

@Composable
private fun BankingDaySummary(sum: Int, date: LocalDate){
    Row(
        Modifier
            .padding(top = 5.dp, bottom = 20.dp)
            .border(2.dp, Theme.outlineVariant, CircleShape)
            // .background(Theme.surfaceContainer, RoundedCornerShape(20.dp))
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp)
        ,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val screens = LocalScreens.current
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
        Text(
            text = sum.formatCents(true),
            fontSize = FontSize.MEDIUM.size,
            color = if (sum < 0) OldColors.Transactions.MINUS else OldColors.Transactions.PLUS
        )
    }
}