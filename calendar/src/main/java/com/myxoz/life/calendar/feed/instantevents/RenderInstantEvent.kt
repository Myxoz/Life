package com.myxoz.life.calendar.feed.instantevents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import com.myxoz.life.core.minus
import com.myxoz.life.lifecore.NavPath
import com.myxoz.life.lifecore.instantevents.InstantEventOpener
import com.myxoz.life.lifeui.providers.LocalNavController
import com.myxoz.life.lifeui.providers.Theme
import com.myxoz.life.lifeui.reusedcompose.rippleClick
import com.myxoz.life.lifeui.theme.FontSize
import com.myxoz.life.lifeui.theme.OldColors
import com.myxoz.life.lifeui.theme.TypoStyle
import java.time.Instant

@Composable
fun RenderInstantEvents(
    instantEvents: List<InstantEventGroup>,
    instantEventOpener: InstantEventOpener,
    oneHourDp: Dp,
    startOfDay: Instant,
) {
    val nav = LocalNavController.current
    val instantEventDisplaySize = INSTANTEVENTSIZE * oneHourDp
    for(instantEventGroup in instantEvents) {
        if(instantEventGroup.instantEvents.size == 1) {
            val ie = instantEventGroup.instantEvents[0]
            Box(
                Modifier
                    .offset(y = ((ie.timestamp - startOfDay).inWholeMilliseconds / 3_600_000f) * oneHourDp - instantEventDisplaySize / 2)
                    .height(instantEventDisplaySize)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                ie.RenderContent(oneHourDp) {
                    ie.openInstantEvent(instantEventOpener)
                }
            }
        } else {
            Box(
                Modifier
                    .offset(y = ((instantEventGroup.start - startOfDay).inWholeMilliseconds / 3_600_000f) * oneHourDp - instantEventDisplaySize / 2)
                    .height(instantEventDisplaySize + ((instantEventGroup.end - instantEventGroup.start).inWholeMilliseconds / 3_600_000f) * oneHourDp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(instantEventDisplaySize)
                        .background(OldColors.SECONDARY.copy(.8f), CircleShape)
                        .clip(CircleShape)
                        .rippleClick {
                            nav.navigate(NavPath.InstantEventSelection(instantEventGroup.instantEvents))
                        },
                )
                instantEventGroup.instantEvents.forEach { ev ->
                    Box(
                        Modifier
                            .offset(y = ((ev.timestamp - instantEventGroup.start).inWholeMilliseconds / 3_600_000f) * oneHourDp)
                            .height(instantEventDisplaySize),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Row(
                            Modifier
                                .size(instantEventDisplaySize),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painterResource(ev.icon),
                                "Card",
                                Modifier
                                    .padding(start = .15f * instantEventDisplaySize)
                                    .height(oneHourDp / 4f),
                                tint = OldColors.PRIMARYFONT
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                ev.subText,
                                style = TypoStyle(Theme.primary, FontSize.XSMALL),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            Modifier
                                .padding(end = instantEventDisplaySize * .9f)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
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