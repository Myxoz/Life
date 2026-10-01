package com.myxoz.life.android.sensors

import android.content.Context
import android.util.Log
import com.myxoz.life.android.integration.HVV
import com.myxoz.life.storage.events.TravelEvent
import com.myxoz.life.storage.events.additionals.TimedTagLikeContainer
import com.myxoz.life.storage.events.additionals.Vehicle
import com.myxoz.life.storage.dbwrapper.DatabaseProvider
import com.myxoz.life.storage.interfaces.LocationInterface

data class SharedRouteParser(val event: com.myxoz.life.storage.events.TravelEvent) {
    companion object {
        suspend fun from(sharedText: String, context: Context, locationInterface: LocationInterface): com.myxoz.life.storage.events.TravelEvent?{
            val parsedRoute = HVV.parseTransitRoute(sharedText) ?: return null
            val db = DatabaseProvider.getDatabase(context)

            val subedStart = parsedRoute.startAddress.drop(2)
            val subedEnd = parsedRoute.endAddress.drop(2)
            Log.d("SharedRouteParser", "Trying to search start in db:")
            val start = if(parsedRoute.startAddress.startsWith("c;")){
                val lat = subedStart.substringBefore(";").toDoubleOrNull().also { Log.d("SharedRouteParser","lat: $it") }?:return null
                val long = subedStart.substringAfter(";").toDoubleOrNull().also { Log.d("SharedRouteParser","long: $it") }?:return null
                locationInterface.queryByCoordinate(lat,long)?.id
            } else if(subedStart.isNotBlank()) db.readLocationsDao().queryLocation(
                subedStart.substringBeforeLast(",").substringBeforeLast(" ").also { Log.d("SharedRouteParser",it) },
                subedStart.substringBeforeLast(",").substringAfterLast(" ").also { Log.d("SharedRouteParser",it) },
                (subedStart.substringAfterLast(", ", "").takeIf { it.isNotBlank() } ?: "Hamburg").also { Log.d("SharedRouteParser",it) }
            )?.id else null
            Log.d("SharedRouteParser","Found: $start")

            Log.d("SharedRouteParser","Trying to search to in db:")
            val to = if(parsedRoute.endAddress.startsWith("c;")){
                val lat = subedEnd.substringBefore(";").toDoubleOrNull().also { Log.d("SharedRouteParser","lat: $it") }?:return null
                val long = subedEnd.substringAfter(";").toDoubleOrNull().also { Log.d("SharedRouteParser","long: $it") }?:return null
                locationInterface.queryByCoordinate(lat,long)?.id
            } else if(subedEnd.isNotBlank()) db.readLocationsDao().queryLocation(
                subedEnd.substringBeforeLast(",").substringBeforeLast(" ").also { Log.d("SharedRouteParser",it) },
                subedEnd.substringBeforeLast(",").substringAfterLast(" ").also { Log.d("SharedRouteParser",it) },
                (subedEnd.substringAfterLast(", ", "").takeIf { it.isNotBlank() } ?: "Hamburg").also { Log.d("SharedRouteParser",it) }
            )?.id else null
            Log.d("SharedRouteParser","Found: $to")

            return _root_ide_package_.com.myxoz.life.storage.events.TravelEvent(
                parsedRoute.startTime,
                parsedRoute.endTime,
                usl = false,
                uss = false,
                from = start ?: 0L,
                to = to ?: 0L,
                vehicles = parsedRoute.vehicleDurations.entries.map {
                    _root_ide_package_.com.myxoz.life.storage.events.additionals.TimedTagLikeContainer(
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
    val vehicleDurations: Map<com.myxoz.life.storage.events.additionals.Vehicle, Int>
)