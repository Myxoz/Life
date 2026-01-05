package com.myxoz.life.screens.feed.search

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalStorage
import com.myxoz.life.api.syncables.Location
import com.myxoz.life.screens.feed.fullscreenevent.ImeActionClicked
import com.myxoz.life.screens.feed.fullscreenevent.InputField
import com.myxoz.life.events.additionals.TagLike
import com.myxoz.life.ui.Chip
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.filteredWith
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultipleLoctionBar(
    defaultSelectedLocations: List<Long>,
    placeholder: String,
    updateLocations: (List<Long>)->Unit
){
    val db = LocalStorage.current
    val selectedLocations = remember { defaultSelectedLocations.toMutableStateList() }
    val allLocations = remember { mutableStateListOf<Location>() }
    LaunchedEffect(Unit) {
        val locations = db.location.getAllLocations()
        allLocations.clear()
        allLocations.addAll(
            locations.map{
                Location.from(it)
            }
        )
    }
    var search: String? by remember {
        mutableStateOf(null)
    }
    val leftItems by remember {
        derivedStateOf {
            val locations = allLocations.filter {
                it.id !in selectedLocations
            }
            val search = search
            if(search != null)
                locations.filteredWith(search, {it.toAddress(true)}){ it.name }
            else
                locations
        }
    }
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FlowRow(
            Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            selectedLocations.forEach { person ->
                val decPerson = allLocations.firstOrNull { it.id == person }
                Chip({
                    selectedLocations.remove(person)
                    updateLocations(selectedLocations)
                }) {
                    Text(
                        decPerson?.name?:"NULL",
                        style = TypoStyle(FontColor.PRIMARY, FontSize.SMALL)
                    )
                }
            }
        }
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            leftItems.forEach { person ->
                Chip({
                    selectedLocations.add(person.id)
                    updateLocations(selectedLocations)
                }) {
                    Text(
                        person.name,
                        style = TypoStyle(FontColor.SECONDARY, FontSize.SMALL)
                    )
                }
            }
        }
        InputField(null, placeholder, onImeAction = {
            return@InputField if(search!=null) {
                leftItems.getOrNull(0)?.apply { selectedLocations.add(this.id); updateLocations(selectedLocations) }
                ImeActionClicked(false, clearField = true)
            } else {
                ImeActionClicked(true, clearField = false)
            }
        }) {
            search = it.ifEmpty { null }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T: TagLike> TagLikeSelection(allTags: List<T>, defaultSelectedTags: List<T>, updateTags: (List<T>)->Unit){
    val selectedTags = remember { defaultSelectedTags.toMutableStateList() }
    val height = FontSize.MEDIUM.size.toDp()
    val notSelectedTags by remember {
        derivedStateOf {
            allTags.filter { it !in selectedTags }
        }
    }
    FlowRow(
        Modifier
            .fillMaxWidth()
        ,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        (selectedTags.map{it to true} + notSelectedTags.map{it to false}).forEach {
            Box(
                Modifier
                    .background(Colors.SECONDARY, CircleShape)
                    .clip(CircleShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .rippleClick{
                        if(it.second) {
                            selectedTags.remove(it.first)
                            updateTags(selectedTags)
                        } else {
                            selectedTags.add(it.first)
                            updateTags(selectedTags)
                        }
                    }
            ){
                Icon(
                    painterResource(it.first.drawable),
                    "Icon",
                    Modifier
                        .size(height),
                    if(it.second) Colors.PRIMARYFONT else Colors.TERTIARYFONT
                )
            }
        }
    }
}