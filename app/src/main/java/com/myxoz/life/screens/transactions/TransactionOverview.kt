package com.myxoz.life.screens.transactions

import android.icu.util.Calendar
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalScreens
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.dbwrapper.banking.BankingEntity
import com.myxoz.life.dbwrapper.banking.formatCents
import com.myxoz.life.repositories.BankingRepo
import com.myxoz.life.screens.feed.dayoverview.edgeToEdgeGradient
import com.myxoz.life.ui.SCREENMAXWIDTH
import com.myxoz.life.ui.rememberAsymmetricalVerticalCornerRadius
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.ui.theme.TypoStyleOld
import com.myxoz.life.utils.collectAsMutableState
import com.myxoz.life.utils.formatMinutes
import com.myxoz.life.utils.formatTimeStamp
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.LargeDataCache
import com.myxoz.life.viewmodels.TransactionViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun TransactionOverview(largeDataCache: LargeDataCache, transactionViewModel: TransactionViewModel){
    val nav = LocalNavController.current
    val transactionAtHand by transactionViewModel.inspectedTransaction.collectAsState()
    val transaction = transactionAtHand ?: return
    val innerPadding = windowPadding
    Box(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
        ,
        Alignment.TopCenter
    ) {
        Column(
            Modifier
                .widthIn(max = SCREENMAXWIDTH)
                .fillMaxWidth(.9f)
            ,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top)
        ) {
            Spacer(Modifier.height(innerPadding.calculateTopPadding()))
            Box(
                Modifier
                    .padding(vertical = 50.dp)
                    .background(Theme.surfaceContainer, CircleShape)
                    .clip(CircleShape)
                    .rippleClick {
                        nav.navigate("bank/me")
                    }
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            ) {
                Text(
                    transaction.entity.amountCents.formatCents(false) + " " + transaction.entity.currency,
                    Modifier
                        .fillMaxWidth(),
                    color = if (transaction.entity.amountCents > 0) OldColors.Transactions.PLUS else OldColors.Transactions.MINUS,
                    fontFamily = FontFamily.Display.family,
                    fontSize = FontSize.XXLARGE.size,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                if (transaction.entity.amountCents < 0) "An" else "Von",
                Modifier
                    .fillMaxWidth(),
                style = TypoStyle(Theme.secondary, FontSize.LARGE),
                textAlign = TextAlign.Center
            )
            Column (
                Modifier
                    .align(Alignment.CenterHorizontally)
                ,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BankCard(
                    transaction.entity.fromName + if (transaction.sidecar != null) " (${transaction.sidecar.name})" else "",
                    transaction.entity.fromIban,
                    largeDataCache,
                    transactionViewModel
                )
                val calendar = remember { Calendar.getInstance() }
                val screens = LocalScreens.current
                Text(
                    run {
                        val transactionType = when {
                            transaction.entity.isWirelessPayment() -> "Bargeldlos"
                            transaction.entity.card -> "Kartenzahlung"
                            else -> "Überweisung"
                        }

                        val timestamp =
                            transaction.entity.purposeDate?.formatTimeStamp(calendar)
                                ?: transaction.sidecar?.date?.formatTimeStamp(calendar)
                                ?: ""

                        val separator = if (transactionType.isNotEmpty() && timestamp.isNotEmpty()) " · " else ""

                        "$transactionType$separator$timestamp"
                    },
                    Modifier
                        .rippleClick(true){
                            screens.openCalendarAt(
                                Instant
                                    .ofEpochMilli(transaction.resolveEffectiveDate())
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            )
                        }
                        .align(Alignment.End)
                    ,
                    textAlign = TextAlign.End,
                    style = TypoStyle(Theme.secondary, FontSize.SMALL)
                )
            }
            if (transaction.entity.bookingTime.isNotBlank()) {
                Spacer(Modifier.height(40.dp))
                TransactionEntry("Buchungsdatum", transaction.entity.bookingTime)
            }
            if (transaction.entity.purpose.isNotBlank()) {
                Spacer(Modifier)
                TransactionEntry("Verwendungszweck", transaction.entity.purpose)
            }
            Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }
}
@Composable
fun TransactionEntry(title: String, value: String) {
    Column {
        Text(title, style = TypoStyle(Theme.primary, FontSize.LARGE))
        Text(value, style = TypoStyle(Theme.secondary, FontSize.MEDIUM), modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
fun MyCard(largeDataCache: LargeDataCache, transactionViewModel: TransactionViewModel){
    var showBalance by transactionViewModel.showBalance.collectAsMutableState()
    val self by transactionViewModel.getSelf.collectAsState(null)
    val lastTransactions by transactionViewModel.lastTransaction.collectAsState()
    val balance = finalDailyBalance(lastTransactions?.map { it.entity })

    val innerPadding = windowPadding
    Box(
        Modifier
            .widthIn(max = SCREENMAXWIDTH)
            .fillMaxWidth(.9f)
        ,
        Alignment.Center
    ) {
        Box(
            Modifier
                .padding(vertical = 50.dp + innerPadding.calculateTopPadding() + 10.dp)
                .background(Theme.surfaceContainer, CircleShape)
                .clip(CircleShape)
                .rippleClick {
                    showBalance = !showBalance
                }
                .fillMaxWidth()
                .padding(vertical = 20.dp)
                .align(Alignment.TopCenter)
        ) {
            Text(
                if (showBalance) balance.toInt()
                    .formatCents(true) else "· · · , · · €",
                Modifier
                    .fillMaxWidth(),
                color = Theme.primary,
                fontFamily = FontFamily.Display.family,
                fontSize = FontSize.XXLARGE.size,
                textAlign = TextAlign.Center
            )
        }
        BankCard(self?.fullName?:"Ich", self?.iban?:"***", largeDataCache, transactionViewModel)
        Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
    }
}
@Composable
fun TransactionList(date: LocalDate, transactitonFeedModel: TransactionViewModel) {
    val bankingEntries by transactitonFeedModel.getOnDay(date).collectAsState(listOf())
    val innerPadding = windowPadding
    Column(
        Modifier
            .background(Theme.background)
            .edgeToEdgeGradient(Theme.background, innerPadding)
            .fillMaxSize()
        ,
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Bottom),
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

@Composable
fun BankingEntryComposable(entry: BankingRepo.BankingDisplayEntity, isFirst: Boolean, isLast: Boolean){
    val calendar = remember { Calendar.getInstance() }
    val screens = LocalScreens.current
    Column(
        Modifier
            .padding(vertical = 2.5.dp)
            .clip(rememberAsymmetricalVerticalCornerRadius(isFirst, isLast))
            .background(Theme.surfaceContainerHigh)
            .rippleClick{screens.openTransaction(entry)}
            .padding(horizontal = 20.dp, vertical = 15.dp)
        ,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
            ,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val card = entry.entity.card
                val height = FontSize.MEDIUMM.size.toDp()
                Icon(
                    painterResource(
                        when {
                            entry.entity.isWirelessPayment() -> R.drawable.wireless_pay
                            card -> R.drawable.pay_with_card
                            else -> R.drawable.bank_transfer
                        }
                    ),
                    "Payment Type",
                    Modifier
                        .height(height)
                        .width(height),
                    Theme.secondary
                )
                Text(
                    if(entry.entity.isWirelessPayment()) "Bargeldlos" else if(card) "Kartenzahlung" else "Überweisung",
                    style = TypoStyle(Theme.secondary, FontSize.MEDIUMM)
                )
            }
            Text(
                entry.resolveEffectiveDate().formatMinutes(calendar),
                style = TypoStyle(Theme.secondary, FontSize.MEDIUMM)
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
            ,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    entry.sidecar?.name ?: entry.entity.fromName,
                    style = TypoStyle(Theme.primary, FontSize.LARGE),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if(entry.sidecar == null) entry.entity.fromIban.chunked(4).joinToString(" ") else entry.entity.fromName,
                    style = TypoStyle(Theme.secondary, FontSize.SMALLM),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                entry.entity.amountCents.formatCents(),
                color = if(entry.entity.amountCents > 0) OldColors.Transactions.PLUS else OldColors.Transactions.MINUS,
                fontFamily = FontFamily.Display.family,
                fontSize = FontSize.XLARGE.size,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BankCard(from: String, fromIBAN: String, largeDataCache: LargeDataCache?, transactionViewModel: TransactionViewModel) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var displaysIban by remember { mutableStateOf(true) }
    val decodedPeople by transactionViewModel.getPeopleWithIbanLike(fromIBAN).collectAsState(listOf())

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offsetX"
    )

    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offsetY"
    )

    val rotationY = (animatedOffsetX / 10f)
    val rotationX = (-animatedOffsetY / 10f)

    Box(
        Modifier
            .graphicsLayer {
                this.rotationY = rotationY
                this.rotationX = rotationX
                this.cameraDistance = 12f * density
                this.translationX = animatedOffsetX * 0.1f
                this.translationY = animatedOffsetY * 0.1f
            }
            .zIndex(1f)
            .shadow(10.dp, RoundedCornerShape(10))
            .height(220.dp)
            .aspectRatio(1.5857725f)
            // Based on https://de.wikipedia.org/wiki/ISO/IEC_7810#ID-1
            .background(
                Brush.radialGradient(
                    listOf(
                        OldColors.Myxoz.ACC,
                        OldColors.Myxoz.MAIN
                    ),
                    center = Offset(Float.POSITIVE_INFINITY, 0f),
                    radius = 600f
                ),
                RoundedCornerShape(10)
            )
            .padding(vertical = 15.dp, horizontal = 20.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onDragCancel = {
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x*.6f
                        offsetY += dragAmount.y*.6f
                    }
                )
            }
    ) {
        Icon(
            painterResource(R.drawable.myxoz_card_text),
            "Myxoz",
            Modifier
                .height(30.dp)
                .align(Alignment.TopEnd),
            Color.Black
        )
        Column(
            Modifier.align(Alignment.BottomStart)
        ) {
            val screens = LocalScreens.current
            Text(
                from + if(decodedPeople.isNotEmpty()) " (${decodedPeople.joinToString { it.name }})" else "",
                Modifier
                    .clickable(null, null, decodedPeople.size == 1) {
                        screens.openPersonDetails(decodedPeople.firstOrNull()?.id ?: return@clickable)
                    }
                ,
                style = TypoStyleOld(FontColor.PRIMARY, FontSize.MLARGE),
                textAlign = TextAlign.Start
            )
            if(displaysIban){
                val clipboard = LocalClipboardManager.current
                if(fromIBAN.isNotBlank())
                    Text(
                        fromIBAN.uppercase().chunked(4).joinToString(" "),
                        style = TypoStyleOld(FontColor.SECONDARY, FontSize.MEDIUM),
                        modifier = Modifier.combinedClickable(null, null, onLongClick = { clipboard.setText(AnnotatedString(fromIBAN))}){
                            if(largeDataCache!=null && fromIBAN.startsWith("DE")) displaysIban=!displaysIban
                        }
                    )
            } else {
                val allBanks = largeDataCache?.bankMap ?: mapOf()
                Text(
                    allBanks[fromIBAN.substring(4, 4+8)]?.format()?:"Keine Informationen möglich",
                    style = TypoStyleOld(FontColor.SECONDARY, FontSize.MEDIUM),
                    modifier = Modifier.clickable(null, null){if(largeDataCache!=null) displaysIban=!displaysIban}
                )
            }
        }
    }
}

fun finalDailyBalance(transactions: List<BankingEntity>?): Long {
    if (transactions?.isEmpty() != false) return 0L

    // Try to find the "last" transaction recursively
    fun findLast(current: BankingEntity, remaining: List<BankingEntity>): BankingEntity {
        // Look for a transaction that could come AFTER the current one
        val next = remaining.find { it.saldoAfterCents - it.amountCents == current.saldoAfterCents }
        return if (next == null) current else findLast(next, remaining - next)
    }

    // Try every transaction as a possible starting point
    for (candidate in transactions) {
        val previous =
            transactions.find { candidate.saldoAfterCents - candidate.amountCents == it.saldoAfterCents }
        if (previous == null) {
            // This is the earliest transaction (no one leads into it)
            return findLast(candidate, transactions - candidate).saldoAfterCents
        }
    }

    error("Could not determine transaction order")
}