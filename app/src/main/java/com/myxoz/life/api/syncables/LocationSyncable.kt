package com.myxoz.life.api.syncables

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.myxoz.life.api.ServerSyncableCompanion
import com.myxoz.life.api.Syncable
import com.myxoz.life.dbwrapper.LocationEntity
import com.myxoz.life.api.API
import com.myxoz.life.screens.feed.fullscreenevent.getId
import com.myxoz.life.utils.getStringOrNull
import org.json.JSONObject
import kotlin.math.abs

class LocationSyncable(
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
    override suspend fun specificsToJson(): JSONObject? = JSONObject()
        .put("name", name)
        .put("longitude", longitude)
        .put("lat", lat)
        .put("radiusm", radiusM)
        .put("ssid", ssid ?: JSONObject.NULL)
        .put("street", street ?: JSONObject.NULL)
        .put("number", number ?: JSONObject.NULL)
        .put("city", city ?: JSONObject.NULL)
        .put("country", country)

    override suspend fun saveToDB(db: API.WriteSyncableDaos) {
        db.locationsDao.insertLocation(
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

    fun toCords() = coordinatesToString(lat, longitude)

    companion object : ServerSyncableCompanion<LocationSyncable> {
        fun coordinatesToString(lat: Double, longitude: Double): String {
            return "%.6f, %.6f (${coordsToDMS(lat, longitude)})".format(lat, longitude)
        }
        fun coordsToDMS(lat: Double, longitude: Double): String {
            fun toDMS(coordinate: Double, isLat: Boolean): String {
                val abs = abs(coordinate)
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
            return "$latDMS, $lonDMS"
        }

        override suspend fun fromDB(db: API.ReadSyncableDaos, id: Long): LocationSyncable? {
            return from(db.locationsDao.getLocationById(id) ?: return null)
        }

        override fun fromJSON(json: JSONObject): LocationSyncable = LocationSyncable(
            json.getString("name"),
            json.getDouble("longitude"),
            json.getDouble("lat"),
            json.getInt("radiusm"),
            json.getStringOrNull("ssid"),
            json.getStringOrNull("street"),
            json.getStringOrNull("number"),
            json.getStringOrNull("city"),
            json.getString("country"),
            json.getId()
        )

        fun from(en: LocationEntity) = LocationSyncable(
            en.name, en.longitude, en.lat, en.radiusM,
            en.ssid, en.street, en.number, en.city,
            en.country, en.id
        )

        fun openInGoogleMaps(context: Context, location: LocationSyncable?) = openInGoogleMaps(
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