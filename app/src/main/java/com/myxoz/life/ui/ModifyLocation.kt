package com.myxoz.life.ui

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
import com.myxoz.life.LocalNavController
import com.myxoz.life.Theme
import com.myxoz.life.api.syncables.LocationSyncable
import com.myxoz.life.api.API
import com.myxoz.life.lifeui.theme.FontSize
import com.myxoz.life.lifeui.theme.TypoStyle
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.state.screenscoped.LocationEditingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModifyLocation(locationEditingViewModel: LocationEditingViewModel){
    val nav = LocalNavController.current
    val defaultLocation = locationEditingViewModel.nowEditing
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
            com.myxoz.life.ui.feed.fullscreenevent.InputField(name, "Name") {
                name = it
            }
            var lat by remember { mutableStateOf(defaultLocation?.lat?.toString()?:"") }
            com.myxoz.life.ui.feed.fullscreenevent.InputField(lat, "Lat") {
                lat = it
            }
            var long by remember { mutableStateOf(defaultLocation?.longitude?.toString()?:"") }
            com.myxoz.life.ui.feed.fullscreenevent.InputField(long, "Long") {
                long = it
            }
            var radiusM by remember { mutableStateOf(defaultLocation?.radiusM?.toString()?: "") }
            com.myxoz.life.ui.feed.fullscreenevent.InputField(
                radiusM,
                "RadiusM"
            ) {
                radiusM = it
            }
            var ssid by remember { mutableStateOf(defaultLocation?.ssid ?: "") }
            com.myxoz.life.ui.feed.fullscreenevent.InputField(ssid, "SSID") {
                ssid = it
            }
            var street by remember { mutableStateOf(defaultLocation?.street ?: "") }
            com.myxoz.life.ui.feed.fullscreenevent.InputField(street, "Street") {
                street = it
            }
            var number by remember { mutableStateOf(defaultLocation?.number ?: "") }
            com.myxoz.life.ui.feed.fullscreenevent.InputField(number, "Number") {
                number = it
            }
            var city by remember { mutableStateOf(defaultLocation?.city ?: "") }
            com.myxoz.life.ui.feed.fullscreenevent.InputField(city, "City") {
                city = it
            }
            var country by remember { mutableStateOf(defaultLocation?.country ?: "") }
            com.myxoz.life.ui.feed.fullscreenevent.InputField(
                country,
                "Country"
            ) {
                country = it
            }
            val coroutineScope = rememberCoroutineScope()
            Box(
                Modifier
                    .background(Theme.primary, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .clip(CircleShape)
                    .rippleClick {
                        coroutineScope.launch {
                            locationEditingViewModel.save(
                                LocationSyncable(
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
                                )
                            )
                            nav.popBackStack()
                        }
                    }
            ) {
                Text("Eintragen", style = TypoStyle(Theme.onPrimary, FontSize.LARGE))
            }
        }
    }
}