package com.myxoz.life.ui.map

import androidx.compose.runtime.snapshotFlow
import androidx.core.content.edit
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CameraState
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.myxoz.life.android.integration.MapBoxAPI
import com.myxoz.life.api.syncables.LocationSyncable
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.utils.subscribeToColdFlow
import com.myxoz.life.ui.ThreeStateBottomSheetState
import com.myxoz.life.utils.def
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin

class MapRepo(
    private val repos: DatabaseInterface
) {
    val sheetState = ThreeStateBottomSheetState(ThreeStateBottomSheetState.SheetValue.Partial)
    val allLocations = repos.locationInterface.getAllLocations.map{ it.map{MapDisplayState.DisplayedLifeLocation(it)} }
        .subscribeToColdFlow(repos.appScope, listOf())

    val dbInterface = repos.locationInterface
    private val stack = MutableStateFlow<List<MapDisplayState>>(listOf(MapDisplayState.SearchPage(null)))
    val state = stack.map(List<MapDisplayState>::last).subscribeToColdFlow(repos.appScope, MapDisplayState.SearchPage(null))
    fun popStack() {
        stack.update {
            if(it.size > 1) it.dropLast(1) else it
        }
    }
    fun pushToStack(item: MapDisplayState){
        stack.update { it + item }
    }
    var keepUpdatedJob: Job? = null
    fun keepUpdatedIfNeeded(newStackItem: MapDisplayState){
        keepUpdatedJob?.cancel()
        if(newStackItem is MapNeedsUpdates) {
             keepUpdatedJob = repos.appScope.launch {
                newStackItem.keepUpdated(dbInterface)
            }
        }
    }
    fun updateLastStackItem(item: MapDisplayState){
        stack.update {
            val isPopable = it.size > 1
            if(isPopable) it.toMutableList().apply { set(size-1, item) } else it
        }
    }

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
    fun saveCameraPosition(state: CameraState) {
        repos.prefs.edit {
            putFloat("map_zoom", state.zoom.toFloat())
            putFloat("map_center_long", state.center.longitude().toFloat())
            putFloat("map_center_lat", state.center.latitude().toFloat())
            putFloat("map_pitch", state.pitch.toFloat())
            putFloat("map_bearing", state.bearing.toFloat())
        }
    }
    var pendingMapState: CameraOptions? = null
    fun focusLocation(location: LocationDisplayable) {
        val stackItem = location as? MapDisplayState.DisplayedLifeLocation ?: location as? MapDisplayState.ResolvedLocation
        if(stackItem != null) pushToStack(stackItem)

        val targetMetersOnScreen = 2 * location.radiusM.takeIf { it != 0 }.def(10) / .002f
        val metersPerPixel = targetMetersOnScreen / getScreenWidthSynchronously()
        val lat = location.lat ?: return
        val longitude = location.longitude ?: return
        val zoom = ln(EARTH_R * cos(Math.toRadians(lat)) / metersPerPixel) / ln(2.0)
        println("Trying to focus: $lat, $longitude")
        val shouldCameraState = CameraOptions.Builder().apply {
            center(Point.fromLngLat(longitude, lat))
            zoom(zoom.coerceIn(0.0, 22.0))
            bearing(0.0)
            pitch(0.0)
        }.build()
        if(cameraOptions.cameraState != null)
            pendingMapState = shouldCameraState
        else
            cameraOptions.setCameraOptions(shouldCameraState)
    }

    suspend fun animateSelectLocation(location: LocationDisplayable, newComposition: Boolean = false) {
        val stackItem = location as? MapDisplayState.DisplayedLifeLocation ?: location as? MapDisplayState.ResolvedLocation
        if(stackItem != null) pushToStack(stackItem)

        val targetMetersOnScreen = 2 * location.radiusM.takeIf { it != 0 }.def(10) / .002f
        val metersPerPixel = targetMetersOnScreen / getScreenWidthSynchronously()
        val lat = location.lat ?: return
        val longitude = location.longitude  ?: return
        val zoom = ln(EARTH_R * cos(Math.toRadians(lat)) / metersPerPixel) / ln(2.0)
        snapshotFlow { cameraOptions.cameraState }
            .filterNotNull()
            .let { if(newComposition) it.drop(1) else it }
            .first()
        cameraOptions.flyTo(
            cameraOptions = CameraOptions.Builder()
                .center(Point.fromLngLat(longitude, lat))
                .zoom(zoom.coerceIn(0.0, 22.0))
                .bearing(0.0)
                .pitch(0.0)
                .build()
        )
    }
    fun getScreenWidthSynchronously(): Float {
        return repos.context.resources.displayMetrics.widthPixels.toFloat()
    }
    suspend fun queryByCoordinate(lat: Double, long: Double, cached: List<LocationSyncable>?) =
        repos.locationInterface.queryByCoordinate(lat, long, cached)
    suspend fun saveAndStage(location: LocationSyncable) = repos.locationInterface.updateAndStage(location)
    suspend fun saveEdited(location: MapDisplayState.EditingLocation): String? {
        val (location, error) = location.convertToLocationSyncable()
        if(location == null) return error
        popStack()
        saveAndStage(location)
        updateLastStackItem(MapDisplayState.DisplayedLifeLocation(location))
        return error
    }
    suspend fun clickedOn(point: Point) {
        val currentStackItem = state.value
        val asEditing = currentStackItem as? MapDisplayState.EditingLocation
        if(asEditing != null) return asEditing.handleMapClick(point.latitude(), point.longitude())
        val lifeLocation = queryByCoordinate(point.latitude(), point.longitude(), allLocations.value.map { it.location })
        val update = currentStackItem is MapDisplayState.PinNotResolved || currentStackItem is MapDisplayState.ResolvedLocation
        val newStackItem = if(lifeLocation != null)
            MapDisplayState.DisplayedLifeLocation(lifeLocation)
        else
            MapDisplayState.PinNotResolved(point.latitude(), point.longitude())
        if(update) updateLastStackItem(newStackItem) else pushToStack(newStackItem)
    }

    suspend fun loadSearchResults(query: String): List<MapDisplayState.ResolvedLocation>? {
        if(query.isBlank()) return null
        return MapBoxAPI.getLocationListFromAPIResponse(MapBoxAPI.forwardGeocode(repos.context, query) ?: return null)
    }

    suspend fun openAtPage(page: MapDisplayState) {
        pushToStack(page)
        if(page is LocationDisplayable) animateSelectLocation(page, true)
    }
    init {
        repos.appScope.launch {
            state.collect {
                keepUpdatedIfNeeded(it)
            }
        }
    }


    companion object {
        const val EARTH_R = 6_371_000.0 // meters
        fun circleRing(
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