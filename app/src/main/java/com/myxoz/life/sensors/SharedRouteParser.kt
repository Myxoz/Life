package com.myxoz.life.sensors

import android.content.Context
import com.myxoz.life.dbwrapper.DatabaseProvider
import com.myxoz.life.events.TravelEvent
import com.myxoz.life.events.Vehicle
import com.myxoz.life.events.additionals.TimedTagLikeContainer
import com.myxoz.life.integration.HVV
import com.myxoz.life.viewmodels.SharingContent

data class SharedRouteParser(val event: TravelEvent): SharingContent() {
    companion object {
        suspend fun from(sharedText: String, context: Context): TravelEvent{
            // Write to shared preferences
            val parsedRoute = HVV.parseTransitRoute(sharedText)
            val db = DatabaseProvider.getDatabase(context)
            val locations = db.locationDao()

            println("Trying to search start in db:")
            val start = if(parsedRoute.startAddress.isNotBlank()) locations.queryLocation(
                parsedRoute.startAddress.substringBeforeLast(",").substringBeforeLast(" ").also { println(it) },
                parsedRoute.startAddress.substringBeforeLast(",").substringAfterLast(" ").also { println(it) },
                (parsedRoute.startAddress.substringAfterLast(", ", "").takeIf { it.isNotBlank() } ?: "Hamburg").also { println(it) }
            )?.id else null
            println("Found: $start")

            println("Trying to search to in db:")
            val to = if(parsedRoute.endAddress.isNotBlank()) locations.queryLocation(
                parsedRoute.endAddress.substringBeforeLast(",").substringBeforeLast(" ").also { println(it) },
                parsedRoute.endAddress.substringBeforeLast(",").substringAfterLast(" ").also { println(it) },
                (parsedRoute.endAddress.substringAfterLast(", ", "").takeIf { it.isNotBlank() } ?: "Hamburg").also { println(it) }
            )?.id else null
            println("Found: $to")

            return TravelEvent(
                parsedRoute.startTime,
                parsedRoute.endTime,
                usl = false,
                uss = false,
                from = start ?: 0L,
                to = to ?: 0L,
                vehicles = parsedRoute.vehicleDurations.entries.map {
                    TimedTagLikeContainer(
                        it.key,
                        it.value * 1000L * 60L
                    )
                }
            )
        }
    }
}
data class ParsedRoute(
    val startAddress: String,
    val endAddress: String,
    val startTime: Long,
    val endTime: Long,
    val vehicleDurations: Map<Vehicle, Int>
)