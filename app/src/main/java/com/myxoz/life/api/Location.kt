package com.myxoz.life.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.MainThread
import androidx.core.net.toUri
import com.myxoz.life.calendar.getEventId
import com.myxoz.life.dbwrapper.LocationEntity
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.events.getStringOrNull
import org.json.JSONObject

class Location(
    val name: String,
    val longitude: Double,
    val lat: Double,
    val radiusM: Int,
    val ssid: String?,
    val street: String?,
    val number: String?,
    val city: String?,
    val country: String,
    id: Long
) : Syncable(SpecialSyncablesIds.LOCATIONS, id) {
    override suspend fun specificsToJson(db: StorageManager): JSONObject? = JSONObject()
        .put("name", name)
        .put("longitude", longitude)
        .put("lat", lat)
        .put("radiusm", radiusM)
        .put("ssid", ssid ?: JSONObject.NULL)
        .put("street", street ?: JSONObject.NULL)
        .put("number", number ?: JSONObject.NULL)
        .put("city", city ?: JSONObject.NULL)
        .put("country", country)

    override suspend fun saveToDB(db: StorageManager) {
        db.location.insertLocation(
            LocationEntity(
                id, name, longitude, lat, radiusM, ssid, street, number, city, country
            )
        )
    }

    fun toAddress(stripCountry: Boolean = false): String = buildString {
        if (street != null) append(street)
        if (number != null) append(" $number")
        if (city != null) append(", $city")
        if (!stripCountry) append(", $country")
    }

    fun toCords(): String {
        fun toDMS(coordinate: Double, isLat: Boolean): String {
            val abs = kotlin.math.abs(coordinate)
            val degrees = abs.toInt()
            val minutesFloat = (abs - degrees) * 60
            val minutes = minutesFloat.toInt()
            val seconds = (minutesFloat - minutes) * 60
            val hemisphere = when {
                isLat && coordinate >= 0 -> "N"
                isLat && coordinate < 0 -> "S"
                !isLat && coordinate >= 0 -> "E"
                else -> "W"
            }
            return "%d°%d'%1.1f\"%s".format(degrees, minutes, seconds, hemisphere)
        }

        val latDMS = toDMS(lat, true)
        val lonDMS = toDMS(longitude, false)

        return "%.6f, %.6f (%s, %s)".format(lat, longitude, latDMS, lonDMS)
    }

    companion object : ServerSyncableCompanion {
        override suspend fun overwriteByJson(db: StorageManager, it: JSONObject) {
            fromJSON(it).saveToDB(db)
        }

        suspend fun fromDB(db: StorageManager, id: Long): Location? {
            return from(db.location.getLocation(id) ?: return null)
        }

        fun fromJSON(json: JSONObject): Location = Location(
            json.getString("name"),
            json.getDouble("longitude"),
            json.getDouble("lat"),
            json.getInt("radiusm"),
            json.getStringOrNull("ssid"),
            json.getStringOrNull("street"),
            json.getStringOrNull("number"),
            json.getStringOrNull("city"),
            json.getString("country"),
            json.getEventId()
        )

        fun from(en: LocationEntity) = Location(
            en.name, en.longitude, en.lat, en.radiusM,
            en.ssid, en.street, en.number, en.city,
            en.country, en.id
        )

        @MainThread
        fun openLink(context: Context, link: String) {
            println("Opening link: $link")
            val uri = link.toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }

        fun openInGoogleMaps(context: Context, location: Location?) = openInGoogleMaps(
            context,
            location?.toAddress(),
            location?.lat,
            location?.longitude,
            location?.name ?: ""
        )

        fun openInGoogleMaps(
            context: Context,
            address: String? = null,
            lat: Double? = null,
            lon: Double? = null,
            label: String = ""
        ) {
            val uri = when {
                lat != null && lon != null ->
                    "geo:$lat,$lon?q=$lat,$lon($label)".toUri()

                address != null ->
                    "geo:0,0?q=${Uri.encode(address)}".toUri()

                else -> return
            }

            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.setPackage("com.google.android.apps.maps")
            context.startActivity(intent)
        }

        fun openRouteInGoogleMaps(
            context: Context,
            fromAddress: String? = null,
            fromLat: Double? = null,
            fromLon: Double? = null,
            toAddress: String? = null,
            toLat: Double? = null,
            toLon: Double? = null,
            mode: String = "transit" // can be "driving", "walking", "transit", "bicycling"
        ) {
            val origin = when {
                fromLat != null && fromLon != null -> "$fromLat,$fromLon"
                !fromAddress.isNullOrBlank() -> Uri.encode(fromAddress)
                else -> "My Location"
            }

            val destination = when {
                toLat != null && toLon != null -> "$toLat,$toLon"
                !toAddress.isNullOrBlank() -> Uri.encode(toAddress)
                else -> return
            }

            val uri = ("https://www.google.com/maps/dir/?api=1" +
                    "&origin=$origin" +
                    "&destination=$destination" +
                    "&travelmode=$mode").toUri()

            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.setPackage("com.google.android.apps.maps")
            context.startActivity(intent)
        }
    }
}