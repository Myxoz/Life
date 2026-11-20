package com.myxoz.life.calendar.feed.events.prerendered

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.myxoz.life.core.min
import com.myxoz.life.core.minus
import com.myxoz.life.storage.api.syncables.SyncedEvent
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

internal class PrerenderedEvent(
    val event: SyncedEvent,
    val segments: List<Segment>,
    val nextToPeople: Set<Long>
) {
    data class Segment(
        val isFullWidth: Boolean,
        val isLeft: Boolean,
        val hasContent: Boolean,
        val segmentStart: Instant,
        val segmentEnd: Instant
    ) {
        internal fun getDurationInRange(startOfDay: Instant, endOfDay: Instant): Duration =
            min((segmentEnd - segmentStart), min(endOfDay - segmentStart, endOfDay - startOfDay))
        internal fun getBlockHeight(startOfDay: Instant, endOfDay: Instant): Int =
            (getDurationInRange(startOfDay, endOfDay).inWholeMinutes/15).toInt()
        internal fun getTopPadding(oneHour: Dp, startOfDay: Instant) =
            ((segmentStart - startOfDay).coerceAtLeast(Duration.ZERO) / 1.hours) * oneHour + 1.dp
        internal fun getHeightDp(oneHour: Dp, startOfDay: Instant, endOfDay: Instant) =
            oneHour * (getBlockHeight(startOfDay, endOfDay)/4f) - 1.dp
    }
}