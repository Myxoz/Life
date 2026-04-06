package com.myxoz.life.screens.transactions

import android.icu.util.Calendar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalScreens
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.api.syncables.ManualTransactionSyncable
import com.myxoz.life.api.syncables.TransactionSplitSyncable
import com.myxoz.life.dbwrapper.banking.formatCents
import com.myxoz.life.repositories.BankingRepo
import com.myxoz.life.repositories.BankingRepo.BankingDisplayEntity.Companion.finalDailyBalance
import com.myxoz.life.screens.NavPath
import com.myxoz.life.screens.feed.dayoverview.edgeToEdgeGradient
import com.myxoz.life.ui.ActionBar
import com.myxoz.life.ui.EditToTickAndDiscard
import com.myxoz.life.ui.SCREENMAXWIDTH
import com.myxoz.life.ui.holdToCopy
import com.myxoz.life.ui.rememberAsymmetricalVerticalCornerRadius
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.ui.theme.TypoStyleOld
import com.myxoz.life.utils.collectAsMutableState
import com.myxoz.life.utils.formatDayTime
import com.myxoz.life.utils.formatTimeStamp
import com.myxoz.life.utils.matchInstrinsicHeight
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.LargeDataCache
import com.myxoz.life.viewmodels.TransactionViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

@Composable
fun TransactionOverview(largeDataCache: LargeDataCache, transactionViewModel: TransactionViewModel) {
    val nav = LocalNavController.current
    val transactionAtHand by transactionViewModel.inspectedTransaction.collectAsState()
    val transaction = transactionAtHand ?: return
    val innerPadding = windowPadding
    val predictedTransactionName by BankingRepo.BankingDisplayEntity.getResolvedName(transaction, transactionViewModel)
    Column(
        Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .weight(1f)
                .fillMaxWidth()
            ,
            horizontalAlignment = Alignment.CenterHorizontally
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
                            nav.navigate(NavPath.Transaction.ME)
                        }
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    Text(
                        transaction.amount.formatCents(false) + " " + transaction.currency,
                        Modifier
                            .fillMaxWidth(),
                        color = if (transaction.amount > 0) OldColors.Transactions.PLUS else OldColors.Transactions.MINUS,
                        fontFamily = FontFamily.Display.family,
                        fontSize = FontSize.XXLARGE.size,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    if (transaction.amount < 0) "An" else "Von",
                    Modifier
                        .fillMaxWidth(),
                    style = TypoStyle(Theme.secondary, FontSize.LARGE),
                    textAlign = TextAlign.Center
                )
                val isAiPredicted = predictedTransactionName != null
                Column(
                    Modifier
                        .align(Alignment.CenterHorizontally),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    BankCard(
                        transaction.displayName(predictedTransactionName),
                        transaction.iban,
                        isAiPredicted,
                        largeDataCache,
                        transactionViewModel
                    )
                    val calendar = remember { Calendar.getInstance() }
                    val screens = LocalScreens.current
                    Text(
                        run {
                            val timestamp =
                                transaction.displayTimestamp?.formatTimeStamp(calendar) ?: ""
                            val separator = if (timestamp.isNotEmpty()) " · " else ""
                            "${transaction.categorization}$separator$timestamp"
                        },
                        Modifier
                            .rippleClick(true) {
                                screens.openCalendarAt(
                                    Instant
                                        .ofEpochMilli(transaction.timestamp)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                )
                            }
                            .align(Alignment.End),
                        textAlign = TextAlign.End,
                        style = TypoStyle(Theme.secondary, FontSize.SMALL)
                    )
                }
                if (!transaction.bookingTime.isNullOrBlank()) {
                    Spacer(Modifier.height(40.dp))
                    TransactionEntry("Buchungsdatum", transaction.bookingTime)
                }
                if (!transaction.purpose.isNullOrBlank()) {
                    Spacer(Modifier)
                    TransactionEntry("Verwendungszweck", transaction.purpose)
                }
                Spacer(Modifier)
                PaymentSplit(transactionViewModel, transaction)
                // Uncomment when you need the weights
//            val props by produceState<DoubleArray?>(null) {
//                    val entity = transaction.entity
//                    val ts = transaction.resolveEffectiveDate()
//                    value = transactionViewModel.predictPaymentProbs(
//                        ReadBankingDao.BankingTrainingRow(
//                            entity.amountCents,
//                            ts,
//                            "(What we want to find out)",
//                            transactionViewModel.getLastTransactionIdBefore(ts)
//                        )
//                    )
//                }
//            Text(props?.withIndex()?.sortedByDescending { it.value }?.joinToString("\n") { "${transactionViewModel.paymentPredictor?.buckets[it.index]}: ${it.value}" }?:"", color = Theme.primary)
                Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
            }
        }
        if(transaction.getStoredManualTransactionSyncable() != null) {
            Box(
                Modifier
                    .padding(vertical = 10.dp)
                    .setMaxTabletWidth()
            ) {
                val screens = LocalScreens.current
                ActionBar(
                    null,
                    null,
                    OldColors.Myxoz.MAIN,
                    {
                        screens.editTransaction(transaction)
                    }
                )  {
                    Text(
                        "Bearbeiten",
                        style = TypoStyle(Theme.primary, FontSize.LARGE)
                            .copy(fontWeight = FontWeight.W900)
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painterResource(R.drawable.arrow_right),
                        "Continue",
                        tint = Theme.primary,
                        modifier = Modifier.height(20.dp)
                    )
                }
            }
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
    val self by transactionViewModel.getSelf.collectAsState()
    val lastTransactions by transactionViewModel.lastTransactions.collectAsState()
    val balance = remember(lastTransactions) { finalDailyBalance(lastTransactions) }

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
        BankCard(self?.fullName?:"Ich", self?.iban?:"***", false, largeDataCache, transactionViewModel)
        Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
    }
}
@Composable
fun TransactionList(date: LocalDate, transactitonFeedModel: TransactionViewModel) {
    val bankingEntries by transactitonFeedModel.getOnDay(date).collectAsState()
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

@Composable
fun BankingEntryComposable(entry: BankingRepo.BankingDisplayEntity, isFirst: Boolean, isLast: Boolean) {
    val calendar = remember { Calendar.getInstance() }
    val screens = LocalScreens.current
    Column(
        Modifier
            .padding(vertical = 2.5.dp)
            .clip(rememberAsymmetricalVerticalCornerRadius(isFirst, isLast))
            .alpha(if(entry.isCashlessPayment) .5f else 1f)
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
                val height = FontSize.MEDIUMM.size.toDp()
                Icon(
                    painterResource(entry.icon),
                    "Payment Type",
                    Modifier
                        .height(height)
                        .width(height),
                    Theme.secondary
                )
                Text(
                    entry.categorization,
                    style = TypoStyle(Theme.secondary, FontSize.MEDIUMM)
                )
            }
            Text(
                entry.timestamp.formatDayTime(calendar),
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
                    entry.title,
                    style = TypoStyle(Theme.primary, FontSize.LARGE),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    entry.subTitle,
                    style = TypoStyle(Theme.secondary, FontSize.SMALLM),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                entry.amount.formatCents(),
                color = if(entry.amount > 0) OldColors.Transactions.PLUS else OldColors.Transactions.MINUS,
                fontFamily = FontFamily.Display.family,
                fontSize = FontSize.XLARGE.size,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BankCard(
    from: String,
    fromIBAN: String,
    isAIGenerated: Boolean,
    largeDataCache: LargeDataCache?,
    transactionViewModel: TransactionViewModel
) {
    val animatedOffsetX = remember { Animatable(0f) }
    val animatedOffsetY = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    var displaysIban by remember { mutableStateOf(true) }
    val decodedPeople by transactionViewModel.getPeopleWithIbanLike(fromIBAN).collectAsState()

    val rotationY = (animatedOffsetX.value / 10f)
    val rotationX = (-animatedOffsetY.value / 10f)

    val coroutineScope = rememberCoroutineScope()
    Box(
        Modifier
            .graphicsLayer {
                this.rotationY = rotationY
                this.rotationX = rotationX
                this.cameraDistance = 12f * density
                this.translationX = animatedOffsetX.value * 0.1f
                this.translationY = animatedOffsetY.value * 0.1f
                this.scaleX = scale.value
                this.scaleY = scale.value
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
                val end =  {
                    fun animateTo(animatable: Animatable<Float, AnimationVector1D>, to: Float) {
                        coroutineScope.launch {
                            animatable.animateTo(
                                to,
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    }
                    animateTo(animatedOffsetY, 0f)
                    animateTo(animatedOffsetX, 0f)
                    animateTo(scale, 1f)
                }
                detectDragGestures(
                    onDragEnd = {
                        end()
                    },
                    onDragCancel = {
                        end()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            animatedOffsetX.snapTo(animatedOffsetX.value + dragAmount.x*.6f)
                            animatedOffsetY.snapTo(animatedOffsetY.value + dragAmount.y*.6f)
                        }
                        coroutineScope.launch {
                            scale.animateTo(0.9f, spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessVeryLow
                            ))
                        }
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
            Row(
                Modifier
                    .height(IntrinsicSize.Min)
                ,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if(isAIGenerated) {
                    Icon(
                        painterResource(R.drawable.sparkles),
                        "AI predicted name",
                        Modifier.fillMaxHeight(),
                        Theme.primary
                    )
                }
                Text(
                    from + if(decodedPeople.isNotEmpty()) " (${decodedPeople.joinToString { it.name }})" else "",
                    Modifier
                        .clickable(null, null, decodedPeople.size == 1) {
                            screens.openPersonDetails(decodedPeople.firstOrNull()?.id ?: return@clickable)
                        }
                    ,
                    style = TypoStyle(Theme.primary, FontSize.MLARGE),
                    textAlign = TextAlign.Start
                )
            }
            if(displaysIban){
                if(fromIBAN.isNotBlank())
                    Text(
                        fromIBAN.uppercase().chunked(4).joinToString(" "),
                        style = TypoStyle(Theme.secondary, FontSize.MEDIUM),
                        modifier = Modifier
                            .holdToCopy(fromIBAN) {
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

@Composable
private fun PaymentSplit(transactionViewModel: TransactionViewModel, transaction: BankingRepo.BankingDisplayEntity) {
    Column(
        Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var isEditing by transactionViewModel.isEditingSplit.collectAsMutableState()
        var editingSplits by transactionViewModel.editingSplit.collectAsMutableState()
        val splits by transactionViewModel.getSplit(transaction).collectAsState()
        val displaySplit = if(isEditing) editingSplits else splits?.value
        val coroutineScope = rememberCoroutineScope()
        val screens = LocalScreens.current
        fun startEditingIfNotStarted(init: List<TransactionSplitSyncable.Companion.Part> = listOf()){
            if(isEditing) return
            isEditing = true
            editingSplits = splits?.value ?: TransactionSplitSyncable(
                -1L,
                transaction.key.first,
                transaction.key.second,
                init
            )
        }
        val mePart = remember(transaction, displaySplit) {
           transaction.amount - (displaySplit?.parts?.sumOf { it.amount } ?: 0)
        }
        Row(
            Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Aufteilung (${if(transaction.amount <= 0) "Schuldet mir" else "Schulde ich"})", style = TypoStyle(Theme.primary, FontSize.LARGE))
            EditToTickAndDiscard(
                isEditing,
                FontSize.LARGE.size.toDp() + 5.dp,
                { R.drawable.edit },
                10.dp,
                {
                    coroutineScope.launch {
                        if(editingSplits.parts.isEmpty()) {
                            if (editingSplits.isSynced())
                                transactionViewModel.deleteSplit(editingSplits)
                        } else {
                            transactionViewModel.saveAndSyncSplit(editingSplits)
                        }
                        isEditing = false
                    }
                },
                {
                    isEditing = false
                }
            ) {
                startEditingIfNotStarted()
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            displaySplit?.parts?.forEachIndexed { index, part ->
                key(part.person) {
                    Row(
                        Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val person by transactionViewModel.getPerson(part.person).collectAsState(null)
                        Row(
                            Modifier
                                .height(IntrinsicSize.Min)
                            ,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AnimatedVisibility(isEditing) {
                                Icon(painterResource(R.drawable.close), "Remove", Modifier.padding(end = 10.dp).matchInstrinsicHeight(.5f).rippleClick{
                                    editingSplits = editingSplits.copy(parts = editingSplits.parts.toMutableList().apply {
                                        removeAt(index)
                                    })
                                }, Theme.secondary)
                            }
                            Text(
                                person?.name ?: "Lädt...",
                                Modifier
                                    .clip(CircleShape)
                                    .rippleClick{
                                        screens.openPersonDetails(part.person)
                                    }
                                ,
                                style = TypoStyle(Theme.secondary, FontSize.MEDIUM)
                            )
                        }
                        var enteredText by remember { mutableStateOf(abs(part.amount).toString()) }
                        if(isEditing) {
                            BasicTextField(
                                enteredText,
                                {
                                    enteredText = it.trimStart('0')
                                    editingSplits = editingSplits.copy(
                                        parts = editingSplits.parts.toMutableList().apply {
                                            set(index, part.copy(amount = (if(transaction.amount < 0) -1 else 1) * (it.toIntOrNull() ?: 0)))
                                        }
                                    )
                                },
                                textStyle = TypoStyle(Theme.secondary, FontSize.MEDIUM).copy(textAlign = TextAlign.End),
                                singleLine = true,
                                visualTransformation = remember {
                                    ManualTransactionSyncable.MoneyBasedVisualTransformation()
                                }
                            )
                        } else {
                            Text(
                                ManualTransactionSyncable.MoneyBasedVisualTransformation.toTransformed(abs(part.amount).toString()),
                                style = TypoStyle(Theme.secondary, FontSize.MEDIUM).copy(textAlign = TextAlign.End)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        AnimatedVisibility(isEditing) {
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(Theme.primaryContainer)
                    .rippleClick {
                        screens.getPerson{
                            val split = transactionViewModel.editingSplit
                            split.value = split.value.copy(
                                parts = split.value.parts.toMutableList().apply {
                                    if(it != null) add(TransactionSplitSyncable.Companion.Part(it, if(split.value.parts.isEmpty()) transaction.amount else 0))
                                }
                            )
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("+ Auswählen", style = TypoStyle(Theme.onPrimaryContainer, FontSize.MEDIUM))
            }
        }
        Spacer(Modifier.height(10.dp))
        if(!displaySplit?.parts.isNullOrEmpty()) {
            HorizontalDivider(color = Theme.outlineVariant)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    Modifier
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(painterResource(R.drawable.arrow_right), "Results", Modifier.matchInstrinsicHeight(), Theme.secondary)
                    Text("Ich", style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
                }
                Text(mePart.formatCents(), style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
            }
        }
    }
}