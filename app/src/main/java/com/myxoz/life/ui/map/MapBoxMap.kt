package com.myxoz.life.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import com.mapbox.annotation.MapboxExperimental
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
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.ui.map.MapRepo.Companion.circleRing
import com.myxoz.life.lifeui.theme.OldColors
import com.myxoz.life.background.settings.Settings
import kotlinx.coroutines.launch

@OptIn(MapboxExperimental::class)
@Composable
fun MapBoxMap(mapRepo: MapRepo, innerPadding: PaddingValues){
    val state = mapRepo.sheetState
    var mapBoxInitialRender by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val sheetHeight by remember {
        derivedStateOf {
            with(density) { state.height.toDp()
            }
        }
    }
    val snapHeight by remember {
        derivedStateOf {
            with(density) { state.snapHeightPx.toDp() }
        }
    }
    val shrunkArea = sheetHeight.coerceIn(0.dp, snapHeight)
    val coroutineScope = rememberCoroutineScope()
    MapboxMap(
        Modifier
            .fillMaxWidth(),
        mapViewportState = mapRepo.cameraOptions,
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
            coroutineScope.launch {
                mapRepo.clickedOn(it)
            }
            true
        },
        style = {
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
        val settings = LocalSettings.current
        val isLocationEnabled by settings.has(Settings.Feature.MapBoxLocation).collectAsState()
        MapEffect(Unit) {
            val pendingMapState = mapRepo.pendingMapState
            if(pendingMapState != null) mapRepo.cameraOptions.setCameraOptions(pendingMapState)
        }
        MapEffect(isLocationEnabled) {
            it.location.enabled = isLocationEnabled
            it.location.pulsingEnabled = isLocationEnabled
            it.location.puckBearingEnabled = true
            it.location.locationPuck = createDefault2DPuck(withBearing = true)
            it.location.puckBearing = PuckBearing.HEADING
            it.mapboxMap.subscribeCameraChangedCoalesced { state ->
                mapRepo.saveCameraPosition(state.cameraState)
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
        val allLocations by mapRepo.allLocations.collectAsState()
        val displayMarker by mapRepo.state.collectAsState()
        val displayedPolygon by remember { derivedStateOf {
            val stackItem = displayMarker
            (allLocations + listOfNotNull(displayMarker as? MapDisplayState.ResolvedLocation))
                .mapNotNull { loc ->
                    val asLifeLocation = loc as? MapDisplayState.DisplayedLifeLocation
                    val isDisplayedLifeLocation = (stackItem is MapDisplayState.DisplayedLifeLocation && asLifeLocation?.id == stackItem.id)
                    val isEditedLifeLocation = (stackItem is MapDisplayState.EditingLocation && stackItem.basedOnId != null && asLifeLocation?.id == stackItem.basedOnId)
                    val isAditionalLocation = loc is MapDisplayState.ResolvedLocation
                    val lat = (if(isEditedLifeLocation) stackItem.lat else loc.lat) ?: return@mapNotNull null
                    val longitude = (if(isEditedLifeLocation) stackItem.longitude else loc.longitude) ?: return@mapNotNull null
                    val radiusM = (if(isEditedLifeLocation) stackItem.radiusM else loc.radiusM) ?: return@mapNotNull null
                    val ring = circleRing(longitude, lat, radiusM.toDouble())
                    val polygon = PolygonAnnotationOptions().withPoints(listOf(ring))
                    if (isDisplayedLifeLocation || isEditedLifeLocation || isAditionalLocation) {
                        polygon.withFillColor(OldColors.SELECTED.copy(alpha = 0.5f).toArgb())
                    } else {
                        polygon.withFillColor(Color(0x80000000).toArgb())
                    }
                    polygon
                }
        } }
        PolygonAnnotationGroup(annotations = displayedPolygon)
        val marker = rememberIconImage(key = R.drawable.location_marker, painter = painterResource(R.drawable.location_marker))
        val pinPointableMarker = displayMarker as? PinPointable
        val longitude = pinPointableMarker?.longitude
        val latitude = pinPointableMarker?.lat
        if(pinPointableMarker != null && latitude != null && longitude != null)
            PointAnnotation(point = Point.fromLngLat(longitude, latitude)) {
                iconImage = marker
                iconSize = 1.0
                iconAnchor = IconAnchor.BOTTOM
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