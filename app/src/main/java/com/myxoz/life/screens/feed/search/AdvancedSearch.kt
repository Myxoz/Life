package com.myxoz.life.screens.feed.search

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalNavController
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.events.additionals.DigSocPlatform
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.events.additionals.Vehicle
import com.myxoz.life.screens.feed.fullscreenevent.CalendarChip
import com.myxoz.life.screens.feed.fullscreenevent.InputField
import com.myxoz.life.screens.feed.fullscreenevent.PersonBar
import com.myxoz.life.screens.feed.fullscreenevent.TagsBar
import com.myxoz.life.ui.ActionBar
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.toDp
import com.myxoz.life.viewmodels.CalendarViewModel
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedSearch(calendarViewModel: CalendarViewModel){
    val search = calendarViewModel.search
    Scaffold(
        containerColor = Theme.background
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
            ,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(.95f)
                ,
                verticalArrangement = Arrangement.spacedBy(15.dp, Alignment.Bottom),
            ) {
                val selectedPeople by search.selectedPeople.collectAsState()
                val nav = LocalNavController.current
                val selectedCalendars by search.selectedEventTypes.collectAsState()
                if(selectedCalendars.isNotEmpty() && selectedCalendars.none { it != EventType.DigSoc }){
                    val digsocPlatforms by search.digsocPlatforms.collectAsState()
                    TagLikeSelection(DigSocPlatform.entries.toList(), digsocPlatforms){
                        search.digsocPlatforms.value = it
                    }
                }
                if(selectedCalendars.isNotEmpty() && selectedCalendars.all { it.isTagEvent() }) {
                    val tags by search.tags.collectAsState()
                    TagsBar(
                        tags
                    ) {
                        search.tags.value = it
                    }
                }
                if(selectedCalendars.isNotEmpty() && selectedCalendars.all { it.isTitleEvent() }) {
                    val title by search.titleQuery.collectAsState()
                    InputField(
                        title,
                        "Title",
                    ){
                        search.titleQuery.value = it
                    }
                }
                if(selectedCalendars.isNotEmpty() && selectedCalendars.all { it.isDetailsEvent() }) {
                    val details by search.detailsQuery.collectAsState()
                    InputField(
                        details,
                        "Details",
                    ){
                        search.detailsQuery.value = it
                    }
                }
                if(selectedCalendars.isNotEmpty() && selectedCalendars.none { it != EventType.Travel }){
                    val from by search.locationFrom.collectAsState()
                    MultipleLoctionBar(
                        from,
                        "Von"
                    ) {
                        search.locationFrom.value = it
                    }
                    @SuppressLint("StateFlowValueCalledInComposition") // Default value, this is fine and SHOULD NOT update when it updates
                    TagLikeSelection(Vehicle.entries.toList(), search.selectedVehicles.value){
                        search.selectedVehicles.value = it
                    }
                    val to by search.locationTo.collectAsState()
                    MultipleLoctionBar(
                        to,
                        "Nach"
                    ) {
                        search.locationTo.value = it
                    }
                }
                @Composable
                fun CheckBoxRow(mutableFlow: MutableStateFlow<Boolean>, title: String){
                    val textSize = FontSize.MEDIUM.size.toDp()
                    Row(
                        Modifier
                            .fillMaxWidth()
                        ,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val ignoreCase by mutableFlow.collectAsState()
                        Checkbox(ignoreCase, {
                            mutableFlow.value = it
                        },
                            Modifier.size(textSize),
                        )
                        Text(
                            title,
                            style = TypoStyle(Theme.secondary, FontSize.MEDIUM),
                            modifier = Modifier
                                .clickable(null, null){
                                    mutableFlow.value = !mutableFlow.value
                                }
                        )
                    }
                }
                CheckBoxRow(search.regexMode, "Regex-Modus")
                CheckBoxRow(search.ignoreCase, "Groß- und Kleinschreibung ignorieren")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (type in EventType.order) {
                        CalendarChip(
                            type,
                            selectedCalendars.contains(type)
                        ) {
                            if(!selectedCalendars.contains(type))
                                search.selectedEventTypes.value = selectedCalendars + listOf(type)
                            else
                                search.selectedEventTypes.value = selectedCalendars.toMutableList().apply { remove(type) }
                        }
                    }
                }
                HorizontalDivider(Modifier.clip(CircleShape), color = Theme.outlineVariant)
                PersonBar(
                    selectedPeople,
                    false,
                    false,
                    {
                        nav.navigate("contacts")
                    }
                ) { l, _ ->
                    search.selectedPeople.value = l
                }
                HorizontalDivider(Modifier.clip(CircleShape), color = Theme.outlineVariant)
                ActionBar(
                    {
                        search.reset()
                        nav.popBackStack("home", false, true)
                    },
                    {
                        Icon(painterResource(R.drawable.close), "Close", Modifier.fillMaxSize(), tint = OldColors.SECONDARYFONT)
                    },
                    OldColors.SELECTED,
                    {
                        nav.popBackStack("home", false, true)
                    },
                ) {
                    Text(
                        "Suchen",
                        style = TypoStyle(
                            Theme.primary,
                            FontSize.LARGE
                        ).copy(fontWeight = FontWeight.W900)
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painterResource(R.drawable.arrow_right),
                        "Continue",
                        tint = Theme.primary,
                        modifier = Modifier.height(20.dp)
                    )
                }
                search.wasUpdated()
            }
        }
    }
}