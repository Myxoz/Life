package com.myxoz.life.search

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalStorage
import com.myxoz.life.R
import com.myxoz.life.api.Location
import com.myxoz.life.api.PersonSyncable
import com.myxoz.life.events.additionals.TagLike
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.viewmodels.CalendarViewModel

@Composable
fun LifeBottomBar(calendarViewModel: CalendarViewModel){
    val nav = LocalNavController.current
    Row(
        Modifier
            .padding(vertical = 7.dp, horizontal = 10.dp)
            .fillMaxWidth()
        ,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val search = calendarViewModel.search
        val mode by search.mode.collectAsState()
        val inputedValue by search.textInputValue.collectAsState()
        val setWidth by calendarViewModel.dayAmount.collectAsState()
        val width by animateFloatAsState(setWidth.toFloat())
        val rowHeight = FontSize.LARGE.size.toDp() + 20.dp
        val db = LocalStorage.current
        val allPeopleMap by produceState(mapOf()) {
            val allPeople = db.people.getAllPeople()
            val map = mutableMapOf<Long, PersonSyncable>()
            for (entity in allPeople) {
                map[entity.id] = PersonSyncable.from(db, entity)
            }
            value = map
        }
        val allLocations by produceState(mapOf()) {
            val allLocations = db.location.getAllLocations()
            val map = mutableMapOf<Long, Location>()
            for (entity in allLocations) {
                map[entity.id] = Location.from(entity)
            }
            value = map
        }
        Row(
            Modifier
                .clip(CircleShape)
                .size(rowHeight)
                .padding(7.dp)
                .rippleClick {
                    calendarViewModel.dayAmount.value = setWidth % 4 + 1
                }
        ) {
            repeat(4){
                val widthPerElem = (FontSize.LARGE.size.toDp() + 6.dp)/width
                Box(Modifier
                    .fillMaxHeight()
                    .width(widthPerElem)
                    .padding(horizontal = (6f / width).dp)
                    .background(Colors.PRIMARYFONT, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(7.dp))
        val focusManager = LocalFocusManager.current
        var hasFocus by remember { mutableStateOf(false) }
        if(mode == SearchField.SearchMode.Target && search.isSearching()) {
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50, 20, 20, 50))
                    .background(Colors.SECONDARY, RoundedCornerShape(50, 20, 20, 50))
                    .rippleClick {
                        nav.navigate("advanced_search")
                    }
                    .padding(start = 15.dp, end = 5.dp)
                    .height(rowHeight)
                ,
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .weight(1f)
                    ,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val eventTypes by search.selectedEventTypes.collectAsState()
                    val tags by search.tags.collectAsState()
                    val rowHeight = FontSize.LARGE.size.toDp()
                    if(eventTypes.isNotEmpty()) {
                        Box(
                            Modifier
                                .background(Colors.TERTIARY, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .width((eventTypes.size + 1) * rowHeight / 2)
                        ){
                            eventTypes.forEachIndexed { i, type ->
                                Box(
                                    Modifier
                                        .offset(x = rowHeight / 2 * i)
                                        .background(type.color, CircleShape)
                                        .size(rowHeight)
                                )
                            }

                        }
                    }
                    val vehicle by search.selectedVehicles.collectAsState()
                    val digsocPlatforms by search.digsocPlatforms.collectAsState()
                    for (like in arrayOf<List<TagLike>>(tags, vehicle, digsocPlatforms)) {
                        if(like.isEmpty()) continue
                        Row(
                            Modifier
                                .background(Colors.TERTIARY, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                            ,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            for (tag in like) {
                                Icon(
                                    painterResource(tag.drawable),
                                    "Taglike",
                                    Modifier.size(rowHeight),
                                    Colors.PRIMARYFONT
                                )
                            }
                        }
                    }
                    val people by search.selectedPeople.collectAsState()
                    val locationsFrom by search.locationFrom.collectAsState()
                    val locationsTo by search.locationFrom.collectAsState()
                    for (string in arrayOf(
                        if (people.isNotEmpty()) "Mit: " + people.mapNotNull { allPeopleMap[it] }.joinToString(",") { it.name } else null,
                        if (locationsFrom.isNotEmpty()) "Abfahrt: " + locationsFrom.mapNotNull { allLocations[it] }.joinToString(",") { it.name } else null,
                        if (locationsTo.isNotEmpty()) "Ankunft: " + locationsTo.mapNotNull { allLocations[it] }.joinToString(",") { it.name } else null,
                    )) {
                        if(string==null) continue
                        Box(
                            Modifier
                                .background(Colors.TERTIARY, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                            ,
                            contentAlignment = Alignment.Center
                        ) {
                            Text(string, style = TypoStyle(FontColor.PRIMARY, FontSize.MEDIUM))
                        }
                    }
                }
                Icon(
                    painterResource(R.drawable.close),
                    "End",
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .rippleClick {
                            focusManager.clearFocus()
                            search.reset()
                        }
                    ,
                    Colors.SECONDARYFONT
                )
            }
        } else BasicTextField(
            inputedValue?:"",
            {
                search.setText(it)
            },
            Modifier
                .weight(1f)
                .onFocusChanged {
                    hasFocus = it.hasFocus || it.isFocused || it.isCaptured
                }
            ,
//            placeholder = @Composable{
//                Text("Filtern / Finden", style = TypoStyle(FontColor.SECONDARY, FontSize.LARGE))
//            },
//            colors = TextFieldDefaults.colors(
//                unfocusedContainerColor = Color.Transparent,
//                focusedContainerColor = Color.Transparent,
//                focusedIndicatorColor = Color.Transparent,
//                unfocusedIndicatorColor = Color.Transparent
//            ),
            decorationBox = @Composable { innerTextField ->
                Box(
                    Modifier
                        .background(Colors.SECONDARY, RoundedCornerShape(50, 20, 20, 50))
                        .fillMaxWidth()
                        .padding(start = 15.dp, end = 5.dp)
                        .height(rowHeight)
                    ,
                    contentAlignment = Alignment.CenterStart
                ){
                    if(hasFocus || inputedValue!=null) {
                        Row(
                            Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .weight(1f)
                            ) {
                                innerTextField()
                            }
                            Icon(
                                painterResource(R.drawable.close),
                                "End",
                                Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .rippleClick {
                                        focusManager.clearFocus()
                                        search.reset()
                                    }
                                ,
                                Colors.SECONDARYFONT
                            )
                        }
                    } else{
                        Text(
                            "Suchen / Finden",
                            Modifier.align(Alignment.Center),
                            style = TypoStyle(FontColor.SECONDARY, FontSize.LARGE),
                        )
                    }
                }
            },
            singleLine = true,
            cursorBrush = SolidColor(Colors.PRIMARYFONT),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
            }),
            textStyle = TypoStyle(FontColor.PRIMARY, FontSize.LARGE)
        )
        Spacer(Modifier.width(2.dp))
        val navController = LocalNavController.current
        val targetShape = RoundedCornerShape(20, 50, 50, 20)
        Box(
            Modifier
                .clip(targetShape)
                .size(rowHeight)
                .background(Colors.SECONDARY, targetShape)
                .padding(7.dp)
                .rippleClick {
                    search.mode.value = SearchField.SearchMode.Target
                    navController.navigate("advanced_search")
                }
        ) {
            Icon(painterResource(R.drawable.target), "Search Specific", Modifier.fillMaxSize(), Colors.PRIMARYFONT)
        }
        Spacer(Modifier.width(5.dp))
        Box(
            Modifier
                .clip(CircleShape)
                .size(rowHeight)
                .background(Colors.SECONDARY, CircleShape)
                .padding(7.dp)
                .rippleClick {
                    navController.navigate("settings")
                }
        ) {
            Icon(painterResource(R.drawable.app), "Life", Modifier.fillMaxSize(), Colors.PRIMARYFONT)
        }
    }
}