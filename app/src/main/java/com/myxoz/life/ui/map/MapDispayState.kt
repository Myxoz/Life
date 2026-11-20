package com.myxoz.life.ui.map

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.LocationSyncable
import com.myxoz.life.core.def
import com.myxoz.life.core.nullIfEmpty
import com.myxoz.life.storage.api.syncables.LocationSyncable
import com.myxoz.life.storage.interfaces.LocationInterface
import com.myxoz.life.ui.map.MapDisplayState.DisplayedLifeLocation
import com.myxoz.life.ui.map.MapDisplayState.EditingLocation
import com.myxoz.life.utils.def
import com.myxoz.life.utils.nullIfEmpty

sealed interface MapLocation {
    val displayName: String
    val subTitle: String
}
sealed interface MapDisplayState {
    class SearchPage(initialQuery: String?): MapDisplayState {
        var onlineSearchResults by mutableStateOf<List<ResolvedLocation>?>(null)
        var inputQuery by mutableStateOf(initialQuery ?: "")
    }
    class PinNotResolved(override val lat: Double, override val longitude: Double): MapDisplayState, PinPointable, MapLocation, Editable {
        override val radiusM: Int? = null
        override val displayName: String = "Markierung"
        override val subTitle = "Lädt..."
        override fun toEditing(): EditingLocation = EditingLocation(
            basedOnId = null,
            initialName = null,
            initialLatitute = lat,
            initialLongitude = longitude,
            initialRadiusM = null,
            initialStreet = null,
            initialNumber = null,
            initialPlace = null,
            initialCountry = null,
            initialSSID = null
        )
    }

    class ResolvedLocation(
        override val displayName: String,
        override val lat: Double,
        override val longitude: Double,
        override val street: String?,
        override val number: String?,
        override val city: String?,
        override val country: String
    ): MapDisplayState, LocationDisplayable, MapLocation {
        override val radiusM: Int? = null
        override val subTitle: String = toAddress()
    }

    class DisplayedLifeLocation(val location: LocationSyncable): MapDisplayState, LocationDisplayable, MapLocation, MapNeedsUpdates {
        // We are doing this due to the sealed interface that we cannot apply to LocationSyncable directly....
        val id = location.id
        override var street: String? by mutableStateOf(location.street); private set
        override var number: String? by mutableStateOf(location.number); private set
        override var city: String? by mutableStateOf(location.city); private set
        override var country: String by mutableStateOf(location.country); private set
        override val subTitle: String = toAddress()
        var ssid: String? by mutableStateOf(location.ssid); private set
        override var lat by mutableDoubleStateOf(location.lat); private set
        override var longitude by mutableDoubleStateOf(location.longitude); private set
        override var radiusM by mutableIntStateOf(location.radiusM); private set
        override var displayName by mutableStateOf(location.name)
        override suspend fun keepUpdated(dbInterface: LocationInterface) {
            dbInterface.getLocationById(location.id).collect {
                it ?: return@collect
                displayName = it.name
                longitude = it.longitude
                lat = it.lat
                radiusM = it.radiusM
                ssid = it.ssid
                street = it.street
                number = it.number
                city = it.city
                country = it.country
            }
        }
    }

