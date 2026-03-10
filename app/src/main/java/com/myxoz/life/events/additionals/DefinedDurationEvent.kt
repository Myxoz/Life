package com.myxoz.life.events.additionals

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import kotlin.math.min

open class DefinedDurationEvent(val start: Long, val end: Long) {
    fun length() = (end - start).coerceAtLeast(15*1000L*60)
    fun getBlockHeight(startOfDay: Long, endOfDay: Long): Int = (min(length(), min(endOfDay - start, end - startOfDay))/(900*1000)).toInt()
    fun getBlockLength() = (length() / (900*1000)).toInt()
    fun getTopPadding(oneHour: Dp, startOfDay: Long) = ((this.start - startOfDay).coerceAtLeast(0L) / (3600 * 1000L).toFloat()) * oneHour + 1.dp
    fun getHeightDp(oneHour: Dp, startOfDay: Long, endOfDay: Long) = oneHour*(getBlockHeight(startOfDay, endOfDay)/4f) - 1.dp
}
