package com.myxoz.life.screens.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolygonAnnotationGroup
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.compose.style.standard.LightPresetValue
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.standard.ThemeValue
import com.mapbox.maps.extension.compose.style.standard.rememberStandardStyleState
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.myxoz.life.LocalSettings
import com.myxoz.life.LocalStorage
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.api.syncables.Location
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.viewmodels.MapViewModel
import kotlinx.coroutines.launch
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MapBoxMap(mapViewModel: MapViewModel, innerPadding: PaddingValues){
    var sheetHeight by remember { mutableStateOf(0.dp) }
    val state = mapViewModel.sheetState
    LaunchedEffect(Unit) {
        state.height.collect {
            sheetHeight = it
        }
    }
    val db = LocalStorage.current
    val sheetLocation by mapViewModel.sheetLocation.collectAsState()
    val selectedCoordinates by mapViewModel.selectedCoordinates.collectAsState()
    val selectCoordsOnMap by mapViewModel.selectCoordsOnMap.collectAsState()
    val isEditing by mapViewModel.isEditing.collectAsState()
    val refetchLocations by mapViewModel.refetchLocations.collectAsState()
    var allLocations by remember { mutableStateOf(listOf<Location>()) }
    val viewPortState = mapViewModel.cameraOptions
    var mapBoxInitialRender by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(refetchLocations) {
        allLocations = db.location.getAllLocations().map { Location.from(it) }
    }
    val snapHeight by state.snapHeight.collectAsState()
    val shrunkArea = sheetHeight.coerceIn(0.dp, snapHeight)
    MapboxMap(
        Modifier
            .fillMaxWidth(),
        mapViewportState = viewPortState,
        attribution = { Attribution(Modifier.padding(innerPadding).padding(bottom = shrunkArea)) },
        logo = { Logo(Modifier.padding(innerPadding).padding(bottom = shrunkArea)) },
        compass = { Compass(Modifier.padding(innerPadding)) },
        scaleBar = {
            ScaleBar(
                Modifier.padding(innerPadding),
                showTextBorder = false,
                textColor = OldColors.PRIMARYFONT,
            )
        },
        onMapClickListener = OnMapClickListener {
            if (isEditing) {
                if (selectCoordsOnMap) {
                    mapViewModel.selectedCoordinates.value = it
                }
            } else {
                coroutineScope.launch {
                    val dbLocation = db.location.queryByCoordinate(it.latitude(), it.longitude())
                        ?.let { Location.from(it) }
                    mapViewModel.setSheetLocation(dbLocation)
                    mapViewModel.selectedCoordinates.value =
                        dbLocation?.let { loc -> Point.fromLngLat(loc.longitude, loc.lat) } ?: it
                }
            }
            return@OnMapClickListener true
        },
        style = {
            @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
            MapboxStandardStyle(
                standardStyleState = rememberStandardStyleState {
                    configurationsState.apply {
                        theme = ThemeValue.FADED
                        lightPreset = LightPresetValue.NIGHT
                    }
                }
            )
        }
    ) {
        val features = LocalSettings.current
        val isLocationEnabled by features.features.mapBoxLocation.has.collectAsState()
        MapEffect(isLocationEnabled) {
            it.location.enabled = isLocationEnabled
            it.location.pulsingEnabled = isLocationEnabled
            it.location.puckBearingEnabled = true
            it.location.locationPuck = createDefault2DPuck(withBearing = true)
            it.location.puckBearing = PuckBearing.HEADING
            it.mapboxMap.subscribeCameraChangedCoalesced { state ->
                mapViewModel.saveCameraPosition(state.cameraState)
            }
        }
        val density = LocalDensity.current
        val shrunkAreaPx = with(density) { shrunkArea.toPx().toDouble() }

        MapEffect(shrunkAreaPx) { mapView ->
            val mapboxMap = mapView.mapboxMap
            val cs = mapboxMap.cameraState

            // Re-apply same center/zoom/etc, but with bottom padding equal to the sheet height.
            mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(cs.center)
                    .zoom(cs.zoom)
                    .bearing(cs.bearing)
                    .pitch(cs.pitch)
                    .padding(EdgeInsets(0.0, 0.0, shrunkAreaPx, 0.0))
                    .build()
            )
        }
        val selectedLoc by mapViewModel.sheetLocation.collectAsState()
        val selectedRadius by mapViewModel.radiusMInput.collectAsState()
        val selectedLocationInput by mapViewModel.coordsInput.collectAsState()
        val polygonOptions = remember(selectedLoc, selectedRadius, selectedLocationInput, allLocations) {
            val result = mutableListOf<PolygonAnnotationOptions>()
            allLocations
                .filter { loc -> loc.id != selectedLoc?.id }
                .forEach { loc ->
                    val radius = loc.radiusM
                    val ring = circleRing(loc.longitude,loc.lat,radius.toDouble())
                    result += PolygonAnnotationOptions()
                        .withPoints(listOf(ring))
                        .withFillColor(Color(0x80000000).toArgb())
                }

            val loc = selectedLoc
            if (loc != null) {
                val radius = selectedRadius?.toIntOrNull() ?: loc.radiusM
                val decodeCoordsFromInput = selectedLocationInput
                    ?.split(",")
                    ?.map { it.trim().toDoubleOrNull() }

                var lat = loc.lat
                var long = loc.longitude
                if (decodeCoordsFromInput?.size == 2) {
                    lat = decodeCoordsFromInput[0] ?: loc.lat
                    long = decodeCoordsFromInput[1] ?: loc.longitude
                }

                val ring = circleRing(
                    long,
                    lat,
                    radius.toDouble()
                )
                result += PolygonAnnotationOptions()
                    .withPoints(listOf(ring))
                    .withFillColor(OldColors.SELECTED.copy(alpha = 0.5f).toArgb())
            }
            result
        }

        PolygonAnnotationGroup(annotations = polygonOptions)
        val marker = rememberIconImage(key = R.drawable.location_marker, painter = painterResource(R.drawable.location_marker))
        selectedCoordinates?.let {
            PointAnnotation(point = it) {
                iconImage = marker
                iconSize = if(sheetLocation==null) 1.5 else 1.0
                iconAnchor = IconAnchor.BOTTOM
            }
        }
        mapBoxInitialRender = true
    }
    AnimatedVisibility(
        !mapBoxInitialRender,
        exit = fadeOut()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Theme.background)
        )
    }
}
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