package com.myxoz.life.integration

import android.icu.util.Calendar
import androidx.core.net.toUri
import com.myxoz.life.api.Location
import com.myxoz.life.autodetect.roundToNearest15Min
import com.myxoz.life.events.additionals.Vehicle
import com.myxoz.life.sensors.ParsedRoute

object HVV {
    fun parseTransitRoute(input: String): ParsedRoute? {
        val lines = input.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val partedLines = input.split("\n\n").dropLast(1).map { it.lines() } /*  Drop URL */
        val durationMap = mutableMapOf<Vehicle, Int>()

        for (linePart in partedLines) {
            val vehicleLine = linePart.getOrNull(0) ?: continue
            val fromLine = linePart.getOrNull(1) ?: continue
            val toLine = linePart.getOrNull(2) ?: continue
            val stepContent = vehicleLine.substringAfter(")").trim()
            val vehicleType = when {
                stepContent.startsWith("Walk", ignoreCase = true) -> Vehicle.Gehen
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
                            val walkMin = numberStr.toIntOrNull() ?: 0
                            durationMap[Vehicle.Gehen] =
                                durationMap.getOrDefault(Vehicle.Gehen, 0) + walkMin
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

                val duration = if (fromTime > toTime) /* 23:59 > 0:01 */ toTime + 24 * 60 - fromTime else toTime - fromTime
                durationMap[vehicleType] = durationMap.getOrDefault(vehicleType, 0) + duration
            }
        }

        val url = (lines.lastOrNull() ?: return null).toUri()
        val calendar = Calendar.getInstance()
        val startDate = url.getQueryParameter("date") ?: return null
        calendar.set(Calendar.DAY_OF_MONTH, startDate.substringBefore(".").toIntOrNull()?:return null)
        calendar.set(Calendar.MONTH, startDate.substringAfter(".").substringBefore(".").toIntOrNull()?.minus(1)?:return null)
        calendar.set(Calendar.YEAR, startDate.substringAfterLast(".").toIntOrNull()?:return null)

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
        calendar.set(Calendar.HOUR_OF_DAY, t?.substringBefore(":")?.toIntOrNull()?:return null)
        calendar.set(Calendar.MINUTE, t.substringAfter(":").toIntOrNull()?:return null)
        val oneTimePart = calendar.timeInMillis
        val otherTime = if(!timeIsDeparture)
                partedLines.first()[1].substringAfter("From ").substringBefore(" ")
            else
                partedLines.last().last().substringAfter("To ").substringBefore(" ")
        calendar.set(Calendar.HOUR_OF_DAY, otherTime.substringBefore(":").toIntOrNull()?:return null)
        calendar.set(Calendar.MINUTE, otherTime.substringAfter(":").toIntOrNull()?:return null)
        var otherTimePart = calendar.timeInMillis
        if(timeIsDeparture && otherTimePart < oneTimePart )  {
            calendar.add(Calendar.DATE, 1)
        }
        if(!timeIsDeparture && otherTimePart > oneTimePart /* END */ )  {
            calendar.add(Calendar.DATE, -1)
        }
        otherTimePart = calendar.timeInMillis

        val startTime = (if(timeIsDeparture) oneTimePart else otherTimePart).roundToNearest15Min()
        val endTime = (if(!timeIsDeparture) oneTimePart else otherTimePart).roundToNearest15Min().coerceAtLeast(startTime+15*1000L*60)
        return ParsedRoute(
            start,
            end,
            startTime,
            endTime,
            durationMap
        )
    }

    fun constructLink(from: Location?, to: Location?, startAt: String? = null) =
        "https://geofox.hvv.de/websearch/en/connections-mobile?execute=true&" +
                (if(from!=null) "start=${from.toAddress().substringBeforeLast(",")}&startCity=${from.city}&startType=COORDINATE&startX=${from.longitude}&startY=${from.lat}&" else "") +
                (if(to!=null) "destination=${to.toAddress().substringBeforeLast(",")}&destinationCity=${to.city}&destinationType=COORDINATE&destinationX=${to.longitude}&destinationY=${to.lat}" else "") +
                (if(startAt!=null) "&timeIsDeparture=1&date=${startAt.substringBefore(' ')}&time=${startAt.substringAfterLast(' ').substringBeforeLast(':').replace(":","%3A")}" else "")

    private fun String.getDayMinutes() = this.split(":").let { (it.getOrNull(0)?.toIntOrNull() ?: 0)*60 + (it.getOrNull(1)?.toIntOrNull() ?: 0) }
}