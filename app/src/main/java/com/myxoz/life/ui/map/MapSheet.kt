package com.myxoz.life.ui.map

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.myxoz.life.LocalNavController
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.android.integration.MapBoxAPI
import com.myxoz.life.api.syncables.LocationSyncable
import com.myxoz.life.ui.EditToTickAndDiscard
import com.myxoz.life.ui.NavPath
import com.myxoz.life.ui.ThreeStateBottomSheetState
import com.myxoz.life.ui.ThreeStateBottomSheetState.Companion.ThreeStateBottomSheet
import com.myxoz.life.ui.map.MapViewModel.Companion.EARTH_R
import com.myxoz.life.ui.person.displayperson.ListEditingField
import com.myxoz.life.ui.person.displayperson.ListEntry
import com.myxoz.life.ui.person.displayperson.navigateForResult
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.def
import com.myxoz.life.utils.filteredWith
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.ln

@Composable
fun MapSheet(mapViewModel: MapViewModel, innerPadding: PaddingValues) {
    val state = mapViewModel.sheetState
    val minSheetHeight = FontSize.XLARGE.size.toDp() + 30.dp * 2
    ThreeStateBottomSheet(
        state,
        minSheetHeight,
        Theme.surfaceContainer,
        innerPadding
    ) {
        BackHandler(mapViewModel.selectedLifeLocation!=null || mapViewModel.selectedCoordinates != null) {
            mapViewModel.deselectLocation()
        }
        BackHandler(mapViewModel.editingLocation != null) {
            mapViewModel.discardChanges()
        }
        BackHandler(mapViewModel.selectingCoordsOnMapMode) {
            mapViewModel.selectingCoordsOnMapMode = false
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
            ,
        ) {
            val iconSize = FontSize.XLARGE.size.toDp()
            val context = LocalContext.current
            val selectedCoordinates = mapViewModel.selectedCoordinates
            if(selectedCoordinates != null) {
                val isNotCollapsed = mapViewModel.sheetState.state != ThreeStateBottomSheetState.SheetValue.Collapsed
                LaunchedEffect(selectedCoordinates, mapViewModel.selectedLifeLocation, isNotCollapsed) {
                    if(!isNotCollapsed || mapViewModel.selectedLifeLocation != null) return@LaunchedEffect
                    val response = MapBoxAPI.getLocationListFromAPIResponse(MapBoxAPI.reverseGeocode(context, selectedCoordinates.latitude(), selectedCoordinates.longitude()) ?: return@LaunchedEffect)
                    mapViewModel.selectOrDeselectLifeLocation(response.getOrNull(0)?.let {
                        LocationSyncable(it.name,
                            selectedCoordinates.longitude(),
                            selectedCoordinates.latitude(),
                            it.radiusM, it.ssid, it.street, it.number, it.city, it.country, it.id)
                    } ?: return@LaunchedEffect)
                }
                val display = mapViewModel.selectedLifeLocation?.name ?: "Markierung"
                val subTitle = mapViewModel.selectedLifeLocation?.toAddress() ?: "Lädt..."
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                    ,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val focusManager = LocalFocusManager.current
                    BasicTextField(
                        mapViewModel.editingLocation?.name ?: display,
                        {
                            mapViewModel.editWith {
                                copy(name = it)
                            }
                        },
                        Modifier
                            .padding(vertical = 5.dp)
                            .weight(1f)
                        ,
                        cursorBrush = SolidColor(Theme.primary),
                        enabled = mapViewModel.editingLocation != null && isNotCollapsed,
                        textStyle = TypoStyle(Theme.primary, FontSize.XLARGE, FontFamily.Display),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions{
                            focusManager.clearFocus()
                        },
                        singleLine = true,
                    )
                    EditToTickAndDiscard(
                        mapViewModel.editingLocation != null,
                        iconSize,
                        { if(mapViewModel.selectedLifeLocation == null || mapViewModel.selectedLifeLocation?.id == -1L) R.drawable.add else R.drawable.edit },
                        10.dp,
                        {
                            val error = mapViewModel.saveAndSyncLocationOrError()
                            if(error != null) Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        },
                        {
                            mapViewModel.discardChanges()
                        }
                    ) {
                        mapViewModel.edit()
                    }
                }
                Text(
                    subTitle,
                    Modifier
                        .offset(y = 20.dp*(1-state.partialToCollapsed))
                        .alpha(if(mapViewModel.editingLocation != null) 0f else state.partialToCollapsed),
                    style = TypoStyle(Theme.secondary, FontSize.MEDIUMM),
                )
                Spacer(Modifier.height(20.dp))
                val mixedLocation = mapViewModel.selectedLifeLocation ?: mapViewModel.editingLocation
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
                                mapViewModel.editingLocation != null,
                                "%.6f, %.6f".format(selectedCoordinates.latitude(), selectedCoordinates.longitude()),
                                LocationSyncable.coordsToDMS(selectedCoordinates.latitude(), selectedCoordinates.longitude()),
                                mapViewModel.coordsInput,
                                "Koordinaten"
                            ) {
                                mapViewModel.coordsInput = it
                                val (coords, _) = mapViewModel.coordsToTuple()
                                if(coords == null) return@ListEditingField
                                mapViewModel.editWith {
                                    copy(lat = coords.first, longitude = coords.second)
                                }
                                mapViewModel.selectedCoordinates = Point.fromLngLat(coords.second, coords.first)
                            }
                        }
                    }
                    if(mapViewModel.editingLocation != null)
                        Box(
                            Modifier
                                .size(iconSize + 10.dp)
                                .background(if(mapViewModel.selectingCoordsOnMapMode) Theme.primaryContainer else Color.Transparent, CircleShape)
                                .clip(CircleShape)
                                .rippleClick{
                                    mapViewModel.selectingCoordsOnMapMode = !mapViewModel.selectingCoordsOnMapMode
                                }
                                .padding(10.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.pick),
                                "Pick",
                                Modifier.fillMaxSize(),
                                if(mapViewModel.selectingCoordsOnMapMode) Theme.onPrimaryContainer else Theme.primary
                            )
                        }
                }
                AnimatedVisibility(
                    mapViewModel.selectedLifeLocation != null && mapViewModel.selectedLifeLocation?.id != -1L || mapViewModel.editingLocation != null
                ) {
                    ListEntry(
                        "Radius in Metern",
                        painterResource(R.drawable.radius)
                    ) {
                        ListEditingField(
                            mapViewModel.editingLocation != null,
                            mapViewModel.selectedLifeLocation?.radiusM?.toString() ?: "",
                            null,
                            mapViewModel.radiusMInput,
                            "Radius",
                            KeyboardType.Number
                        ) { text ->
                            mapViewModel.radiusMInput = text
                            val radiusM = text.toIntOrNull()
                            if(radiusM != null) {
                                mapViewModel.editWith {
                                    copy(radiusM = radiusM)
                                }
                            }
                        }
                    }
                }
                ListEntry(
                    "Hausnummer",
                    painterResource(R.drawable.house)
                ) {
                    ListEditingField(
                        mapViewModel.editingLocation != null,
                        mixedLocation?.number ?: "???",
                        null,
                        mapViewModel.editingLocation?.number ?: "",
                        "Nummer"
                    ) { text ->
                        mapViewModel.editWith {
                            copy(number = text)
                        }
                    }
                }
                ListEntry(
                    "Straße",
                    painterResource(R.drawable.road)
                ) {
                    ListEditingField(
                        mapViewModel.editingLocation != null,
                        mixedLocation?.street ?: "???",
                        null,
                        mapViewModel.editingLocation?.street ?: "",
                        "Straße"
                    ) { text ->
                        mapViewModel.editWith {
                            copy(street = text)
                        }
                    }
                }
                ListEntry(
                    "Ort, Land",
                    painterResource(R.drawable.globe)
                ) {
                    ListEditingField(
                        mapViewModel.editingLocation != null,
                        if (mixedLocation != null) (mixedLocation.city?.let { "$it, " }
                            ?: "") + mixedLocation.country else "Lädt...",
                        null,
                        mapViewModel.cityCountryInput,
                        "Ort, Land"
                    ) { text ->
                        mapViewModel.cityCountryInput = text
                    }
                }
                AnimatedVisibility(
                    mapViewModel.selectedLifeLocation != null && mapViewModel.selectedLifeLocation?.id != -1L || mapViewModel.editingLocation != null
                ) {
                    ListEntry(
                        "SSID",
                        painterResource(R.drawable.wifi)
                    ) {
                        ListEditingField(
                            mapViewModel.editingLocation != null,
                            mapViewModel.selectedLifeLocation?.ssid ?: "???",
                            null,
                            mapViewModel.editingLocation?.ssid,
                            "WLAN-Name"
                        ) { text ->
                            mapViewModel.editWith {
                                copy(ssid = text)
                            }
                        }
                    }
                }
            } else { // No location selected: Searchox
                ShowSelectScreen(mapViewModel)
            }
        }
    }
}

