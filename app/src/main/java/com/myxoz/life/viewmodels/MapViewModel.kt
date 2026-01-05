package com.myxoz.life.viewmodels

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraState
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.Location
import com.myxoz.life.ui.ThreeStateBottomSheetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MapViewModel(val prefs: SharedPreferences): ViewModel() {
    private val _sheetLocation = MutableStateFlow<Location?>(null)
    val sheetLocation = _sheetLocation.asStateFlow()
    val sheetState = ThreeStateBottomSheetState()
    val selectedCoordinates = MutableStateFlow<Point?>(null)
    var lifeSearchResults = MutableStateFlow<List<Location>?>(null)
    var mapBoxSearchResults = MutableStateFlow<List<Location>?>(null)
    val decodedLocation = MutableStateFlow<Location?>(null)
    val isEditing = MutableStateFlow(false)
    val nameInput = MutableStateFlow<String?>(null)
    val coordsInput = MutableStateFlow<String?>(null)
    val radiusMInput = MutableStateFlow<String?>(null)
    val ssidInput = MutableStateFlow<String?>(null)
    val streetInput = MutableStateFlow<String?>(null)
    val numberInput = MutableStateFlow<String?>(null)
    val cityCountryInput = MutableStateFlow<String?>(null)
    val lifeQuery = MutableStateFlow<String?>(null)
    val cameraOptions: MapViewportState by lazy {
        MapViewportState().apply {
            setCameraOptions {
                zoom(prefs.getFloat("map_zoom", 10f).toDouble())
                center(Point.fromLngLat(prefs.getFloat("map_center_long", 9.9872f).toDouble(), prefs.getFloat("map_center_lat", 53.5488f).toDouble()))
                pitch(prefs.getFloat("map_pitch", 10f).toDouble())
                bearing(prefs.getFloat("map_bearing", 10f).toDouble())
            }
        }
    }
    fun saveCameraPosition(state: CameraState){
        prefs.edit {
            putFloat("map_zoom", state.zoom.toFloat())
            putFloat("map_center_long", state.center.longitude().toFloat())
            putFloat("map_center_lat", state.center.latitude().toFloat())
            putFloat("map_pitch", state.pitch.toFloat())
            putFloat("map_bearing", state.bearing.toFloat())
        }
    }
    fun setInputValuesByLocation(location: Location){
        nameInput.value = location.name
        numberInput.value = location.number
        coordsInput.value = "%.6f, %.6f".format(location.lat, location.longitude)

        radiusMInput.value = location.radiusM.takeIf { it != 0 }?.toString()
        ssidInput.value = location.ssid
        streetInput.value = location.street
        cityCountryInput.value = "${location.city}, ${location.country}"
    }
    fun parseLocation(id: Long): Pair<Location?, String?>{
        val coords = coordsInput.value?.split(",") ?: return null to "Gib Koordinaten ein"
        if(coords.size != 2) return null to "Die Koordinaten sollten genau ein Komma beinhalten z.B. '1.23, 4.56'"
        val lat = coords[0].trim().toDoubleOrNull()
        val longitude = coords[1].trim().toDoubleOrNull()
        if(lat==null) return null to "Gib ein validen Breitengrad ein z.B. 12.456"
        if(longitude==null) return null to "Gib ein validen Längengrad ein z.B. 12.456"
        val cityCountryMix = cityCountryInput.value?.split(", ") ?: return null to "Gib Koordinaten ein"
        if(cityCountryMix.size != 2) return null to "Stadt, Land sollte genau ein Komma enthalten"
        return Location(
            nameInput.value?:return null to "Gib einen Name ein",
            longitude,
            lat,
            radiusMInput.value?.trim()?.toIntOrNull()?:return null to "Gib ein Zahl als Radius ein z.B. 10",
            ssidInput.value,
            streetInput.value,
            numberInput.value,
            cityCountryMix[0].trim(),
            cityCountryMix[1].trim(),
            id.takeIf { it > 0 } ?: API.generateId()
        ) to null
    }
    fun setSheetLocation(newLocation: Location?){
        _sheetLocation.value = newLocation
        radiusMInput.value = newLocation?.radiusM?.toString()
        coordsInput.value = newLocation?.let { "%.6f, %.6f".format(it.lat, it.longitude) }
        selectedCoordinates.value = newLocation?.let { Point.fromLngLat(it.longitude, it.lat) }
    }
}