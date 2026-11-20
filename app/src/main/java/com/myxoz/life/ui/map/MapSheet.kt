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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.myxoz.life.LocalNavController
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.android.integration.MapBoxAPI
import com.myxoz.life.api.syncables.LocationSyncable
import com.myxoz.life.core.filteredWith
import com.myxoz.life.ui.EditToTickAndDiscard
import com.myxoz.life.ui.NavPath
import com.myxoz.life.ui.ThreeStateBottomSheetState
import com.myxoz.life.ui.ThreeStateBottomSheetState.Companion.ThreeStateBottomSheet
import com.myxoz.life.ui.person.displayperson.ListEditingField
import com.myxoz.life.ui.person.displayperson.ListEntry
import com.myxoz.life.ui.person.displayperson.navigateForResult
import com.myxoz.life.lifeui.theme.FontFamily
import com.myxoz.life.lifeui.theme.FontSize
import com.myxoz.life.lifeui.theme.OldColors
import com.myxoz.life.lifeui.theme.TypoStyle
import com.myxoz.life.utils.nullIfEmpty
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun MapSheet(mapRepo: MapRepo, innerPadding: PaddingValues) {
    val sheetState = mapRepo.sheetState
    val minSheetHeight = FontSize.XLARGE.size.toDp() + 30.dp * 2
    ThreeStateBottomSheet(
        sheetState,
        minSheetHeight,
        Theme.surfaceContainer,
        innerPadding
    ) {
        val sheetState = mapRepo.sheetState
        val currentStackItem by mapRepo.state.collectAsState()
        BackHandler(currentStackItem !is MapDisplayState.SearchPage) {
            mapRepo.popStack()
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
            ,
        ) {
            val iconSize = FontSize.XLARGE.size.toDp()
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            if(currentStackItem !is MapDisplayState.SearchPage) {
                val isNotCollapsed = mapRepo.sheetState.state != ThreeStateBottomSheetState.SheetValue.Collapsed
                LaunchedEffect(currentStackItem is MapDisplayState.PinNotResolved && isNotCollapsed) {
                    val stackItem = currentStackItem as? MapDisplayState.PinNotResolved
                    if(isNotCollapsed && stackItem != null) {
                        val reversedLocation = MapBoxAPI.reverseGeocode(context, stackItem.lat, stackItem.longitude) ?: return@LaunchedEffect
                        mapRepo.updateLastStackItem(reversedLocation)
                    }
                }
                Spacer(Modifier.height(20.dp))
                val displayed = currentStackItem as? MapLocation ?: return@Column
                val asEditing = displayed as? MapDisplayState.EditingLocation
                val locationDisplayable = displayed as? LocationDisplayable
                val isEditing = asEditing != null
                Row(
                    Modifier
                        .fillMaxWidth()
                    ,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val focusManager = LocalFocusManager.current
                    BasicTextField(
                        displayed.displayName,
                        {
                            asEditing?.displayName = it
                        },
                        Modifier
                            .padding(vertical = 5.dp)
                            .weight(1f)
                        ,
                        cursorBrush = SolidColor(Theme.primary),
                        enabled = isEditing,
                        textStyle = TypoStyle(Theme.primary, FontSize.XLARGE, FontFamily.Display),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions{
                            focusManager.clearFocus()
                        },
                        maxLines = 1,
                    )
                    EditToTickAndDiscard(
                        isEditing,
                        iconSize,
                        {
                            val useAdd = when(displayed) {
                                is MapDisplayState.DisplayedLifeLocation -> false
                                is MapDisplayState.EditingLocation -> displayed.basedOnId == null
                                is MapDisplayState.ResolvedLocation, is MapDisplayState.PinNotResolved -> true
                            }
                            if(useAdd) R.drawable.add else R.drawable.edit
                        },
                        10.dp,
                        {
                            coroutineScope.launch {
                                val error = mapRepo.saveEdited(asEditing ?: return@launch)
                                if(error != null) Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        },
                        {
                            mapRepo.popStack()
                        }
                    ) {
                        mapRepo.pushToStack((displayed as? Editable)?.toEditing() ?: return@EditToTickAndDiscard)
                    }
                }
                Text(
                    displayed.subTitle,
                    Modifier
                        .offset(y = 20.dp*(1-sheetState.partialToCollapsed))
                        .alpha(if(isEditing) 0f else sheetState.partialToCollapsed),
                    style = TypoStyle(Theme.secondary, FontSize.MEDIUMM),
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.weight(1f),
                    ) {
                        BackHandler(asEditing?.selectingCoordsOnMap == true) {
                            asEditing?.selectingCoordsOnMap = false
                        }
                        ListEntry(
                            "Koordinaten",
                            painterResource(R.drawable.location)
                        ) {
                            ListEditingField(
                                isEditing,
                                locationDisplayable?.toCoords() ?: "",
                                locationDisplayable?.toDMS(),
                                asEditing?.coordsField ?: "",
                                "Koordinaten"
                            ) {
                                asEditing?.setCoords(it)
                            }
                        }
                    }
                    if(isEditing)
                        Box(
                            Modifier
                                .size(iconSize + 10.dp)
                                .background(if(asEditing.selectingCoordsOnMap) Theme.primaryContainer else Color.Transparent, CircleShape)
                                .clip(CircleShape)
                                .rippleClick{
                                    asEditing.selectingCoordsOnMap = !asEditing.selectingCoordsOnMap
                                }
                                .padding(10.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.pick),
                                "Pick",
                                Modifier.fillMaxSize(),
                                if(asEditing.selectingCoordsOnMap) Theme.onPrimaryContainer else Theme.primary
                            )
                        }
                }
                AnimatedVisibility(
                    isEditing || displayed is MapDisplayState.DisplayedLifeLocation
                ) {
                    ListEntry(
                        "Radius in Metern",
                        painterResource(R.drawable.radius)
                    ) {
                        ListEditingField(
                            isEditing,
                            locationDisplayable?.radiusM?.toString() ?: "",
                            null,
                            asEditing?.radiusMField ?: "",
                            "Radius",
                            KeyboardType.Number
                        ) { text ->
                            asEditing?.setRadius(text)
                        }
                    }
                }
                ListEntry(
                    "Hausnummer",
                    painterResource(R.drawable.house)
                ) {
                    ListEditingField(
                        isEditing,
                        locationDisplayable?.number ?: "",
                        null,
                        asEditing?.numberField ?: "",
                        "Nummer"
                    ) { text ->
                        asEditing?.numberField = text
                    }
                }
                ListEntry(
                    "Straße",
                    painterResource(R.drawable.road)
                ) {
                    ListEditingField(
                        isEditing,
                        locationDisplayable?.street ?: "",
                        null,
                        asEditing?.streetField ?: "",
                        "Straße"
                    ) { text ->
                        asEditing?.streetField = text
                    }
                }
                ListEntry(
                    "Ort, Land",
                    painterResource(R.drawable.globe)
                ) {
                    ListEditingField(
                        isEditing,
                        locationDisplayable?.let{ "${if(it.city != null) it.city+", " else ""}${it.country}" } ?: "",
                        null,
                        asEditing?.cityCountryInput ?: "",
                        "Ort, Land"
                    ) { text ->
                        asEditing?.cityCountryInput = text
                    }
                }
                AnimatedVisibility(
                    isEditing || (displayed is MapDisplayState.DisplayedLifeLocation && displayed.ssid != null)
                ) {
                    ListEntry(
                        "SSID",
                        painterResource(R.drawable.wifi)
                    ) {
                        ListEditingField(
                            isEditing,
                            (displayed as? MapDisplayState.DisplayedLifeLocation)?.ssid ?: "",
                            null,
                            asEditing?.ssid,
                            "WLAN-Name"
                        ) { text ->
                            asEditing?.ssid = text
                        }
                    }
                }
            } else { // No location selected: Searchox
                ShowSelectScreen(mapRepo, currentStackItem as? MapDisplayState.SearchPage ?: return@Column)
            }
        }
    }
}

