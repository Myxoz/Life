package com.myxoz.life.subscreens.pick

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalStorage
import com.myxoz.life.R
import com.myxoz.life.api.Location
import com.myxoz.life.calendar.InputField
import com.myxoz.life.subscreens.edgeToEdgeGradient
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.filteredWith
import com.myxoz.life.utils.rippleClick
import kotlinx.coroutines.launch

@Composable
fun PickExistingLocation(){
    val db = LocalStorage.current
    val nav = LocalNavController.current
    val allLocations by produceState(listOf()) {
        val locations = db.location.getAllLocations()
        value = locations.map { Location.from(it) }.sortedBy { it.name }
    }
    var filtered by remember(allLocations) {
        mutableStateOf(allLocations)
    }
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        containerColor = Colors.BACKGROUND,
    ) { innerPadding ->
        BackHandler(true) {
            nav.previousBackStackEntry?.savedStateHandle?.set("pelocation", null)
            nav.navigateUp()
        }
        Column(
            Modifier
                .fillMaxSize()
        ) {
            LazyColumn(
                Modifier
                    .weight(1f)
                    .edgeToEdgeGradient(Colors.BACKGROUND, innerPadding)
                ,
                reverseLayout = true, // IMPORTANT
                verticalArrangement = Arrangement.Bottom
            ){
                itemsIndexed(filtered, { _, it -> it.id}){ i, it ->
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(15.dp))
                            .rippleClick{
                                coroutineScope.launch {
                                    nav.previousBackStackEntry?.savedStateHandle?.set("pelocation", it.toJson(db).toString())
                                    nav.navigateUp()
                                }
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
                    if(i != filtered.size-1) HorizontalDivider(color = Colors.DIVIDERS)
                }
                item { // At the start
                    Spacer(Modifier.height(innerPadding.calculateTopPadding()))
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ){
                InputField(
                    null,
                    "Suchen"
                ) { filter ->
                    filtered = allLocations.filteredWith(filter, {it.toAddress(true)}) { it.name }
                }
                Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
            }
        }
    }
}