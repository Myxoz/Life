package com.myxoz.life.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxExperimental
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
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalSettings
import com.myxoz.life.LocalStorage
import com.myxoz.life.R
import com.myxoz.life.api.syncables.Location
import com.myxoz.life.api.jsonObjArray
import com.myxoz.life.android.integration.MapBoxAPI
import com.myxoz.life.screens.person.displayperson.ListEditingField
import com.myxoz.life.screens.person.displayperson.ListEntry
import com.myxoz.life.screens.person.displayperson.navigateForResult
import com.myxoz.life.ui.ThreeStateBottomSheet
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.MaterialShapes
import com.myxoz.life.utils.filteredWith
import com.myxoz.life.utils.getJSONObjectOrNull
import com.myxoz.life.utils.getStringOrNull
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.utils.toShape
import com.myxoz.life.utils.transformed
import com.myxoz.life.viewmodels.MapViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@OptIn(MapboxExperimental::class, ExperimentalMaterial3Api::class,
    com.mapbox.annotation.MapboxExperimental::class
)
@Composable
fun MapBoxMap(mapViewModel: MapViewModel){
    val nav = LocalNavController.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    var sheetHeight by remember { mutableStateOf(0.dp) }
    var selectCoordsOnMap by remember { mutableStateOf(false) }
    val sheetLocation by mapViewModel.sheetLocation.collectAsState()
    val selectedCoordinates by mapViewModel.selectedCoordinates.collectAsState()
    val isEditing by mapViewModel.isEditing.collectAsState()
    val state = mapViewModel.sheetState
    val minSheetHeight = FontSize.XLARGE.size.toDp() + 30.dp * 2
    var refetchLocations by remember { mutableLongStateOf(System.currentTimeMillis()) }
    Scaffold(
        containerColor = Colors.BACKGROUND
    ) { innerPadding ->
        val snapHeight by state.snapHeight.collectAsState()
        val db = LocalStorage.current
        var allLocations by remember { mutableStateOf(listOf<Location>()) }
        val viewPortState = mapViewModel.cameraOptions
        var mapBoxInitialRender by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        LaunchedEffect(refetchLocations) {
            allLocations = db.location.getAllLocations().map { Location.from(it) }
        }
        val shrunkArea = sheetHeight.coerceIn(0.dp, snapHeight)
        MapboxMap(
            Modifier
                .fillMaxWidth(),
            mapViewportState = viewPortState,
            attribution = { Attribution(Modifier.padding(innerPadding).padding(bottom = shrunkArea)) },
            logo = { Logo(Modifier.padding(innerPadding).padding(bottom = shrunkArea)) },
            compass = { Compass(Modifier.padding(innerPadding)) },
            scaleBar = { ScaleBar(
                Modifier.padding(innerPadding),
                showTextBorder = false,
                textColor = Colors.PRIMARYFONT,
            ) },
            onMapClickListener = OnMapClickListener {
                if(isEditing) {
                    if(selectCoordsOnMap) {
                        mapViewModel.selectedCoordinates.value = it
                    }
                } else {
                    coroutineScope.launch {
                        val dbLocation = db.location.queryByCoordinate(it.latitude(), it.longitude())?.let { Location.from(it) }
                        mapViewModel.setSheetLocation(dbLocation)
                        mapViewModel.selectedCoordinates.value = dbLocation?.let{ loc -> Point.fromLngLat(loc.longitude, loc.lat)} ?: it
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
                        .withFillColor(Colors.SELECTED.copy(alpha = 0.5f).toArgb())
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
                    .background(Colors.SECONDARY)
            )
        }
        ThreeStateBottomSheet(
            state,
            minSheetHeight,
            Colors.BACKGROUND,
            innerPadding
        ) {
            LaunchedEffect(Unit) {
                state.height.collect {
                    sheetHeight = it
                }
            }
            BackHandler(sheetLocation!=null || selectedCoordinates != null) {
                mapViewModel.setSheetLocation(null)
                mapViewModel.decodedLocation.value = null
                selectCoordsOnMap = false
                mapViewModel.isEditing.value = false
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                val progress by state.progress.collectAsState()
                val loc = sheetLocation
                val decodedLocation by mapViewModel.decodedLocation.collectAsState()
                val coords = selectedCoordinates
                val iconSize = FontSize.XLARGE.size.toDp()
                val context = LocalContext.current
                if (coords != null) {
                    LaunchedEffect(coords) {
                        if(decodedLocation != null || loc != null) return@LaunchedEffect
                        while (true) {
                            if(progress < 0.1f) {
                                delay(100)
                                continue
                            }
                            val response = MapBoxAPI.reverseGeocode(context, coords.latitude(), coords.longitude()) ?: return@LaunchedEffect
                            val features = JSONObject(response)
                                .getJSONArray("features")
                                .jsonObjArray
                            if(features.isEmpty()) return@LaunchedEffect
                            val searchedItem = features[0].getJSONObjectOrNull("properties")?:return@LaunchedEffect
                            val details = searchedItem.getJSONObjectOrNull("context")?:return@LaunchedEffect
                            mapViewModel.decodedLocation.value = Location(
                                searchedItem.getStringOrNull("name_preferred")?:"Name",
                                coords.longitude(),
                                coords.latitude(),
                                0,
                                null,
                                details.getJSONObjectOrNull("street")?.getStringOrNull("name"),
                                details.getJSONObjectOrNull("address")?.getStringOrNull("address_number"),
                                details.getJSONObjectOrNull("place")?.getStringOrNull("name"),
                                details.getJSONObjectOrNull("country")?.getStringOrNull("name")?:"Germany",
                                -1,
                            )
                            break
                        }
                    }
                    val display = loc?.name ?: decodedLocation?.name ?: "Markierung"
                    val subTitle = loc?.toAddress() ?: decodedLocation?.let { "In der Nähe von ${it.toAddress()}" } ?: "Lädt..."
                    Spacer(Modifier.height(20.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                        ,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val nameInput by mapViewModel.nameInput.collectAsState()
                        BasicTextField(
                            if(isEditing) nameInput?:"" else display,
                            {
                                mapViewModel.nameInput.value = it
                            },
                            Modifier
                                .padding(vertical = 5.dp)
                                .weight(1f)
                            ,
                            cursorBrush = SolidColor(Colors.PRIMARYFONT),
                            enabled = isEditing && progress > 0.1f,
                            textStyle = TypoStyle(FontColor.PRIMARY, FontSize.XLARGE, FontFamily.Display),
                            singleLine = true,
                        )
                        val morph = remember {
                            Morph(MaterialShapes.Cookie12Sided, MaterialShapes.Ghostish.transformed(Matrix().apply { rotateZ(90f) }))
                        }
                        val editingAnimationProgress by animateFloatAsState(if(isEditing) 1f else 0f)
                        val path = remember(editingAnimationProgress) {
                            morph.toPath(editingAnimationProgress).asComposePath()
                        }
                        val shape = path.toShape()
                        Box(
                            Modifier.width(iconSize+10.dp + editingAnimationProgress*(iconSize+10.dp)),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            val discardShape = MaterialShapes.Ghostish.transformed(Matrix().apply { rotateZ(-90f) }).toShape()
                            fun discard(){
                                if((loc?.id ?: -1) < 1) { // Discard unsynced location
                                    mapViewModel.decodedLocation.value = loc
                                    mapViewModel.setSheetLocation(null)
                                } else {
                                    mapViewModel.decodedLocation.value = null
                                    mapViewModel.setSheetLocation(loc)
                                }
                                selectCoordsOnMap = false
                                mapViewModel.isEditing.value = false
                            }
                            BackHandler(isEditing){
                                discard()
                            }
                            Box(
                                Modifier
                                    .offset(x = -editingAnimationProgress*(iconSize+10.dp))
                                    .alpha(editingAnimationProgress)
                                    .size(iconSize+10.dp)
                                    .background(Colors.TERTIARY, discardShape)
                                    .clip(discardShape)
                                    .rippleClick{
                                        discard()
                                    }
                                    .padding(10.dp)
                            ) {
                                Icon(painterResource(R.drawable.close), "Discard", Modifier.fillMaxSize(), Colors.PRIMARYFONT)
                            }
                            Box(
                                Modifier
                                    .size(iconSize+10.dp)
                                    .background(Colors.TERTIARY, shape)
                                    .clip(shape)
                                    .rippleClick{
                                        if(loc == null) { // Add new Location
                                            mapViewModel.setSheetLocation(decodedLocation)
                                            mapViewModel.decodedLocation.value = null
                                            mapViewModel.isEditing.value = true
                                            mapViewModel.setInputValuesByLocation(decodedLocation?:return@rippleClick)
                                        } else {
                                            if(!isEditing) { // Edit known location
                                                mapViewModel.setInputValuesByLocation(loc)
                                                mapViewModel.setSheetLocation(loc)
                                                mapViewModel.decodedLocation.value = null
                                                mapViewModel.isEditing.value = true
                                            } else { // Save already edited location
                                                val reason = mapViewModel.parseLocation(loc.id)
                                                if(reason.second!=null) {
                                                    Toast.makeText(context, reason.second, Toast.LENGTH_LONG).show()
                                                    return@rippleClick
                                                }
                                                coroutineScope.launch {
                                                    (reason.first?:return@launch).saveAndSync(db)
                                                    mapViewModel.isEditing.value = false
                                                    mapViewModel.setSheetLocation(reason.first)
                                                    refetchLocations = System.currentTimeMillis()
                                                }
                                            }
                                        }
                                    }
                                    .padding(10.dp)
                            ) {
                                androidx.compose.animation.AnimatedVisibility(
                                    loc == null,
                                    enter = scaleIn() + fadeIn(),
                                    exit = scaleOut() + fadeOut(),
                                ) {
                                    Icon(Icons.Rounded.Add, "Add", Modifier.fillMaxSize(), Colors.PRIMARYFONT)
                                }
                                androidx.compose.animation.AnimatedVisibility(
                                    loc != null && !isEditing,
                                    enter = scaleIn() + fadeIn(),
                                    exit = scaleOut() + fadeOut(),
                                ) {
                                    Icon(painterResource(R.drawable.edit), "Edit", Modifier.fillMaxSize(), Colors.PRIMARYFONT)
                                }
                                androidx.compose.animation.AnimatedVisibility(
                                    loc != null && isEditing,
                                    enter = scaleIn() + fadeIn(),
                                    exit = scaleOut() + fadeOut(),
                                ) {
                                    Icon(painterResource(R.drawable.tick), "Save", Modifier.fillMaxSize(), Colors.PRIMARYFONT)
                                }
                            }
                        }
                    }
                    Text(
                        subTitle,
                        Modifier
                            .offset(y = 20.dp*(1-progress))
                            .alpha(if(isEditing) 0f else progress),
                        style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUMM),
                    )
                    Spacer(Modifier.height(20.dp))
                    val mixedLocation = loc ?: decodedLocation
                    Row(
                        Modifier
                            .fillMaxWidth()
                        ,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .weight(1f),
                        ) {
                            ListEntry(
                                "Koordinaten",
                                painterResource(R.drawable.location)
                            ) {
                                ListEditingField(
                                    isEditing,
                                    "%.6f, %.6f".format(coords.latitude(), coords.longitude()),
                                    Location.coordsToDMS(coords.latitude(), coords.longitude()),
                                    mapViewModel.coordsInput,
                                    "Koordinaten"
                                )
                            }
                        }
                        LaunchedEffect(Unit) {
                            mapViewModel.selectedCoordinates.collect {
                                if(it==null) return@collect
                                mapViewModel.coordsInput.value = "%.6f, %.6f".format(it.latitude(), it.longitude())
                            }
                        }
                        if(isEditing)
                            Box(
                                Modifier
                                    .size(iconSize + 10.dp)
                                    .background(Colors.SECONDARY, CircleShape)
                                    .clip(CircleShape)
                                    .rippleClick{
                                        selectCoordsOnMap = !selectCoordsOnMap
                                    }
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    painterResource(R.drawable.pick),
                                    "Pick",
                                    Modifier.fillMaxSize(),
                                    if(selectCoordsOnMap) Colors.SELECTED else Colors.PRIMARYFONT
                                )
                            }
                    }
                    AnimatedVisibility(
                        loc != null
                    ) {
                        ListEntry(
                            "Radius in Metern",
                            painterResource(R.drawable.radius)
                        ) {
                            ListEditingField(
                                isEditing,
                                loc?.radiusM?.toString()?:"",
                                null,
                                mapViewModel.radiusMInput,
                                "Radius"
                            )
                        }
                    }
                    ListEntry(
                        "Hausnummer",
                        painterResource(R.drawable.house)
                    ) {
                        ListEditingField(
                            isEditing,
                            mixedLocation?.number ?: "???",
                            null,
                            mapViewModel.numberInput,
                            "Nummer"
                        )
                    }
                    ListEntry(
                        "Straße",
                        painterResource(R.drawable.road)
                    ) {
                        ListEditingField(
                            isEditing,
                            mixedLocation?.street ?: "???",
                            null,
                            mapViewModel.streetInput,
                            "Straße"
                        )
                    }
                    ListEntry(
                        "Ort, Land",
                        painterResource(R.drawable.globe)
                    ) {
                        ListEditingField(
                            isEditing,
                            if(mixedLocation != null) (mixedLocation.city?.let { "$it, " } ?: "") + mixedLocation.country else "Lädt...",
                            null,
                            mapViewModel.cityCountryInput,
                            "Ort, Land"
                        )
                    }
                    AnimatedVisibility(
                        loc != null
                    ) {
                        ListEntry(
                            "SSID",
                            painterResource(R.drawable.wifi)
                        ) {
                            ListEditingField(
                                isEditing,
                                loc?.ssid?:"???",
                                null,
                                mapViewModel.ssidInput,
                                "WLAN-Name"
                            )
                        }
                    }
                } else { // No location selected: Searchox
                    val screenHeightPx = with(LocalDensity.current) { screenHeight.toPx()  }
                    val mapBoxSearchResults by mapViewModel.mapBoxSearchResults.collectAsState()
                    val lifeSearchResults by mapViewModel.lifeSearchResults.collectAsState()
                    var lifeQueryValue by remember {
                        mutableStateOf(
                            TextFieldValue(
                                text = mapViewModel.lifeQuery.value ?: "",
                                selection = TextRange((mapViewModel.lifeQuery.value ?: "").length)
                            )
                        )
                    }
                    LaunchedEffect(lifeQueryValue.text) {
                        val query = lifeQueryValue.text.takeIf { it.isNotBlank() } ?: return@LaunchedEffect Unit.apply {
                            mapViewModel.lifeSearchResults.value = null
                        }
                        val locations = db.location.getAllLocations().map { Location.from(it) }
                        mapViewModel.lifeSearchResults.value = locations.filteredWith(query, {
                            it.toAddress()
                        }){
                            it.name
                        }.take(5)
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 15.dp)
                        ,
                        contentAlignment = Alignment.Center
                    ) {
                        var hasFocus by remember { mutableStateOf(false) }
                        val focusManager = LocalFocusManager.current
                        val focusRequester = remember { FocusRequester() }
                        val density = LocalDensity.current
                        BackHandler(lifeQueryValue.text.isNotEmpty()) {
                            focusManager.clearFocus()
                            mapViewModel.lifeQuery.value = null
                            lifeQueryValue = TextFieldValue("")
                        }
                        BasicTextField(
                            lifeQueryValue,
                            {
                                lifeQueryValue = it
                                mapViewModel.lifeQuery.value = it.text
                            },
                            Modifier
                                .padding(vertical = 5.dp)
                                .background(Colors.SECONDARY, RoundedCornerShape(10.dp))
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .focusRequester(focusRequester)
                                .onFocusChanged {
                                    hasFocus = it.hasFocus || it.isFocused || it.isCaptured
                                }
                                .rippleClick{
                                    coroutineScope.launch {
                                        state.expandTo(1, screenHeightPx.toInt(), with(density) {minSheetHeight.toPx()})
                                        val text = mapViewModel.lifeQuery.value ?: ""
                                        lifeQueryValue = lifeQueryValue.copy(
                                            text = text,
                                            selection = TextRange(text.length)
                                        )
                                        focusRequester.requestFocus()
                                    }
                                }
                                .padding(horizontal = 15.dp, vertical = 10.dp)
                            ,
                            textStyle = TypoStyle(FontColor.PRIMARY, FontSize.LARGE),
                            cursorBrush = SolidColor(Colors.PRIMARYFONT),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            enabled = progress > 0.1f
                        ) { innerTextField ->
                            if(hasFocus || mapViewModel.lifeQuery.value!=null) {
                                innerTextField()
                            } else{
                                Text(
                                    "Suchen",
                                    style = TypoStyle(FontColor.SECONDARY, FontSize.LARGE),
                                )
                            }
                        }
                    }
                    val screenWidthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
                    fun selectLocation(location: Location, newComposition: Boolean){
                        mapViewModel.setSheetLocation(location)
                        val targetMetersOnScreen = 2 * location.radiusM / .002f // TODO TWEAK

                        val metersPerPixel = targetMetersOnScreen / screenWidthPx
                        val zoom = ln(EARTH_R * cos(Math.toRadians(location.lat)) / metersPerPixel) / ln(2.0)
                        mapViewModel.viewModelScope.launch {
                            snapshotFlow { mapViewModel.cameraOptions.cameraState }
                                .filterNotNull()
                                .let { if(newComposition) it.drop(1) else it }
                                .first()
                            mapViewModel.cameraOptions.flyTo(
                                cameraOptions = CameraOptions.Builder()
                                    .center(Point.fromLngLat(location.longitude, location.lat))
                                    .zoom(zoom.coerceIn(0.0, 22.0))
                                    .bearing(0.0)
                                    .pitch(0.0)
                                    .build()
                            )
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .offset(y = (1-progress)*10.dp)
                        ,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Life Search",
                            style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUMM)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                "Alle",
                                Modifier
                                    .rippleClick{
                                        nav.navigateForResult<String?>(
                                            "pick/existing/location",
                                            "pelocation",
                                            {
                                                setOrRemove("pequery", mapViewModel.lifeQuery.value)
                                            }
                                        ) {
                                            selectLocation(Location.fromJSON(JSONObject(it?:return@navigateForResult)), true)
                                        }
                                    }
                                ,
                                style = TypoStyle(FontColor.TERTIARY, FontSize.MEDIUMM)
                            )
                            Icon(painterResource(R.drawable.arrow_right), "All", Modifier.size(FontSize.MEDIUMM.size.toDp()), Colors.TERTIARYFONT)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .then(
                                if(lifeSearchResults.isNullOrEmpty())
                                    Modifier.border(2.dp, Colors.SECONDARY, RoundedCornerShape(15.dp))
                                else
                                    Modifier.background(Colors.SECONDARY, RoundedCornerShape(15.dp))
                            )
                    ) {
                        if(lifeSearchResults.isNullOrEmpty()) {
                            Text(
                                if(lifeSearchResults==null) "Suche etwas" else "Keine Ergebnisse",
                                Modifier.padding(vertical = 15.dp).fillMaxWidth(),
                                style = TypoStyle(FontColor.SECONDARY, FontSize.LARGE).copy(fontStyle = FontStyle.Italic),
                                textAlign = TextAlign.Center
                            )
                        }
                        lifeSearchResults?.forEach {
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(15.dp))
                                    .fillMaxWidth()
                                    .rippleClick{
                                        selectLocation(it, false)
                                    }
                                    .padding(horizontal = 15.dp, vertical = 15.dp)
                                ,
                                horizontalArrangement = Arrangement.spacedBy(15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painterResource(R.drawable.location),
                                    "Location",
                                    Modifier.size(20.dp),
                                    Colors.PRIMARYFONT
                                )
                                Column{
                                    Text(
                                        it.name,
                                        style = TypoStyle(FontColor.PRIMARY, FontSize.LARGE)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        it.toAddress(false),
                                        style = TypoStyle(FontColor.SECONDARY, FontSize.SMALLM)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
private const val EARTH_R = 6_371_000.0 // meters
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