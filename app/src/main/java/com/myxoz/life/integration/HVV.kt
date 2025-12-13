package com.myxoz.life.integration

import android.icu.util.Calendar
import com.myxoz.life.api.Location
import com.myxoz.life.autodetect.roundToNearest15Min
import com.myxoz.life.events.Vehicle
import com.myxoz.life.sensors.ParsedRoute

object HVV {
    fun parseTransitRoute(input: String): ParsedRoute {
        val lines = input.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var startAddress = ""
        var endAddress = ""
        var startTime = ""
        var endTime = ""
        val durationMap = mutableMapOf<Vehicle, Int>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]

            // Check if this is a step line (starts with number)
            if (line.firstOrNull()?.isDigit() == true && line.contains(")")) {
                val stepContent = line.substringAfter(")").trim()

                // Determine vehicle type
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
                                val numberStr = afterWalk.substring(0, minIndex).trim()
                                val walkMin = numberStr.toIntOrNull() ?: 0
                                durationMap[Vehicle.Walk] = durationMap.getOrDefault(Vehicle.Walk, 0) + walkMin
                            }
                        }
                        i++
                        continue
                    }
                    else -> null
                }

                // Look for From and To lines
                var fromLine = ""
                var toLine = ""

                if (i + 1 < lines.size && lines[i + 1].startsWith("From")) {
                    fromLine = lines[i + 1]
                }
                if (i + 2 < lines.size && lines[i + 2].startsWith("To")) {
                    toLine = lines[i + 2]
                }

                // Extract start address (first occurrence)
                if (startAddress.isEmpty() && fromLine.isNotEmpty()) {
                    val fromContent = fromLine.substringAfter("From").trim()
                    // Extract address after time (format: "HH:MM Address")
                    val parts = fromContent.split(" ", limit = 2)
                    if (parts.size == 2) {
                        startAddress = parts[1].trim()
                        startTime = parts[0].trim()
                    }
                }

                // Extract end address (last walk step)
                if (vehicleType == Vehicle.Walk && toLine.isNotEmpty()) {
                    val toContent = toLine.substringAfter("To").trim()
                    val parts = toContent.split(" ", limit = 2)
                    if (parts.size == 2) {
                        endAddress = parts[1].trim()
                        endTime = parts[0].trim()
                    }
                }

                // Calculate duration
                if (vehicleType != null && fromLine.isNotEmpty() && toLine.isNotEmpty()) {
                    val fromTime = extractTime(fromLine)
                    val toTime = extractTime(toLine)

                    if (fromTime != null && toTime != null) {
                        val duration = calculateDuration(fromTime, toTime)
                        durationMap[vehicleType] = durationMap.getOrDefault(vehicleType, 0) + duration
                    }
                }
            }

            i++
        }

        var startTimeStamp = System.currentTimeMillis().roundToNearest15Min()
        var endTimeStamp = System.currentTimeMillis().roundToNearest15Min()
        val calendar = Calendar.getInstance()
        if(startTime.isNotBlank()){
            calendar.set(Calendar.HOUR_OF_DAY, startTime.split(":")[0].toInt())
            calendar.set(Calendar.MINUTE, startTime.split(":")[1].toInt())
            startTimeStamp = calendar.timeInMillis
        }
        if(endTime.isNotBlank()){
            calendar.set(Calendar.HOUR_OF_DAY, endTime.split(":")[0].toInt())
            calendar.set(Calendar.MINUTE, endTime.split(":")[1].toInt())
            endTimeStamp = calendar.timeInMillis
        }
        if(startTimeStamp > endTimeStamp){
            endTimeStamp+=24*60*60L*1000L
        }
        return ParsedRoute(
            startAddress,
            endAddress,
            startTimeStamp.roundToNearest15Min(),
            endTimeStamp.roundToNearest15Min(),
            durationMap
        )
    }
    fun extractTime(line: String): Pair<Int, Int>? {
        // Find time pattern HH:MM after "From" or "To"
        val content = when {
            line.contains("From") -> line.substringAfter("From").trim()
            line.contains("To") -> line.substringAfter("To").trim()
            else -> return null
        }

        // Time is at the beginning, format: HH:MM
        val firstSpace = content.indexOf(" ")
        if (firstSpace == -1) return null

        val timeStr = content.take(firstSpace).trim()
        val parts = timeStr.split(":")

        if (parts.size != 2) return null

        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].toIntOrNull() ?: return null

        return Pair(hours, minutes)
    }

    fun calculateDuration(from: Pair<Int, Int>, to: Pair<Int, Int>): Int {
        val fromMinutes = from.first * 60 + from.second
        var toMinutes = to.first * 60 + to.second

        // Handle midnight crossing (e.g., 23:57 to 00:10)
        if (toMinutes < fromMinutes) {
            toMinutes += 24 * 60
        }

        return toMinutes - fromMinutes
    }
    fun constructLink(from: Location?, to: Location?, startAt: String? = null) =
        "https://geofox.hvv.de/websearch/en/connections-mobile?execute=true&" +
                (if(from!=null) "start=${from.toAddress().substringBeforeLast(",")}&startCity=${from.city}&startType=COORDINATE&startX=${from.longitude}&startY=${from.lat}&" else "") +
                (if(to!=null) "destination=${to.toAddress().substringBeforeLast(",")}&destinationCity=${to.city}&destinationType=COORDINATE&destinationX=${to.longitude}&destinationY=${to.lat}" else "") +
                (if(startAt!=null) "&timeIsDeparture=1&date=${startAt.substringBefore(' ')}&time=${startAt.substringAfterLast(' ').substringBeforeLast(':').replace(":","%3A")}" else "")
}