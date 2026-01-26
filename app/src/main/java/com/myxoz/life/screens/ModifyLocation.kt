package com.myxoz.life.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalStorage
import com.myxoz.life.Theme
import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.Location
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.screens.feed.fullscreenevent.InputField
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.windowPadding
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModifyLocation(){
    val db = LocalStorage.current
    val nav = LocalNavController.current
    val defaultLocation = remember {
        db.prefs.getString("editing_location", null)?.let {
            Location.fromJSON(JSONObject(it))
        }?.apply {
            db.prefs.edit {
                remove("editing_location")
            }.apply {
                db.prefs.edit {
                    remove("editing_location")
                }
            }
        }
    }
    Box(
        Modifier
            .background(Theme.background)
            .padding(windowPadding)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .setMaxTabletWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
            ,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            var name by remember { mutableStateOf(defaultLocation?.name?:"") }
            InputField(name, "Name") {
                name = it
            }
            var lat by remember { mutableStateOf(defaultLocation?.lat?.toString()?:"") }
            InputField(lat, "Lat") {
                lat = it
            }
            var long by remember { mutableStateOf(defaultLocation?.longitude?.toString()?:"") }
            InputField(long, "Long") {
                long = it
            }
            var radiusM by remember { mutableStateOf(defaultLocation?.radiusM?.toString()?: "") }
            InputField(radiusM, "RadiusM") {
                radiusM = it
            }
            var ssid by remember { mutableStateOf(defaultLocation?.ssid ?: "") }
            InputField(ssid, "SSID") {
                ssid = it
            }
            var street by remember { mutableStateOf(defaultLocation?.street ?: "") }
            InputField(street, "Street") {
                street = it
            }
            var number by remember { mutableStateOf(defaultLocation?.number ?: "") }
            InputField(number, "Number") {
                number = it
            }
            var city by remember { mutableStateOf(defaultLocation?.city ?: "") }
            InputField(city, "City") {
                city = it
            }
            var country by remember { mutableStateOf(defaultLocation?.country ?: "") }
            InputField(country, "Country") {
                country = it
            }
            var selectedPerson: PersonSyncable? by remember { mutableStateOf(null) }
            val coroutineScope = rememberCoroutineScope()
            Box(
                Modifier
                    .background(Theme.primary, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .clip(CircleShape)
                    .rippleClick {
                        coroutineScope.launch {
                            Location(
                                name,
                                long.toDoubleOrNull() ?: return@launch,
                                lat.toDoubleOrNull() ?: return@launch,
                                radiusM.toIntOrNull() ?: return@launch,
                                ssid.ifEmpty { null },
                                street.ifEmpty { null },
                                number.ifEmpty { null },
                                city.ifEmpty { null },
                                country,
                                defaultLocation?.id ?: API.generateId()
                            ).saveAndSync(db)
                            nav.popBackStack()
                        }
                    }
            ) {
                Text("Eintragen", style = TypoStyle(Theme.onPrimary, FontSize.LARGE))
            }
        }
    }
}