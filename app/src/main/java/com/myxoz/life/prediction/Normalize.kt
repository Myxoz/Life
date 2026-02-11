package com.myxoz.life.prediction

import com.myxoz.life.utils.atStartAsMillis
import com.myxoz.life.utils.toLocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

object Normalize {
    private val zone: ZoneId = ZoneId.systemDefault()
    fun DoubleArray.putTime(index: Int, timestampMs: Long): Int {
        var index = index
        val ts = unixTimestamp(timestampMs)
        this[index++] = ts.timeX
        this[index++] = ts.timeY
        this[index++] = ts.weekDayX
        this[index++] = ts.weekDayY
        return index
    }
    fun unixTimestamp(timestampMs: Long): DateResult {
        val day = timestampMs.toLocalDate(zone)
        val startOfDay = day.atStartAsMillis(zone)
        val angle = toAngle(((timestampMs - startOfDay) / 1000) / 86400.0)
        val weekDayAngle = toAngle((day.dayOfWeek.value-1)/7.0)
        return DateResult(
            sin(angle),
            cos(angle),
            sin(weekDayAngle),
            cos(weekDayAngle)
        )
    }
    private fun toAngle(value: Double) = 2.0 * Math.PI * value
    data class DateResult(
        val timeX: Double,
        val timeY: Double,
        val weekDayX: Double,
        val weekDayY: Double,
    )
    fun Iterable<Double>.stdDev(): Double {
        val count = this.count()
        if (count < 1) return 0.0

        val mean = this.average()
        val sumOfSquaredDifferences = this.sumOf { (it - mean) * (it - mean) }

        return sqrt(sumOfSquaredDifferences / count)
    }
    fun Double.lnUnsigned() = (if(this < 0) -1 else 1) * ln(abs(this) + 1.0)
}