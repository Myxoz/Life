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
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalScreens
import com.myxoz.life.R
import com.myxoz.life.screens.LocalScreensProvider
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyleOld
import com.myxoz.life.utils.rippleClick

class InstantEvent(
    val icon: Int,
    val subText: String,
    val timestamp: Long,
    val openDetails: (LocalScreensProvider)->Unit
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
            val screens = LocalScreens.current
            Column(
                Modifier
                    .size(instantEventDisplaySize)
                    .background(OldColors.SECONDARY.copy(.8f), CircleShape)
                    .clip(CircleShape)
                    .rippleClick {
                        openDetails(
                            screens
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
                    val screens = LocalScreens.current
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .width(instantEventDisplaySize)
                            .background(OldColors.SECONDARY.copy(.8f), CircleShape)
                            .clip(CircleShape)
                            .rippleClick {
                                screens.openInstantEventRange(instantEvents)
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
    companion object {
        const val INSTANTEVENTSIZE = 1.5f
    }
}