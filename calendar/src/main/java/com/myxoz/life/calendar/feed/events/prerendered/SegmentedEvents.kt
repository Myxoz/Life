package com.myxoz.life.calendar.feed.events.prerendered

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myxoz.life.RenderContent
import com.myxoz.life.calendar.search.SearchField
import com.myxoz.life.presentation.events.FormattableEvent.Companion.formattable
import java.time.Instant


@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RenderPrerenderedEvent(
    prerenderedEvent: PrerenderedEvent,
    searched: SearchField,
    oneHour: Dp,
    instantEventSize: Dp,
    startOfDay: Instant,
    endOfDay: Instant,
    width: Dp,
    isClickEnabled: Boolean,
    editEvent: ()->Unit,
    openEventDetails: ()->Unit
) {
    val isSearched = searched.query.isSearched(prerenderedEvent)
    Box(
        Modifier
            .fillMaxSize()
            .alpha(if (isSearched) 1f else 0.3f)
    ) {
        for (segment in prerenderedEvent.segments) {
            Box(
                Modifier
                    .padding(
                        top = segment.getTopPadding(oneHour, startOfDay),
                        start = if (!segment.isLeft) (width - instantEventSize).coerceAtLeast(0.dp) else 0.dp
                    )
                    .height(segment.getHeightDp(oneHour, startOfDay, endOfDay))
                    .width(if (segment.isFullWidth) width else if (segment.isLeft) width - instantEventSize else instantEventSize)
                    .clip(RoundedCornerShape(10.dp))
                    .combinedClickable(
                        remember { MutableInteractionSource() },
                        ripple(),
                        isClickEnabled,
                        onClick = openEventDetails,
                        onLongClick = editEvent
                    )
                    .background(prerenderedEvent.event.raw.formattable().bg, RoundedCornerShape(10.dp))
            ) {
                if(segment.hasContent) {
                    RenderContent(prerenderedEvent., oneHour, startOfDay, endOfDay, !segment.isLeft, segment.getBlockHeight(startOfDay, endOfDay))
                }
            }
        }
    }
}