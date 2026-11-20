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
import com.myxoz.life.lifecore.instantevents.InstantEvent
import com.myxoz.life.lifecore.instantevents.InstantEventOpener
import com.myxoz.life.lifeui.providers.LocalNavController
import com.myxoz.life.lifeui.providers.Theme
import com.myxoz.life.lifeui.reusedcompose.rippleClick
import com.myxoz.life.lifeui.theme.FontSize
import com.myxoz.life.lifeui.theme.OldColors
import com.myxoz.life.lifeui.theme.TypoStyle
import com.myxoz.life.presentation.R
import java.time.Instant

class InstantEventGroup(
    val instantEvents: List<InstantEvent>,
    val start: Instant = instantEvents.minOf { it.timestamp },
    val end: Instant = instantEvents.maxOf { it.timestamp }
) {
}
