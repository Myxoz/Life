package com.myxoz.life.feature.socialgraph

import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import com.myxoz.life.lifeui.time.updatingToday
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days


class SocialGraphPanel {
    var chartScale by mutableStateOf(SocialGraphScale.MONTH)
    var isPlaying by mutableStateOf(false)
    var playingSpeedExponent by mutableFloatStateOf(1f)
    var isVisible by mutableStateOf(false)
    var snapToToday by mutableStateOf(true)
    var currentTimelineDate: LocalDate by mutableStateOf(LocalDate.now())
        private set
    @Composable
    fun endDate(): State<LocalDate>{
        val today by updatingToday()
        return remember {
            derivedStateOf {
                if(snapToToday) today else currentTimelineDate
            }
        }
    }

    fun endDate(today: LocalDate): LocalDate{
        return if(snapToToday) today else currentTimelineDate
    }

    fun setEndDate(newEndDate: LocalDate) {
        currentTimelineDate = newEndDate
        snapToToday = newEndDate == LocalDate.now()
    }

    var addMyself by mutableStateOf(false)
    enum class SocialGraphScale(val displayName: String, val duration: Duration) {
        ALL("All", Duration.INFINITE),
        YEAR("365d", 365.days),
        QUATER("90d",90.days),
        MONTH("30d",30.days),
        WEEK("1w",7.days);
        fun days(): Long? = if(this.duration != Duration.INFINITE) duration.inWholeDays else null
        fun millis(): Long? = if(this.duration != Duration.INFINITE) duration.inWholeMilliseconds else null
        companion object {
            private val ascendingEntries = entries.sortedBy(SocialGraphScale::duration)
            fun minIncluding(duration: Duration): SocialGraphScale =
                ascendingEntries
                    .firstOrNull { it.duration >= duration }
                    ?: ALL
        }
    }
    companion object {
        const val TIME = 300
        val animation = tween<Float>(TIME)
        val animationColor = tween<Color>(TIME)
        val animationIntOffset = tween<IntOffset>(TIME)
    }
}
