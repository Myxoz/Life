package com.myxoz.life.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
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
import com.myxoz.life.api.API
import com.myxoz.life.api.Location
import com.myxoz.life.api.PersonSyncable
import com.myxoz.life.calendar.InputField
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.rippleClick
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModifyLocation(){
    val db = LocalStorage.current
    val nav = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()
    val defaultLocation = remember {
        db.prefs.getString("editing_location", null)?.let {
            println(it)
            Location.fromJSON(JSONObject(it)).apply {
                coroutineScope.launch {
                    println(this@apply.toJson(db))
                }
            }
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
    Scaffold(
        Modifier.fillMaxSize(),
        containerColor = Colors.BACKGROUND
    ) { innerPadding ->
        Box(
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier.fillMaxWidth(.95f),
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
                        .background(Colors.SECONDARY, CircleShape)
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
                    Text("Eintragen", style = TypoStyle(FontColor.PRIMARY, FontSize.LARGE))
                }
            }
        }
    }
}