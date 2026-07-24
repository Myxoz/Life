package com.myxoz.life.ui.map

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CameraState
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.myxoz.life.android.integration.MapBoxAPI
import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.LocationSyncable
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.utils.subscribeToColdFlow
import com.myxoz.life.ui.NavPath
import com.myxoz.life.ui.ThreeStateBottomSheetState
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.utils.def
import com.myxoz.life.utils.nullIfEmpty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin

class MapViewModel(
    handle: SavedStateHandle,
    private val repos: DatabaseInterface
): ViewModel() {
    val sheetState = ThreeStateBottomSheetState(ThreeStateBottomSheetState.SheetValue.Partial)

    var selectedCoordinates by mutableStateOf<Point?>(null)
    var selectingCoordsOnMapMode by mutableStateOf(false)
    var selectedLifeLocation by mutableStateOf<LocationSyncable?>(null)
        private set
    var allLocations by mutableStateOf(listOf<LocationSyncable>())
        private set
    var coordsInput by mutableStateOf("")
    var radiusMInput by mutableStateOf("")
    var cityCountryInput by mutableStateOf("")
    var editingLocation by mutableStateOf<LocationSyncable?>(null)
        private set

    val lifeQuery = MutableStateFlow<String?>(null)
    val lifeSearchResults = MutableStateFlow<List<LocationSyncable>?>(null)
    var mapBoxSearchResults = MutableStateFlow<List<LocationSyncable>?>(null)
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
    fun deselectLocation() {
        selectOrDeselectLifeLocation(null)
        selectedCoordinates = null
    }
    fun discardChanges() {
        editingLocation = null
        selectingCoordsOnMapMode = false
        selectOrDeselectLifeLocation(selectedLifeLocation)
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
    fun editWith(transform: LocationSyncable.()-> LocationSyncable) {
        editingLocation = editingLocation?.transform()
    }
    fun edit() {
        // val decodedLocationLoaded = selectedLifeLocation == null
        val lifeLocation = selectedLifeLocation ?: LocationSyncable(
            "",
            selectedCoordinates?.longitude() ?: return,
            selectedCoordinates?.latitude() ?: return,
            0,
            null,
            null,
            null,
            null,
            "",
            -1L
        )
        setSelectedLocation(lifeLocation)
        editingLocation = lifeLocation
        cityCountryInput = lifeLocation.city?.let { "$it, " }.orEmpty()+lifeLocation.country
        radiusMInput = lifeLocation.radiusM.takeIf { it != 0 }?.toString() ?: ""
    }
    fun saveAndSyncLocationOrError(): String? {
        val lifeLocation = selectedLifeLocation ?: return null
        val reason = parseLocation(lifeLocation.id)
        if(reason.second!=null) {
            return reason.second
        }
        val location = reason.first ?: return null
        viewModelScope.launch {
            saveAndSync(location)
            editingLocation = null
            setSelectedLocation(location)
        }
        return null
    }
    fun coordsToTuple(): Pair<Pair<Double, Double>?, String?> {
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
    private fun parseLocation(id: Long): Pair<LocationSyncable?, String?>{
        val editVersion = editingLocation ?: return null to "Bearbeite zuerst"
        val cityCountryMix = cityCountryInput.split(",")
        if(cityCountryMix.size !in 1..2) return null to "Stadt, Land sollte genau ein Komma enthalten"
        val (coords, returnString) = coordsToTuple()
        if(coords == null) return null to returnString
        val (lat, longitude) = coords
        return LocationSyncable(
            editVersion.name.nullIfEmpty() ?: return null to "Gib einen Name ein",
            longitude,
            lat,
            radiusMInput.toIntOrNull()?.takeIf { it != 0 } ?: return null to "Gib ein Zahl > 0 als Radius ein z.B. 10",
            editVersion.ssid?.nullIfEmpty(),
            editVersion.street?.nullIfEmpty(),
            editVersion.number?.nullIfEmpty(),
            if(cityCountryMix.size == 2) cityCountryMix[0].trim() else null,
            cityCountryMix[if(cityCountryMix.size == 1) 0 else 1].trim(),
            id.takeIf { it > 0 } ?: API.generateId()
        ) to null
    }
    private fun setSelectedCoords(lat: Double, longitude: Double){
        coordsInput = "%.6f, %.6f".format(lat, longitude)
        selectedCoordinates = Point.fromLngLat(longitude, lat)
    }
    private fun setSelectedLocation(newLocation: LocationSyncable) {
        selectedLifeLocation = newLocation
        setSelectedCoords(newLocation.lat, newLocation.longitude)
    }
    fun selectOrDeselectLifeLocation(lifeLocation: LocationSyncable?) {
        if(lifeLocation != null)
            setSelectedLocation(lifeLocation)
        else
            selectedLifeLocation = null
    }
    val getAllLocations = repos.locationInterface.getAllLocations.subscribeToColdFlow(viewModelScope, listOf())
    suspend fun queryByCoordinate(lat: Double, long: Double) = repos.locationInterface.queryByCoordinate(lat, long)
    suspend fun saveAndSync(location: LocationSyncable) = repos.locationInterface.updateAndStage(location)
    fun clickedOn(point: Point) {
        if (editingLocation != null) {
            if (selectingCoordsOnMapMode) {
                setSelectedCoords(point.latitude(), point.longitude())
                editWith {
                    copy(
                        lat = point.latitude(),
                        longitude = point.longitude()
                    )
                }
            }
        } else {
            viewModelScope.launch {
                val dbLocation = queryByCoordinate(point.latitude(), point.longitude())
                if(dbLocation == null) {
                    selectedCoordinates = point
                }
                selectOrDeselectLifeLocation(dbLocation)
            }
        }
    }

    fun loadSearchResults() {
        val query = lifeQuery.value
        if(query.isNullOrBlank()) return
        viewModelScope.launch {
            val resp = MapBoxAPI.getLocationListFromAPIResponse(MapBoxAPI.forwardGeocode(repos.context, query) ?: return@launch)
            mapBoxSearchResults.value = resp
        }
    }

    val displayedPolygon by derivedStateOf {
        (allLocations + listOfNotNull(if(allLocations.none { it.id == selectedLifeLocation?.id }) selectedLifeLocation else null))
            .map { loc ->
                val isEditingLocation = loc.id == selectedLifeLocation?.id
                val location = if (isEditingLocation) editingLocation ?: loc else loc
                val ring = circleRing(location.longitude,location.lat, location.radiusM.toDouble())
                if (isEditingLocation) {
                    PolygonAnnotationOptions()
                        .withPoints(listOf(ring))
                        .withFillColor(OldColors.SELECTED.copy(alpha = 0.5f).toArgb())
                } else {
                    PolygonAnnotationOptions()
                        .withPoints(listOf(ring))
                        .withFillColor(Color(0x80000000).toArgb())
                }
            }
    }

    init {
        val paramLocation = handle.get<String>(NavPath.Menu.MAP.parameterName)?.toLongOrNull()
        if(paramLocation != null) {
            viewModelScope.launch {
                val location = repos.locationInterface.getLocationById(paramLocation).first() ?: return@launch
                val targetMetersOnScreen = 2 * location.radiusM.takeIf { it != 0 }.def(10) / .002f
                val metersPerPixel = targetMetersOnScreen / 300 // Screenwidth estimation
                val zoom = ln(EARTH_R * cos(Math.toRadians(location.lat)) / metersPerPixel) / ln(2.0)
                cameraOptions.setCameraOptions(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(location.longitude, location.lat))
                        .zoom(zoom.coerceIn(0.0, 22.0))
                        .bearing(0.0)
                        .pitch(0.0)
                        .build()
                )
            }
        }
        viewModelScope.launch {
            repos.locationInterface.getAllLocations.collect {
                allLocations = it
            }
        }
    }
    companion object {
        const val EARTH_R = 6_371_000.0 // meters
        private fun circleRing(
            centerLon: Double,
            centerLat: Double,
            radiusM: Double,
            steps: Int = 20
        ): List<Point> {
            val fi1 = Math.toRadians(centerLat)
            val lamda1 = Math.toRadians(centerLon)
            val s = radiusM / EARTH_R

            val ring = ArrayList<Point>(steps + 1)

            for (i in 0 until steps) {
                val theta = 2.0 * Math.PI * i / steps // bearing in radians

                val sinfi2 = sin(fi1) * cos(s) +
                        cos(fi1) * sin(s) * cos(theta)
                val fi2 = asin(sinfi2)

                val y = sin(theta) * sin(s) * cos(fi1)
                val x = cos(s) - sin(fi1) * sin(fi2)
                val lambda2 = lamda1 + atan2(y, x)

                ring += Point.fromLngLat(Math.toDegrees(lambda2), Math.toDegrees(fi2))
            }

            ring += ring.first() // close ring
            return ring
        }
    }
}