package com.myxoz.life.viewmodels

import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraState
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.LocationSyncable
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.repositories.utils.subscribeToColdFlow
import com.myxoz.life.ui.ThreeStateBottomSheetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class MapViewModel(private val repos: AppRepositories): ViewModel() {
    private val _sheetLocation = MutableStateFlow<LocationSyncable?>(null)
    val sheetLocation = _sheetLocation.asStateFlow()
    val sheetState = ThreeStateBottomSheetState()
    val selectedCoordinates = MutableStateFlow<Point?>(null)
    val selectCoordsOnMap = MutableStateFlow(false)
    var lifeSearchResults = MutableStateFlow<List<LocationSyncable>?>(null)
    var mapBoxSearchResults = MutableStateFlow<List<LocationSyncable>?>(null)
    val decodedLocation = MutableStateFlow<LocationSyncable?>(null)
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
                zoom(repos.prefs.getFloat("map_zoom", 10f).toDouble())
                center(Point.fromLngLat(repos.prefs.getFloat("map_center_long", 9.9872f).toDouble(), repos.prefs.getFloat("map_center_lat", 53.5488f).toDouble()))
                pitch(repos.prefs.getFloat("map_pitch", 10f).toDouble())
                bearing(repos.prefs.getFloat("map_bearing", 10f).toDouble())
            }
        }
    }
    fun saveCameraPosition(state: CameraState){
        repos.prefs.edit {
            putFloat("map_zoom", state.zoom.toFloat())
            putFloat("map_center_long", state.center.longitude().toFloat())
            putFloat("map_center_lat", state.center.latitude().toFloat())
            putFloat("map_pitch", state.pitch.toFloat())
            putFloat("map_bearing", state.bearing.toFloat())
        }
    }
    fun setInputValuesByLocation(location: LocationSyncable){
        nameInput.value = location.name
        numberInput.value = location.number
        coordsInput.value = "%.6f, %.6f".format(location.lat, location.longitude)

        radiusMInput.value = location.radiusM.takeIf { it != 0 }?.toString()
        ssidInput.value = location.ssid
        streetInput.value = location.street
        cityCountryInput.value = "${location.city}, ${location.country}"
    }
    fun parseLocation(id: Long): Pair<LocationSyncable?, String?>{
        val coords = coordsInput.value?.split(",") ?: return null to "Gib Koordinaten ein"
        if(coords.size != 2) return null to "Die Koordinaten sollten genau ein Komma beinhalten z.B. '1.23, 4.56'"
        val lat = coords[0].trim().toDoubleOrNull()
        val longitude = coords[1].trim().toDoubleOrNull()
        if(lat==null) return null to "Gib ein validen Breitengrad ein z.B. 12.456"
        if(longitude==null) return null to "Gib ein validen Längengrad ein z.B. 12.456"
        val cityCountryMix = cityCountryInput.value?.split(", ") ?: return null to "Gib Koordinaten ein"
        if(cityCountryMix.size != 2) return null to "Stadt, Land sollte genau ein Komma enthalten"
        return LocationSyncable(
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
    fun setSheetLocation(newLocation: LocationSyncable?){
        _sheetLocation.value = newLocation
        radiusMInput.value = newLocation?.radiusM?.toString()
        coordsInput.value = newLocation?.let { "%.6f, %.6f".format(it.lat, it.longitude) }
        selectedCoordinates.value = newLocation?.let { Point.fromLngLat(it.longitude, it.lat) }
    }
    val getAllLocations = repos.locationRepo.getAllLocations().map {
        locations -> locations.map { it.data }
    }.subscribeToColdFlow(viewModelScope, listOf())
    suspend fun queryByCoordinate(lat: Double, long: Double) = repos.locationRepo.queryByCoordinate(lat, long)
    suspend fun saveAndSync(location: LocationSyncable) = repos.locationRepo.updateAndStage(location)
}