    class EditingLocation(
        val basedOnId: Long?,
        initialName: String?,
        initialLatitute: Double?,
        initialLongitude: Double?,
        initialRadiusM: Int?,
        initialStreet: String?,
        initialNumber: String?,
        initialPlace: String?,
        initialCountry: String?,
        initialSSID: String?,
    ): MapDisplayState, PinPointable, MapLocation {
        override var displayName by mutableStateOf(initialName ?: "")
        override val subTitle: String by derivedStateOf {
            buildString {
                if (streetField.isNotEmpty()) append(streetField)
                if (numberField.isNotEmpty()) append(" $numberField")
                if (cityCountryInput.isNotEmpty()) append(", $cityCountryInput")
            }
        }
        var coordsField by mutableStateOf(if(initialLatitute != null && initialLongitude != null) "%.6f, %.6f".format(initialLatitute, initialLongitude) else "lat, long")
            private set
        var radiusMField by mutableStateOf(initialRadiusM?.toString() ?: "")
            private set
        override var radiusM by mutableStateOf(initialRadiusM)
            private set
        override var longitude by mutableStateOf(initialLongitude)
        override var lat by mutableStateOf(initialLatitute)
        var streetField by mutableStateOf(initialStreet?:"")
        var numberField by mutableStateOf(initialNumber?:"")
        var cityCountryInput by mutableStateOf(initialPlace?.let { "$it, " }.def("") + (initialCountry ?: "Germany"))
        var selectingCoordsOnMap by mutableStateOf(false)
        var ssid by mutableStateOf(initialSSID?:"")
        fun convertToLocationSyncable(): Pair<LocationSyncable?, String?>{
            val cityCountryMix = cityCountryInput.split(",")
            if(cityCountryMix.size !in 1..2) return null to "Stadt, Land sollte genau ein Komma enthalten"
            val (coords, returnString) = coordsToTuple(coordsField)
            if(coords == null) return null to returnString
            val (lat, longitude) = coords
            return LocationSyncable(
                displayName.nullIfEmpty() ?: return null to "Gib einen Name ein",
                longitude,
                lat,
                radiusMField.toIntOrNull()?.takeIf { it != 0 } ?: return null to "Gib ein Zahl > 0 als Radius ein z.B. 10",
                ssid.nullIfEmpty(),
                streetField.nullIfEmpty(),
                numberField.nullIfEmpty(),
                if(cityCountryMix.size == 2) cityCountryMix[0].trim() else null,
                cityCountryMix[if(cityCountryMix.size == 1) 0 else 1].trim(),
                basedOnId ?: API.generateId()
            ) to null
        }

        fun setCoords(content: String) {
            val coordsJoined = coordsToTuple(content)
            lat = coordsJoined.first?.first
            longitude = coordsJoined.first?.second
            coordsField = content
        }

        fun setRadius(newRadius: String) {
            radiusM = newRadius.toIntOrNull()
            radiusMField = newRadius
        }

        fun handleMapClick(latitude: Double, longitude: Double) {
            if(!selectingCoordsOnMap) return
            this.lat = latitude
            this.longitude = longitude
            this.coordsField = "%.6f, %.6f".format(latitude, longitude)
        }

        companion object {
            fun coordsToTuple(coordsInput: String): Pair<Pair<Double, Double>?, String?> {
                val coords = coordsInput.split(",")
                if(coords.size != 2 && coords.size != 4) return null to "Die Koordinaten sollten 1 oder 3 Kommas enthalten: '1.23, 3.45' oder '1,23, 3,45'"
                val (lat, longitude) = if(coords.size == 2)
                    coords[0].trim().toDoubleOrNull() to coords[1].trim().toDoubleOrNull()
                else
                    "${coords[0].trim()}.${coords[1].trim()}".toDoubleOrNull() to "${coords[2].trim()}.${coords[3].trim()}".trim().toDoubleOrNull()
                if(lat==null) return null to "Gib ein validen Breitengrad ein z.B. 12.456 oder 12,456"
                if(longitude==null) return null to "Gib ein validen Längengrad ein z.B. 12.456 oder 12,456"
                return (lat to longitude) to null
            }
        }
    }
}
sealed interface LocationDisplayable: Adressable, PinPointable, Editable {
    val displayName: String
    override fun toEditing() = EditingLocation(
        (this as? DisplayedLifeLocation)?.id,
        this.displayName,
        this.lat,
        this.longitude,
        this.radiusM,
        this.street,
        this.number,
        this.city,
        this.country,
        (this as? DisplayedLifeLocation)?.ssid,
    )

    fun toCoords(): String {
        return "%.6f, %.6f".format(lat ?: return "", longitude ?: return "")
    }

    fun toDMS(): String? {
        return LocationSyncable.coordsToDMS(lat ?: return null, longitude ?: return null)
    }
}
sealed interface Editable {
    fun toEditing(): EditingLocation
}
interface Adressable {
    val street: String?
    val number: String?
    val city: String?
    val country: String
    fun toAddress(stripCountry: Boolean = false): String = buildString {
        if (street != null) append(street)
        if (number != null) append(" $number")
        if (city != null) append(", $city")
        if (!stripCountry) append(", $country")
    }
}
interface PinPointable {
    val lat: Double?
    val longitude: Double?
    val radiusM: Int?
}

interface MapNeedsUpdates{
    suspend fun keepUpdated(dbInterface: LocationInterface)
}