@Composable
private fun ShowSelectScreen(mapViewModel: MapViewModel) {
    var lifeQueryValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = mapViewModel.lifeQuery.value ?: "",
                selection = TextRange((mapViewModel.lifeQuery.value ?: "").length)
            )
        )
    }
    val allLocations by mapViewModel.getAllLocations.collectAsState()
    val screenWidthPx = with(LocalDensity.current) { LocalWindowInfo.current.containerDpSize.width.toPx() }
    fun selectLocation(location: LocationSyncable, newComposition: Boolean){
        mapViewModel.selectOrDeselectLifeLocation(location)
        val targetMetersOnScreen = 2 * location.radiusM.takeIf { it != 0 }.def(10) / .002f

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
        val isFieldEnabled = mapViewModel.sheetState.state != ThreeStateBottomSheetState.SheetValue.Collapsed

        val coroutineScope = rememberCoroutineScope()

        BackHandler(lifeQueryValue.text.isNotEmpty()) {
            focusManager.clearFocus()
            mapViewModel.lifeQuery.value = null
            lifeQueryValue = TextFieldValue("")
        }
        BasicTextField(
            lifeQueryValue,
            { text ->
                lifeQueryValue = text
                mapViewModel.lifeQuery.value = text.text
                mapViewModel.mapBoxSearchResults.value = null
                if(text.text.isEmpty()) {
                    mapViewModel.lifeSearchResults.value = null
                } else {
                    mapViewModel.lifeSearchResults.value = allLocations.filteredWith(
                        lifeQueryValue.text,
                        {
                            it.toAddress()
                        }
                    ) {
                        it.name
                    }.take(5)
                }
            },
            Modifier
                .padding(vertical = 5.dp)
                .background(Theme.surfaceContainerHighest, RoundedCornerShape(10.dp))
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .focusRequester(focusRequester)
                .onFocusChanged {
                    hasFocus = it.hasFocus || it.isFocused || it.isCaptured
                }
                .rippleClick{
                    coroutineScope.launch {
                        mapViewModel.sheetState.expandTo(ThreeStateBottomSheetState.SheetValue.Partial)
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
            textStyle = TypoStyle(Theme.primary, FontSize.LARGE),
            cursorBrush = SolidColor(OldColors.PRIMARYFONT),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions {
                if(mapViewModel.lifeSearchResults.value?.size != 0) {
                    selectLocation(mapViewModel.lifeSearchResults.value?.getOrNull(0) ?: return@KeyboardActions, false)
                } else {
                    mapViewModel.loadSearchResults()
                }
            },
            enabled = isFieldEnabled
        ) { innerTextField ->
            if(hasFocus || mapViewModel.lifeQuery.value!=null) {
                innerTextField()
            } else{
                Text(
                    "Suchen",
                    style = TypoStyle(Theme.secondary, FontSize.LARGE),
                )
            }
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .offset(y = (1-mapViewModel.sheetState.partialToCollapsed) * 10.dp)
        ,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Life Search",
            style = TypoStyle(Theme.secondary, FontSize.MEDIUMM)
        )
        val nav = LocalNavController.current
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                "Alle",
                Modifier
                    .rippleClick{
                        nav.navigateForResult<String?>(
                            NavPath.Pick.LOCATION,
                            "pelocation",
                            {
                                setOrRemove("pequery", mapViewModel.lifeQuery.value)
                            }
                        ) {
                            selectLocation(LocationSyncable.fromJSON(
                                JSONObject(
                                    it ?: return@navigateForResult
                                )
                            ), true)
                        }
                    }
                ,
                style = TypoStyle(Theme.secondary, FontSize.MEDIUMM)
            )
            Icon(painterResource(R.drawable.arrow_right), "All", Modifier.size(FontSize.MEDIUMM.size.toDp()), OldColors.TERTIARYFONT)
        }
    }
    Spacer(Modifier.height(10.dp))
    val lifeSearchResults by mapViewModel.lifeSearchResults.collectAsState()
    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if(lifeSearchResults.isNullOrEmpty())
                    Modifier.border(1.dp, Theme.outlineVariant, RoundedCornerShape(15.dp))
                else
                    Modifier.background(Theme.surfaceContainerHigh, RoundedCornerShape(15.dp))
            )
    ) {
        if(lifeSearchResults.isNullOrEmpty()) {
            Text(
                if(lifeSearchResults==null) "Suche etwas" else "Keine Ergebnisse",
                Modifier.padding(vertical = 15.dp).fillMaxWidth(),
                style = TypoStyle(Theme.secondary, FontSize.LARGE).copy(fontStyle = FontStyle.Italic),
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
                    Theme.primary
                )
                Column{
                    Text(
                        it.name,
                        style = TypoStyle(Theme.primary, FontSize.LARGE)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        it.toAddress(false),
                        style = TypoStyle(Theme.secondary, FontSize.SMALLM)
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(20.dp))
    Row(
        Modifier
            .fillMaxWidth()
            // .offset(y = (1-progress)*10.dp)
        ,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Mapbox Search",
            style = TypoStyle(Theme.secondary, FontSize.MEDIUMM)
        )
    }
    Spacer(Modifier.height(10.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if(lifeSearchResults.isNullOrEmpty())
                    Modifier.border(1.dp, Theme.outlineVariant, RoundedCornerShape(15.dp))
                else
                    Modifier.background(Theme.surfaceContainerHigh, RoundedCornerShape(15.dp))
            )
        ,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val mapBoxSearchResults by mapViewModel.mapBoxSearchResults.collectAsState()
        if(mapBoxSearchResults.isNullOrEmpty()) {
            if(mapBoxSearchResults == null) {
                Text(
                    "Suchen",
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(15.dp))
                        .rippleClick{
                            mapViewModel.loadSearchResults()
                        }
                        .padding(vertical = 10.dp)
                    ,
                    style = TypoStyle(Theme.secondary, FontSize.LARGE),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    "Keine Ergebnisse",
                    Modifier.padding(vertical = 15.dp).fillMaxWidth(),
                    style = TypoStyle(Theme.secondary, FontSize.LARGE).copy(fontStyle = FontStyle.Italic),
                    textAlign = TextAlign.Center
                )
            }
        }
        mapBoxSearchResults?.forEach {
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
                    Theme.primary
                )
                Column{
                    Text(
                        it.name,
                        style = TypoStyle(Theme.primary, FontSize.LARGE)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        it.toAddress(false),
                        style = TypoStyle(Theme.secondary, FontSize.SMALLM)
                    )
                }
            }
        }
    }
}