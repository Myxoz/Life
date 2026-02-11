package com.myxoz.life.screens.map

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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
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
import com.myxoz.life.LocalNavController
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.android.integration.MapBoxAPI
import com.myxoz.life.api.syncables.LocationSyncable
import com.myxoz.life.screens.person.displayperson.ListEditingField
import com.myxoz.life.screens.person.displayperson.ListEntry
import com.myxoz.life.screens.person.displayperson.navigateForResult
import com.myxoz.life.ui.ThreeStateBottomSheet
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.MaterialShapes
import com.myxoz.life.utils.def
import com.myxoz.life.utils.filteredWith
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
import kotlin.math.cos
import kotlin.math.ln

@Composable
fun MapSheet(mapViewModel: MapViewModel, innerPadding: PaddingValues){
    val nav = LocalNavController.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val coroutineScope = rememberCoroutineScope()
    val selectCoordsOnMap by mapViewModel.selectCoordsOnMap.collectAsState()
    val sheetLocation by mapViewModel.sheetLocation.collectAsState()
    val selectedCoordinates by mapViewModel.selectedCoordinates.collectAsState()
    val isEditing by mapViewModel.isEditing.collectAsState()
    val state = mapViewModel.sheetState
    val minSheetHeight = FontSize.XLARGE.size.toDp() + 30.dp * 2
    ThreeStateBottomSheet(
        state,
        minSheetHeight,
        Theme.surfaceContainer,
        innerPadding
    ) {
        BackHandler(sheetLocation!=null || selectedCoordinates != null) {
            mapViewModel.setSheetLocation(null)
            mapViewModel.decodedLocation.value = null
            mapViewModel.selectCoordsOnMap.value = false
            mapViewModel.isEditing.value = false
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
            ,
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
                        val response = MapBoxAPI.getLocationListFromAPIResponse(MapBoxAPI.reverseGeocode(context, coords.latitude(), coords.longitude()) ?: return@LaunchedEffect)
                        mapViewModel.decodedLocation.value = response.getOrNull(0)?.let {
                            LocationSyncable(it.name,
                                coords.longitude(),
                                coords.latitude(),
                                it.radiusM, it.ssid, it.street, it.number, it.city, it.country, it.id)
                        } ?: return@LaunchedEffect
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
                    val focusManager = LocalFocusManager.current
                    BasicTextField(
                        if(isEditing) nameInput?:"" else display,
                        {
                            mapViewModel.nameInput.value = it
                        },
                        Modifier
                            .padding(vertical = 5.dp)
                            .weight(1f)
                        ,
                        cursorBrush = SolidColor(Theme.primary),
                        enabled = isEditing && progress > 0.1f,
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
                    val morph = remember {
                        Morph(
                            MaterialShapes.Cookie12Sided,
                            MaterialShapes.Ghostish.transformed(Matrix().apply { rotateZ(90f) })
                        )
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
                            mapViewModel.selectCoordsOnMap.value = false
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
                                .background(Theme.secondaryContainer, discardShape)
                                .clip(discardShape)
                                .rippleClick{
                                    discard()
                                }
                                .padding(10.dp)
                        ) {
                            Icon(painterResource(R.drawable.close), "Discard", Modifier.fillMaxSize(), Theme.onSecondaryContainer)
                        }
                        Box(
                            Modifier
                                .size(iconSize+10.dp)
                                .background(Theme.primaryContainer, shape)
                                .clip(shape)
                                .rippleClick{
                                    if(loc == null) { // Add new Location enter edit
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
                                                mapViewModel.saveAndSync(
                                                    (reason.first?:return@launch)
                                                )
                                                mapViewModel.isEditing.value = false
                                                mapViewModel.setSheetLocation(reason.first)
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
                                Icon(painterResource(R.drawable.add), "Add", Modifier.fillMaxSize(), Theme.onSecondaryContainer)
                            }
                            androidx.compose.animation.AnimatedVisibility(
                                loc != null && !isEditing,
                                enter = scaleIn() + fadeIn(),
                                exit = scaleOut() + fadeOut(),
                            ) {
                                Icon(painterResource(R.drawable.edit), "Edit", Modifier.fillMaxSize(), Theme.onSecondaryContainer)
                            }
                            androidx.compose.animation.AnimatedVisibility(
                                loc != null && isEditing,
                                enter = scaleIn() + fadeIn(),
                                exit = scaleOut() + fadeOut(),
                            ) {
                                Icon(painterResource(R.drawable.tick), "Save", Modifier.fillMaxSize(), Theme.onSecondaryContainer)
                            }
                        }
                    }
                }
                Text(
                    subTitle,
                    Modifier
                        .offset(y = 20.dp*(1-progress))
                        .alpha(if(isEditing) 0f else progress),
                    style = TypoStyle(Theme.secondary, FontSize.MEDIUMM),
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
                            val coordsInput by mapViewModel.coordsInput.collectAsState()
                            ListEditingField(
                                isEditing,
                                "%.6f, %.6f".format(coords.latitude(), coords.longitude()),
                                LocationSyncable.coordsToDMS(coords.latitude(), coords.longitude()),
                                coordsInput,
                                "Koordinaten"
                            ) {
                                mapViewModel.coordsInput.value = it
                            }
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
                                .background(if(selectCoordsOnMap) Theme.primaryContainer else Color.Transparent, CircleShape)
                                .clip(CircleShape)
                                .rippleClick{
                                    mapViewModel.selectCoordsOnMap.value = !selectCoordsOnMap
                                }
                                .padding(10.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.pick),
                                "Pick",
                                Modifier.fillMaxSize(),
                                if(selectCoordsOnMap) Theme.onPrimaryContainer else Theme.primary
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
                        val radiusMField by mapViewModel.radiusMInput.collectAsState()
                        ListEditingField(
                            isEditing,
                            loc?.radiusM?.toString()?:"",
                            null,
                            radiusMField,
                            "Radius"
                        ) { text ->
                            mapViewModel.radiusMInput.value = text
                        }
                    }
                }
                ListEntry(
                    "Hausnummer",
                    painterResource(R.drawable.house)
                ) {
                    val numberInp by mapViewModel.numberInput.collectAsState()
                    ListEditingField(
                        isEditing,
                        mixedLocation?.number ?: "???",
                        null,
                        numberInp,
                        "Nummer"
                    ) { text ->
                        mapViewModel.numberInput.value = text
                    }
                }
                ListEntry(
                    "Straße",
                    painterResource(R.drawable.road)
                ) {
                    val streetInp by mapViewModel.streetInput.collectAsState()
                    ListEditingField(
                        isEditing,
                        mixedLocation?.street ?: "???",
                        null,
                        streetInp,
                        "Straße"
                    ) { text ->
                        mapViewModel.streetInput.value = text
                    }
                }
                ListEntry(
                    "Ort, Land",
                    painterResource(R.drawable.globe)
                ) {
                    val cityCountryInput by mapViewModel.cityCountryInput.collectAsState()
                    ListEditingField(
                        isEditing,
                        if(mixedLocation != null) (mixedLocation.city?.let { "$it, " } ?: "") + mixedLocation.country else "Lädt...",
                        null,
                        cityCountryInput,
                        "Ort, Land"
                    ) { text ->
                        mapViewModel.cityCountryInput.value = text
                    }
                }
                AnimatedVisibility(
                    loc != null
                ) {
                    ListEntry(
                        "SSID",
                        painterResource(R.drawable.wifi)
                    ) {
                        val ssidInput by mapViewModel.ssidInput.collectAsState()
                        ListEditingField(
                            isEditing,
                            loc?.ssid?:"???",
                            null,
                            ssidInput,
                            "WLAN-Name"
                        ) { text ->
                            mapViewModel.ssidInput.value = text
                        }
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
                val allLocations by mapViewModel.getAllLocations.collectAsState()
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
                        textStyle = TypoStyle(Theme.primary, FontSize.LARGE),
                        cursorBrush = SolidColor(OldColors.PRIMARYFONT),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        enabled = progress > 0.1f
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
                val screenWidthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
                fun selectLocation(location: LocationSyncable, newComposition: Boolean){
                    if(location.id == -1L) {
                        mapViewModel.setSheetLocation(null)
                        mapViewModel.selectedCoordinates.value = Point.fromLngLat(location.longitude, location.lat)
                        mapViewModel.decodedLocation.value = location
                    } else {
                        mapViewModel.setSheetLocation(location)
                    }
                    val targetMetersOnScreen = 2 * location.radiusM.takeIf { it != 0 }.def(10) / .002f // TODO TWEAK

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
                        style = TypoStyle(Theme.secondary, FontSize.MEDIUMM)
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
                        .offset(y = (1-progress)*10.dp)
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
                    if(mapBoxSearchResults.isNullOrEmpty()) {
                        if(mapBoxSearchResults == null) {
                            Text(
                                "Suchen",
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(15.dp))
                                    .rippleClick{
                                        if(lifeQueryValue.text.isBlank()) return@rippleClick
                                        coroutineScope.launch {
                                            val resp = MapBoxAPI.getLocationListFromAPIResponse(MapBoxAPI.forwardGeocode(context, lifeQueryValue.text) ?: return@launch)
                                            mapViewModel.mapBoxSearchResults.value = resp
                                        }
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
        }
    }
}