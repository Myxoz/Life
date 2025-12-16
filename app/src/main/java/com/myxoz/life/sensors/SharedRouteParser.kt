package com.myxoz.life.sensors

import android.content.Context
import com.myxoz.life.dbwrapper.DatabaseProvider
import com.myxoz.life.events.TravelEvent
import com.myxoz.life.events.additionals.TimedTagLikeContainer
import com.myxoz.life.events.additionals.Vehicle
import com.myxoz.life.integration.HVV
import com.myxoz.life.viewmodels.SharingContent

data class SharedRouteParser(val event: TravelEvent): SharingContent() {
    companion object {
        suspend fun from(sharedText: String, context: Context): TravelEvent?{
            // Write to shared preferences
            val parsedRoute = HVV.parseTransitRoute(sharedText) ?: return null
            val db = DatabaseProvider.getDatabase(context)
            val locations = db.locationDao()

            val subedStart = parsedRoute.startAddress.drop(2)
            val subedEnd = parsedRoute.endAddress.drop(2)
            println("Trying to search start in db:")
            val start = if(parsedRoute.startAddress.startsWith("c;")){
                val lat = subedStart.substringBefore(";").toDoubleOrNull().also { println("lat: $it") }?:return null
                val long = subedStart.substringAfter(";").toDoubleOrNull().also { println("long: $it") }?:return null
                locations.queryByCoordinate(lat,long)?.id
            } else if(subedStart.isNotBlank()) locations.queryLocation(
                subedStart.substringBeforeLast(",").substringBeforeLast(" ").also { println(it) },
                subedStart.substringBeforeLast(",").substringAfterLast(" ").also { println(it) },
                (subedStart.substringAfterLast(", ", "").takeIf { it.isNotBlank() } ?: "Hamburg").also { println(it) }
            )?.id else null
            println("Found: $start")

            println("Trying to search to in db:")
            val to = if(parsedRoute.endAddress.startsWith("c;")){
                val lat = subedEnd.substringBefore(";").toDoubleOrNull().also { println("lat: $it") }?:return null
                val long = subedEnd.substringAfter(";").toDoubleOrNull().also { println("long: $it") }?:return null
                locations.queryByCoordinate(lat,long)?.id
            } else if(subedEnd.isNotBlank()) locations.queryLocation(
                subedEnd.substringBeforeLast(",").substringBeforeLast(" ").also { println(it) },
                subedEnd.substringBeforeLast(",").substringAfterLast(" ").also { println(it) },
                (subedEnd.substringAfterLast(", ", "").takeIf { it.isNotBlank() } ?: "Hamburg").also { println(it) }
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