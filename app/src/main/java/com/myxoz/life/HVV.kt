package com.myxoz.life

import com.myxoz.life.storage.api.syncables.LocationSyncable

object HVV {
    fun constructLink(from: LocationSyncable?, to: LocationSyncable?, startAt: String? = null) =
        "https://geofox.hvv.de/websearch/en/connections-mobile?execute=true&" +
                (if(from!=null) "start=${from.toHVVAddress()}&startCity=${from.city}&startType=COORDINATE&startX=${from.longitude}&startY=${from.lat}&" else "") +
                (if(to!=null) "destination=${to.toHVVAddress()}&destinationCity=${to.city}&destinationType=COORDINATE&destinationX=${to.longitude}&destinationY=${to.lat}" else "") +
                (if(startAt!=null) "&timeIsDeparture=1&date=${startAt.substringBefore(' ')}&time=${startAt.substringAfterLast(' ').substringBeforeLast(':').replace(":","%3A")}" else "")

    private fun LocationSyncable.toHVVAddress(): String = buildString {
        if (street != null) append(street)
        if (number != null) append(" $number")
        if (city != null) append(", $city")
    }
}