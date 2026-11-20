package com.myxoz.life.background.sensors

import android.content.Context
import android.util.Log
import com.myxoz.life.background.integration.HVV
import com.myxoz.life.storage.DatabaseInterfacable
import com.myxoz.life.storage.events.TravelEvent
import com.myxoz.life.storage.events.additionals.EventTime
import com.myxoz.life.storage.events.additionals.TimedTagLikeContainer
import com.myxoz.life.storage.events.additionals.Vehicle
import java.time.Instant
import kotlin.time.Duration

data class SharedRouteParser(val event: TravelEvent) {
    companion object {
        suspend fun from(sharedText: String, context: Context): TravelEvent?{
            val parsedRoute = HVV.parseTransitRoute(sharedText) ?: return null
            val db = (context.applicationContext as DatabaseInterfacable).dbInterface

            val subedStart = parsedRoute.startAddress.drop(2)
            val subedEnd = parsedRoute.endAddress.drop(2)
            Log.d("SharedRouteParser", "Trying to search start in db:")
            val start = if(parsedRoute.startAddress.startsWith("c;")){
                val lat = subedStart.substringBefore(";").toDoubleOrNull().also { Log.d("SharedRouteParser","lat: $it") }?:return null
                val long = subedStart.substringAfter(";").toDoubleOrNull().also { Log.d("SharedRouteParser","long: $it") }?:return null
                db.locationInterface.queryByCoordinate(lat,long)?.id
            } else if(subedStart.isNotBlank()) db.locationInterface.queryLocation(
                subedStart.substringBeforeLast(",").substringBeforeLast(" ").also { Log.d("SharedRouteParser",it) },
                subedStart.substringBeforeLast(",").substringAfterLast(" ").also { Log.d("SharedRouteParser",it) },
                (subedStart.substringAfterLast(", ", "").takeIf { it.isNotBlank() } ?: "Hamburg").also { Log.d("SharedRouteParser",it) }
            )?.id else null
            Log.d("SharedRouteParser","Found: $start")

            Log.d("SharedRouteParser","Trying to search to in db:")
            val to = if(parsedRoute.endAddress.startsWith("c;")){
                val lat = subedEnd.substringBefore(";").toDoubleOrNull().also { Log.d("SharedRouteParser","lat: $it") }?:return null
                val long = subedEnd.substringAfter(";").toDoubleOrNull().also { Log.d("SharedRouteParser","long: $it") }?:return null
                db.locationInterface.queryByCoordinate(lat,long)?.id
            } else if(subedStart.isNotBlank()) db.locationInterface.queryLocation(
                subedEnd.substringBeforeLast(",").substringBeforeLast(" ").also { Log.d("SharedRouteParser",it) },
                subedEnd.substringBeforeLast(",").substringAfterLast(" ").also { Log.d("SharedRouteParser",it) },
                (subedEnd.substringAfterLast(", ", "").takeIf { it.isNotBlank() } ?: "Hamburg").also { Log.d("SharedRouteParser",it) }
            )?.id else null
            Log.d("SharedRouteParser","Found: $to")

            return TravelEvent(
                EventTime(
                    parsedRoute.startTime,
                    parsedRoute.endTime,
                    unspecifiedStart = false,
                    unspecifiedLength = false
                ),
                from = start ?: return null,
                to = to  ?: return null,
                vehicles = parsedRoute.vehicleDurations.entries.map {
                    TimedTagLikeContainer(it.key, it.value)
                }
            )
        }
    }
}
data class ParsedRoute(
    val startAddress: String,
    val endAddress: String,
    val startTime: Instant,
    val endTime: Instant,
    val vehicleDurations: Map<Vehicle, Duration>
)