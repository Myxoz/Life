package com.myxoz.life.background.integration

import androidx.core.net.toUri
import com.myxoz.life.background.sensors.ParsedRoute
import com.myxoz.life.core.plus
import com.myxoz.life.core.roundToNearest15Min
import com.myxoz.life.storage.events.additionals.Vehicle
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

object HVV {
    fun parseTransitRoute(input: String): ParsedRoute? {
        val zone = ZoneId.systemDefault()
        val lines = input.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val partedLines = input.split("\n\n").dropLast(1).map { it.lines() } /*  Drop URL */
        val durationMap = mutableMapOf<Vehicle, Duration>()

        for (linePart in partedLines) {
            val vehicleLine = linePart.getOrNull(0) ?: continue
            val fromLine = linePart.getOrNull(1) ?: continue
            val toLine = linePart.getOrNull(2) ?: continue
            val stepContent = vehicleLine.substringAfter(")").trim()
            val vehicleType = when {
                stepContent.startsWith("Walk", ignoreCase = true) -> Vehicle.Walk
                stepContent.contains("(Bus)") -> Vehicle.Bus
                stepContent.startsWith("S") && stepContent.contains("➔") -> Vehicle.SBahn
                stepContent.startsWith("U") && stepContent.contains("➔") -> Vehicle.UBahn
                stepContent.startsWith("R") && stepContent.contains("➔") -> Vehicle.RBahn
                stepContent.contains("Change line") -> {
                    // Extract walk duration from change line
                    val walkMinIndex = stepContent.indexOf("walk")
                    if (walkMinIndex != -1) {
                        val afterWalk = stepContent.substring(walkMinIndex + 4).trim()
                        val minIndex = afterWalk.indexOf("min")
                        if (minIndex != -1) {
                            val numberStr = afterWalk.take(minIndex).trim()
                            val walkDuration = (numberStr.toIntOrNull() ?: 0).minutes
                            durationMap.merge(Vehicle.Walk, walkDuration, Duration::plus)
                        }
                    }
                    continue
                }
                else -> null
            }
            if (vehicleType != null) {
                val fromTime = fromLine
                    .substringAfter("From ")
                    .substringBefore(" ")
                    .getDayMinutes()
                val toTime = toLine
                    .substringAfter("To ")
                    .substringBefore(" ")
                    .getDayMinutes()

                val duration = if (fromTime > toTime) /* 23:59 > 0:01 */ toTime + 24.hours - fromTime else toTime - fromTime
                durationMap.merge(vehicleType, duration, Duration::plus)
            }
        }

        val url = (lines.lastOrNull() ?: return null).toUri()
        val startDate = url.getQueryParameter("date") ?: return null
        val localDate: LocalDate = LocalDate.of(
            startDate.substringAfterLast(".").toIntOrNull()?:return null,
            startDate.substringAfter(".").substringBefore(".").toIntOrNull()?:return null,
            startDate.substringBefore(".").toIntOrNull()?:return null
        )
        val start = if(url.getQueryParameter("startType")=="COORDINATE"){
            "c;"+url.getQueryParameter("startY")+";"+url.getQueryParameter("startX")
        } else {
            "l;"+partedLines.first()[1].substringAfter("From").trim().substringAfter(" ")
        }

        val end = if(url.getQueryParameter("destinationType")=="COORDINATE"){
            "c;"+url.getQueryParameter("destinationY")+";"+url.getQueryParameter("destinationX")
        } else {
            "l;"+ partedLines.last().last().substringAfter("To").trim().substringAfter(" ")
        }

        val timeIsDeparture = url.getQueryParameter("timeIsDeparture")=="1"
        val t =
            if(url.getQueryParameter("time")!=null)
                url.getQueryParameter("time")
            else
                if(timeIsDeparture)
                    partedLines.first()[1].substringAfter("From ").substringBefore(" ")
                else
                    partedLines.last().last().substringAfter("To ").substringBefore(" ")
        val fromHour = t?.substringBefore(":")?.toIntOrNull()?:return null
        val fromMinute = t.substringAfter(":").toIntOrNull()?:return null
        val fromTime: LocalDateTime = localDate.atTime(fromHour, fromMinute)
        val fromInstant: Instant = fromTime.atZone(zone).toInstant()

        val otherTime = if(!timeIsDeparture)
                partedLines.first()[1].substringAfter("From ").substringBefore(" ")
            else
                partedLines.last().last().substringAfter("To ").substringBefore(" ")
        val toHour = otherTime.substringBefore(":").toIntOrNull()?:return null
        val toMinute = otherTime.substringAfter(":").toIntOrNull()?:return null
        var toTime: LocalDateTime = localDate.atTime(toHour, toMinute)
        if(timeIsDeparture && toTime < fromTime )  {
            toTime = toTime.plusDays(1)
        }
        if(!timeIsDeparture && toTime > fromTime /* END */ )  {
            toTime = toTime.minusDays(1)
        }
        val toInstant: Instant = toTime.atZone(zone).toInstant()

        val startTime = (if(timeIsDeparture) fromInstant else toInstant).roundToNearest15Min()
        val endTime = (if(!timeIsDeparture) fromInstant else toInstant).roundToNearest15Min().coerceAtLeast(startTime + 15.minutes)
        return ParsedRoute(
            start,
            end,
            startTime,
            endTime,
            durationMap
        )
    }
    private fun String.getDayMinutes() = split(":").let { (it.getOrNull(0)?.toIntOrNull() ?: 0).hours + (it.getOrNull(1)?.toIntOrNull() ?: 0).minutes }
}