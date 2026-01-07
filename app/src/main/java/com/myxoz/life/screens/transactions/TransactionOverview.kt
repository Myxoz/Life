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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.edit
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalScreens
import com.myxoz.life.LocalStorage
import com.myxoz.life.R
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.dbwrapper.BankingEntity
import com.myxoz.life.dbwrapper.BankingSidecarEntity
import com.myxoz.life.dbwrapper.formatCents
import com.myxoz.life.screens.options.ME_ID
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.formatMinutes
import com.myxoz.life.utils.formatTimeStamp
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.viewmodels.LargeDataCache
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun TransactionOverview(transactionId: String, bankViewModel: LargeDataCache){
    val db = LocalStorage.current
    val nav = LocalNavController.current
    val transactionAtHand = remember { runBlocking /* Review if not clean */ { db.banking.getTransactionById(transactionId)?: JSONObject(db.prefs.getString("transactionAtHand", null)?:"{}").let {
        BankingEntity(
            "",
            false,
            false,
            it.getInt("amount"),
            "EUR",
            "",
            it.getLong("timestamp"),
            it.getString("to"),
            "",
            "",
            "",
            0,
            it.getLong("timestamp"),
            it.getLong("timestamp")
        )
    } } }
    val transactionSidecar = remember { runBlocking { db.bankingSidecar.getSidecar(transactionId) } }
    Scaffold(
        Modifier.fillMaxSize(),
        containerColor = Colors.BACKGROUND
    ) { innerPadding ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top)
        ) {
            Spacer(Modifier.height(innerPadding.calculateTopPadding()))
            Box(
                Modifier
                    .padding(vertical = 50.dp)
                    .background(Colors.SECONDARY, CircleShape)
                    .clip(CircleShape)
                    .rippleClick {
                        nav.navigate("bank/me")
                    }
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            ) {
                Text(
                    transactionAtHand.amountCents.formatCents(false) + " " + transactionAtHand.currency,
                    Modifier
                        .fillMaxWidth(),
                    color = if (transactionAtHand.amountCents > 0) Colors.Transactions.PLUS else Colors.Transactions.MINUS,
                    fontFamily = FontFamily.Display.family,
                    fontSize = FontSize.XXLARGE.size,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                if (transactionAtHand.amountCents < 0) "An" else "Von",
                Modifier
                    .fillMaxWidth(),
                style = TypoStyle(FontColor.SECONDARY, FontSize.LARGE),
                textAlign = TextAlign.Center
            )
            BankCard(
                transactionAtHand.fromName + if (transactionSidecar != null) " (${transactionSidecar.name})" else "",
                transactionAtHand.fromIban,
                bankViewModel
            )
            val calendar = remember { Calendar.getInstance() }
            val screens = LocalScreens.current
            Text(
                run {
                    val transactionType = when {
                        transactionAtHand.transfer -> "Überweisung"
                        transactionAtHand.card -> "Kartenzahlung"
                        transactionSidecar != null || transactionAtHand.id.isBlank() -> "Google Pay"
                        else -> ""
                    }

                    val timestamp =
                        transactionAtHand.purposeDate?.formatTimeStamp(calendar)
                            ?: transactionSidecar?.date?.formatTimeStamp(calendar)
                            ?: ""

                    val separator =
                        if (transactionType.isNotEmpty() && timestamp.isNotEmpty()) " · " else ""

                    "$transactionType$separator$timestamp"
                },
                Modifier
                    .rippleClick(transactionAtHand.purposeDate != null || transactionSidecar?.date != null){
                        (transactionAtHand.purposeDate ?: transactionSidecar?.date)?.let {
                            screens.openCalendarAt(
                                Instant
                                    .ofEpochMilli(it)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            )
                        }
                    }
                    .align(Alignment.End)
                ,
                textAlign = TextAlign.End,
                style = TypoStyle(FontColor.SECONDARY, FontSize.SMALL)
            )
            if (transactionAtHand.bookingTime.isNotBlank()) {
                Spacer(Modifier.height(40.dp))
                TransactionEntry("Buchungsdatum", transactionAtHand.bookingTime)
            }
            if (transactionAtHand.purpose.isNotBlank()) {
                Spacer(Modifier)
                TransactionEntry("Verwendungszweck", transactionAtHand.purpose)
            }
            Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }
}
@Composable
fun TransactionEntry(title: String, value: String) {
    Column {
        Text(title, style = TypoStyle(FontColor.PRIMARY, FontSize.LARGE))
        Text(value, style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM), modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
fun MyCard(largeDataCache: LargeDataCache){
    val db = LocalStorage.current
    var showBalance by remember {
        mutableStateOf(
            db.prefs.getBoolean("show_balance", false)
        )
    }
    val self = remember {
        runBlocking {
            db.people.getPersonById(ME_ID)
        }
    }
    val balance = if(showBalance) runBlocking {
        db.banking.getLastTransactionDay()
    } else listOf()
    Scaffold(
        Modifier.fillMaxSize(),
        containerColor = Colors.BACKGROUND
    ) { innerPadding ->
        Box(
            Modifier
                .padding(horizontal = 30.dp)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Spacer(Modifier.height(innerPadding.calculateTopPadding()))
            Box(
                Modifier
                    .padding(vertical = 50.dp)
                    .background(Colors.SECONDARY, CircleShape)
                    .clip(CircleShape)
                    .rippleClick {
                        showBalance = !showBalance
                        db.prefs.edit {
                            putBoolean("show_balance", showBalance)
                        }
                    }
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
                    .align(Alignment.TopCenter)
            ) {
                Text(
                    if (showBalance) finalDailyBalance(balance).toInt()
                        .formatCents(true) else "· · · , · · €",
                    Modifier
                        .fillMaxWidth(),
                    color = Colors.PRIMARYFONT,
                    fontFamily = FontFamily.Display.family,
                    fontSize = FontSize.XXLARGE.size,
                    textAlign = TextAlign.Center
                )
            }
            BankCard(self?.fullname?:"Ich", self?.iban?:"***", largeDataCache)
            Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }
}
@Composable
fun TransactionList(epochDay: Long) {
    val db = LocalStorage.current
    val nav = LocalNavController.current
    val bankingEntries = remember {
        runBlocking {
            val date = LocalDate.ofEpochDay(epochDay)
            val zone = ZoneId.systemDefault()
            db.banking.getFullDayTransactions(
                date.atStartOfDay(zone).toEpochSecond() * 1000L,
                date.plusDays(1).atStartOfDay(zone).toEpochSecond() * 1000L
            ).map { it to db.bankingSidecar.getSidecar(it.id) }
        }
    }
    Scaffold(
        Modifier.fillMaxSize(),
        containerColor = Colors.BACKGROUND
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(innerPadding.calculateTopPadding()))
            bankingEntries.forEach {
                BankingEntryComposable(it) {
                    nav.navigate("bank/transaction/${it.first.id}")
                }
            }
            Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }
}

@Composable
fun BankingEntryComposable(entry: Pair<BankingEntity, BankingSidecarEntity?>, onClick:  ()->Unit){
    val calendar = remember { Calendar.getInstance() }
    Column(
        Modifier
            .background(Colors.SECONDARY, RoundedCornerShape(25))
            .clip(RoundedCornerShape(25))
            .rippleClick{onClick()}
            .padding(horizontal = 20.dp, vertical = 15.dp)
            .fillMaxWidth(.95f)
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
                val card = entry.first.card
                val transfer = entry.first.transfer
                val height = FontSize.MEDIUMM.size.toDp()
                Icon(
                    painterResource(
                        when {
                            card -> R.drawable.pay_with_card
                            transfer -> R.drawable.bank_transfer
                            else -> R.drawable.gpay
                        }
                    ),
                    "Payment Type",
                    Modifier
                        .height(height)
                        .width(if(!transfer && !card) height*2.5f else height),
                    Colors.SECONDARYFONT
                )
                if(card || transfer) Text(
                    if(card) "Kartenzahlung" else "Überweisung",
                    style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUMM)
                )
            }
            Text(
                (entry.second?.date ?: entry.first.purposeDate ?: entry.first.valueDate).formatMinutes(calendar),
                style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUMM)
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
                    entry.second?.name ?: entry.first.fromName,
                    style = TypoStyle(FontColor.PRIMARY, FontSize.LARGE)
                )
                Text(
                    if(entry.second == null) entry.first.fromIban.chunked(4).joinToString(" ") else entry.first.fromName,
                    style = TypoStyle(FontColor.SECONDARY, FontSize.SMALLM)
                )
            }
            Text(
                entry.first.amountCents.formatCents(),
                color = if(entry.first.amountCents > 0) Colors.Transactions.PLUS else Colors.Transactions.MINUS,
                fontFamily = FontFamily.Display.family,
                fontSize = FontSize.XLARGE.size,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BankCard(from: String, fromIBAN: String, largeDataCache: LargeDataCache?) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var displaysIban by remember { mutableStateOf(true) }
    val db = LocalStorage.current
    val decodedPerson by produceState<PersonSyncable?>(null) {
        value = db.people.getPersonByIban(fromIBAN)?.let { PersonSyncable.from(db, it) }
    }

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
            .fillMaxWidth()
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
            .background(
                Brush.radialGradient(
                    listOf(
                        Colors.Myxoz.ACC,
                        Colors.Myxoz.MAIN
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
                from + if(decodedPerson != null) " (${decodedPerson?.name})" else "",
                Modifier
                    .clickable(null, null) {
                        screens.openPersonDetails(decodedPerson?.id ?: return@clickable)
                    }
                ,
                style = TypoStyle(FontColor.PRIMARY, FontSize.MLARGE),
                textAlign = TextAlign.Start
            )
            if(displaysIban){
                val clipboard = LocalClipboardManager.current
                if(fromIBAN.isNotBlank())
                    Text(
                        fromIBAN.uppercase().chunked(4).joinToString(" "),
                        style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM),
                        modifier = Modifier.combinedClickable(null, null, onLongClick = { clipboard.setText(AnnotatedString(fromIBAN))}){
                            if(largeDataCache!=null && fromIBAN.startsWith("DE")) displaysIban=!displaysIban
                        }
                    )
            } else {
                val allBanks = largeDataCache?.bankMap ?: mapOf()
                Text(
                    allBanks[fromIBAN.substring(4, 4+8)]?.format()?:"Keine Informationen möglich",
                    style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM),
                    modifier = Modifier.clickable(null, null){if(largeDataCache!=null) displaysIban=!displaysIban}
                )
            }
        }
    }
}

fun finalDailyBalance(transactions: List<BankingEntity>): Long {
    if (transactions.isEmpty()) error("No transactions")

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