@Composable
private fun ShowSelectScreen(mapRepo: MapRepo, startScreen: MapDisplayState.SearchPage) {
    val allLocations by mapRepo.allLocations.collectAsState()
    val lifeSearchResults by remember(allLocations) {
        derivedStateOf {
            allLocations.filteredWith(
                startScreen.inputQuery.nullIfEmpty() ?: return@derivedStateOf listOf(),
                {
                    it.toAddress()
                }
            ) {
                it.displayName
            }.take(5)
        }
    }
    val coroutineScope = rememberCoroutineScope()
    SearchField(mapRepo, startScreen) {
        if (lifeSearchResults.isNotEmpty()) {
            mapRepo.animateSelectLocation(lifeSearchResults.getOrNull(0) ?: return@SearchField)
        } else {
            coroutineScope.launch {
                startScreen.onlineSearchResults =
                    mapRepo.loadSearchResults(startScreen.inputQuery)
            }
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .offset(y = (1-mapRepo.sheetState.partialToCollapsed) * 10.dp)
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
                                setOrRemove("pequery", startScreen.inputQuery)
                            }
                        ) {
                            coroutineScope.launch {
                                mapRepo.animateSelectLocation(
                                    MapDisplayState.DisplayedLifeLocation(
                                        LocationSyncable.fromJSON(
                                            JSONObject(it ?: return@launch)
                                        )
                                    )
                                )
                            }
                        }
                    }
                ,
                style = TypoStyle(Theme.secondary, FontSize.MEDIUMM)
            )
            Icon(painterResource(R.drawable.arrow_right), "All", Modifier.size(FontSize.MEDIUMM.size.toDp()), OldColors.TERTIARYFONT)
        }
    }
    Spacer(Modifier.height(10.dp))
    val onClick: (LocationDisplayable) -> Unit = { coroutineScope.launch {
        mapRepo.animateSelectLocation(it)
    } }
    SearchResults(lifeSearchResults, onClick) {}
    Spacer(Modifier.height(20.dp))
    Row(
        Modifier
            .fillMaxWidth()
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
    SearchResults(startScreen.onlineSearchResults, onClick)  {
        coroutineScope.launch {
            startScreen.onlineSearchResults = mapRepo.loadSearchResults(startScreen.inputQuery)
        }
    }
}
@Composable
private fun SearchField(mapRepo: MapRepo, startScreen: MapDisplayState.SearchPage, onSubmit: suspend ()->Unit) {
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
        val isFieldEnabled = mapRepo.sheetState.state != ThreeStateBottomSheetState.SheetValue.Collapsed
        val coroutineScope = rememberCoroutineScope()

        BackHandler(startScreen.inputQuery != "") {
            focusManager.clearFocus()
            startScreen.inputQuery = ""
        }
        BasicTextField(
            startScreen.inputQuery,
            { text ->
                startScreen.inputQuery = text
                startScreen.onlineSearchResults = null
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
                        mapRepo.sheetState.expandTo(ThreeStateBottomSheetState.SheetValue.Partial)
                        focusRequester.requestFocus()
                    }
                }
                .padding(horizontal = 15.dp, vertical = 10.dp)
            ,
            textStyle = TypoStyle(Theme.primary, FontSize.LARGE),
            cursorBrush = SolidColor(Theme.primary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions {
                coroutineScope.launch {
                    onSubmit()
                }
            },
            enabled = isFieldEnabled
        ) { innerTextField ->
            if(hasFocus || startScreen.inputQuery != "") {
                innerTextField()
            } else{
                Text(
                    "Suchen",
                    style = TypoStyle(Theme.secondary, FontSize.LARGE),
                )
            }
        }
    }
}
@Composable
private fun SearchResults(results: List<LocationDisplayable>?, onClick: (LocationDisplayable) -> Unit, startSearching: ()-> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if(results.isNullOrEmpty())
                    Modifier.border(1.dp, Theme.outlineVariant, RoundedCornerShape(15.dp))
                else
                    Modifier.background(Theme.surfaceContainerHigh, RoundedCornerShape(15.dp))
            )
        ,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if(results.isNullOrEmpty()) {
            if(results == null) {
                Text(
                    "Suchen",
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(15.dp))
                        .rippleClick(run = startSearching)
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
        results?.forEach {
            RenderLocationListItem(it) { onClick(it) }
        }
    }
}

@Composable
fun RenderLocationListItem(it: LocationDisplayable, onClick: ()->Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(15.dp))
            .fillMaxWidth()
            .rippleClick(run = onClick)
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
                it.displayName,
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