package com.myxoz.life.screens.feed.instantevents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.core.content.edit
import androidx.navigation.NavController
import com.myxoz.life.LocalNavController
import com.myxoz.life.R
import com.myxoz.life.dbwrapper.BankingEntity
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.dbwrapper.formatCents
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyleOld
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.viewmodels.CalendarViewModel
import org.json.JSONObject

class InstantEvent(
    val icon: Int,
    val subText: String,
    val timestamp: Long,
    val openDetails: (NavController)->Unit
) {
    @Composable
    fun Render(startOfDay: Long, endOfDay: Long, oneHourDp: Dp){
        val instantEventDisplaySize = INSTANTEVENTSIZE * oneHourDp
        Box(
            Modifier.Companion
                .offset(y = ((timestamp - startOfDay) / 3_600_000f) * oneHourDp - instantEventDisplaySize / 2)
                .height(instantEventDisplaySize)
                .fillMaxWidth(),
            contentAlignment = Alignment.Companion.CenterEnd
        ) {
            val navController = LocalNavController.current
            Column(
                Modifier.Companion
                    .size(instantEventDisplaySize)
                    .background(OldColors.SECONDARY.copy(.8f), CircleShape)
                    .clip(CircleShape)
                    .rippleClick {
                        openDetails(
                            navController
                        )
                    },
                horizontalAlignment = Alignment.Companion.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painterResource(icon),
                    "Card",
                    Modifier.Companion.height(oneHourDp / 2f),
                    tint = OldColors.PRIMARYFONT
                )
                Spacer(Modifier.Companion.height((if (icon == R.drawable.pay_with_card) 2.dp else 4.dp)))
                Text(subText, style = TypoStyleOld(FontColor.PRIMARY, FontSize.XSMALL))
            }
            Box(
                Modifier.Companion
                    .padding(end = instantEventDisplaySize * .9f)
                    .background(
                        Brush.Companion.horizontalGradient(
                            listOf(
                                Color.Companion.Transparent,
                                OldColors.PRIMARYFONT
                            )
                        ), CircleShape
                    )
                    .width(oneHourDp)
                    .padding(vertical = 1.dp),
            )
        }
    }
    companion object {
        suspend fun getEntriesBetween(db: StorageManager, startOfDay: Long, endOfDay: Long, viewModel: CalendarViewModel): List<InstantEventGroup> {
            val entries = BankingEntity.getAllBankingEntriesFor(db, startOfDay, endOfDay, viewModel.futureBankEntries).mapNotNull {
                InstantEvent(
                    if(it.isWirelessPayment()) R.drawable.wireless_pay else if(it.card) R.drawable.pay_with_card else R.drawable.bank_transfer,
                    it.amountCents.formatCents(),
                    it.purposeDate?:return@mapNotNull null,
                ) { nav ->
                    if(it.id.isBlank()) db.prefs.edit {
                        putString(
                            "transactionAtHand", JSONObject()
                                .put("amount", it.amountCents)
                                .put("timestamp", it.purposeDate)
                                .put("to", it.fromName)
                                .toString()
                        )
                    }
                    nav.navigate("bank/transaction/${it.id}")
                }
            }
            val commits = db.commits.getCommitsForDay(startOfDay, endOfDay).mapNotNull {
                InstantEvent(
                    R.drawable.commit,
                    it.repoName,
                    it.commitDate?:return@mapNotNull null,
                ) { nav ->
                    nav.navigate("commits/commit/${it.commitSha}")
                }
            }
            val mix = (entries + commits).sortedBy { it.timestamp }
            val groups = mutableListOf<MutableList<InstantEvent>>(mutableListOf())
            var lastTs = mix.getOrNull(0)?.timestamp ?: return listOf()
            for (event in mix) {
                if(event.timestamp - lastTs > INSTANTEVENTSIZE * (3600 * 1000L)) {
                    groups.add(mutableListOf())
                }
                groups.last().add(event)
                lastTs = event.timestamp
            }
            return groups.map { InstantEventGroup(it) }
        }
        const val INSTANTEVENTSIZE = 1.5f
        class InstantEventGroup(
            val instantEvents: List<InstantEvent>,
            val start: Long = instantEvents.minOf { it.timestamp },
            val end: Long = instantEvents.maxOf { it.timestamp }
        ){
            @Composable
            fun Render(startOfDay: Long, endOfDay: Long, oneHourDp: Dp){
                val instantEventDisplaySize = INSTANTEVENTSIZE * oneHourDp
                if(instantEvents.size == 1) {
                    instantEvents[0].Render(startOfDay, endOfDay, oneHourDp)
                } else {
                    val nav = LocalNavController.current
                    Box(
                        Modifier.Companion
                            .offset(y = ((start - startOfDay) / 3_600_000f) * oneHourDp - instantEventDisplaySize / 2)
                            .height(instantEventDisplaySize + ((end - start) / 3_600_000f) * oneHourDp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Companion.TopEnd
                    ) {
                        Box(
                            Modifier.Companion
                                .fillMaxHeight()
                                .width(instantEventDisplaySize)
                                .background(OldColors.SECONDARY.copy(.8f), CircleShape)
                                .clip(CircleShape)
                                .rippleClick {
                                    nav.navigate("instant_events_between/${start}/${end}")
                                },
                        )
                        instantEvents.forEach { ev ->
                            Box(
                                Modifier.Companion
                                    .offset(y = ((ev.timestamp - start) / 3_600_000f) * oneHourDp)
                                    .height(instantEventDisplaySize),
                                contentAlignment = Alignment.Companion.CenterEnd
                            ) {
                                Row(
                                    Modifier.Companion
                                        .size(instantEventDisplaySize),
                                    verticalAlignment = Alignment.Companion.CenterVertically,
                                ) {
                                    Icon(
                                        painterResource(ev.icon),
                                        "Card",
                                        Modifier.Companion
                                            .padding(start = .15f * instantEventDisplaySize)
                                            .height(oneHourDp / 4f),
                                        tint = OldColors.PRIMARYFONT
                                    )
                                    Spacer(Modifier.Companion.width((if (ev.icon == R.drawable.pay_with_card) 2.dp else 4.dp)))
                                    Text(
                                        ev.subText,
                                        style = TypoStyleOld(FontColor.PRIMARY, FontSize.XSMALL),
                                        maxLines = 1,
                                        overflow = TextOverflow.Companion.Ellipsis
                                    )
                                }
                                Box(
                                    Modifier.Companion
                                        .padding(end = instantEventDisplaySize * .9f)
                                        .background(
                                            Brush.Companion.horizontalGradient(
                                                listOf(
                                                    Color.Companion.Transparent,
                                                    OldColors.PRIMARYFONT
                                                )
                                            ), CircleShape
                                        )
                                        .width(oneHourDp)
                                        .padding(vertical = 1.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}