package com.myxoz.life.calendar.fullscreenevent

import android.icu.util.Calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.myxoz.life.LocalScreens
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.android.integration.HVV
import com.myxoz.life.api.syncables.LocationSyncable
import com.myxoz.life.lifeui.providers.LocalNavController
import com.myxoz.life.storage.api.syncables.SyncedEvent
import com.myxoz.life.storage.events.DetailsEvent
import com.myxoz.life.storage.events.DigSocEvent
import com.myxoz.life.storage.events.PeopleEvent
import com.myxoz.life.storage.events.SocialEvent
import com.myxoz.life.storage.events.TagEvent
import com.myxoz.life.storage.events.TimewasteEvent
import com.myxoz.life.storage.events.TitleEvent
import com.myxoz.life.storage.events.TravelEvent
import com.myxoz.life.storage.events.additionals.EventType
import com.myxoz.life.storage.events.additionals.TimedTagLikeContainer.Companion.TimedTagLikeBar
import com.myxoz.life.ui.ArrowDirection
import com.myxoz.life.ui.Chip
import com.myxoz.life.ui.drawArrowBehind
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.formatTimeStamp
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.utils.toPx

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DisplayEvent(fullEvent: SyncedEvent) {
    val context = LocalContext.current
    val screens = LocalNavController.current
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
        ,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        if (fullEvent.raw is TitleEvent) {
            Text(
                fullEvent.raw.title.takeIf { it.isNotBlank() } ?: "Kein Titel",
                Modifier
                    .fillMaxWidth(),
                style =
                    if (fullEvent.raw.title.isNotBlank())
                        TypoStyle(Theme.primary, FontSize.XXLARGE, FontFamily.Display)
                    else
                        TypoStyle(Theme.secondary, FontSize.XXLARGE).copy(fontStyle = FontStyle.Italic)
            )
        }
        if (fullEvent.raw is DetailsEvent && fullEvent.raw.details != null) {
            Text(
                fullEvent.raw.details ?: "",
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                style = TypoStyle(Theme.secondary, FontSize.MEDIUM)
            )
        }
        Spacer(Modifier.height(10.dp))
        if (fullEvent.raw is TagEvent) {
            val tagsHeight = FontSize.SMALL.size.toDp() + 2.dp
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                fullEvent.raw.eventTags.forEach {
                    Row(
                        Modifier
                            .padding(vertical = 5.dp)
                            .background(Theme.secondaryContainer, CircleShape)
                            .clip(CircleShape)
                            .rippleClick{
                                screens.openCalendarWithSearch{
                                    tags.value = tags.value.toMutableList().apply {
                                        MutableList.add(it)
                                    }
                                    if(selectedEventTypes.value.isEmpty())
                                        selectedEventTypes.value = _root_ide_package_.com.myxoz.life.storage.events.additionals.EventType.order.filter { t -> t.isTagEvent() }
                                    else
                                        selectedEventTypes.value = selectedEventTypes.value.filter { t -> t.isTagEvent() }
                                            .ifEmpty { _root_ide_package_.com.myxoz.life.storage.events.additionals.EventType.order.filter { t -> t.isTagEvent() } }
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                        ,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(it.drawable),
                            it.displayName,
                            modifier = height(tagsHeight),
                            tint = Theme.onSecondaryContainer
                        )
                        Text(
                            it.displayName,
                            color = Theme.onSecondaryContainer,
                            fontSize = FontSize.MEDIUM.size
                        )
                    }
                }
            }
        }
        if(fullEvent.raw is DigSocEvent) {
            TimedTagLikeBar(fullEvent.raw.digSocEntries) {
                screens.openCalendarWithSearch {
                    selectedEventTypes.value = listOf(_root_ide_package_.com.myxoz.life.storage.events.additionals.EventType.DigSoc)
                    digsocPlatforms.value = digsocPlatforms.value.toMutableList().apply {
                        add(it)
                    }
                }
            }
        }
        if(fullEvent.raw is TimewasteEvent) {
            TimedTagLikeBar(fullEvent.raw.timewastePlatforms) {
                screens.openCalendarWithSearch {
                    selectedEventTypes.value = listOf(_root_ide_package_.com.myxoz.life.storage.events.additionals.EventType.Timewaste)
                    timewastePlatform.value = timewastePlatform.value.toMutableList().apply {
                        MutableList.add(it)
                    }
                }
            }
        }
        if (fullEvent.raw is PeopleEvent) {
            val displayedPeople by LocalScreens.current.activityScopedState.crossRepoSuper.getPeople(fullEvent.raw.people).collectAsState()
            Spacer(Modifier.height(10.dp))
            Text("Mit:", style = TypoStyle(Theme.onSecondaryContainer, FontSize.LARGE))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                displayedPeople.forEach {
                    Chip(
                        {
                            screens.openPersonDetails(it.id)
                        }, {
                            screens.openCalendarWithSearch {
                                selectedPeople.value =
                                    selectedPeople.value.toMutableList().apply {
                                        MutableList.add(it.id)
                                    }
                                if (selectedEventTypes.value.isEmpty())
                                    selectedEventTypes.value =
                                        _root_ide_package_.com.myxoz.life.storage.events.additionals.EventType.order.filter { t -> t.isPeopleEvent() }
                                else
                                    selectedEventTypes.value = selectedEventTypes.value
                                        .filter { t -> t.isPeopleEvent() }
                                        .ifEmpty { _root_ide_package_.com.myxoz.life.storage.events.additionals.EventType.order.filter { t -> t.isPeopleEvent() } }
                            }
                        }
                    ) {
                        Text(
                            it.name,
                            style = TypoStyle(Theme.onSecondaryContainer, FontSize.MEDIUM)
                        )
                    }
                }
                if(fullEvent.raw is SocialEvent && fullEvent.raw.more)
                    Chip(null, null, color = Color.Transparent) {
                        Text("+ Weitere", style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
                    }
            }
        }
        if (fullEvent.raw is TravelEvent) {
            val profileViewModel = LocalScreens.current.activityScopedState.crossRepoSuper
            val from by profileViewModel.getLocationById(fullEvent.raw.from).collectAsState()
            val to by profileViewModel.getLocationById(fullEvent.raw.to).collectAsState()
            val size = FontSize.MEDIUM.size.toDp()
            @Composable
            fun RenderLocation(location: LocationSyncable?, isFrom: Boolean) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Theme.surfaceContainerHighest, RoundedCornerShape(20))
                        .clip(RoundedCornerShape(20))
                        .rippleClick{
                            screens.openLocation(location?:return@rippleClick)
                        }
                        .padding(20.dp)
                ) {
                    Text(
                        location?.name ?: "",
                        style = TypoStyle(Theme.primary, FontSize.XLARGE)
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        location?.toAddress() ?: "",
                        style = TypoStyle(Theme.secondary, FontSize.MEDIUM)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        location?.toCords() ?: "",
                        style = TypoStyle(Theme.secondary, FontSize.SMALLM)
                    )
                    Spacer(Modifier.height(15.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            Modifier.Companion
                                .weight(1f)
                                .background(Theme.primaryContainer, CircleShape)
                                .clip(CircleShape)
                                .rippleClick {
                                    AndroidUtils.openLink(
                                        context,
                                        HVV.constructLink(if(isFrom) location else null, if(isFrom) null else location)
                                    )
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(
                                10.dp,
                                Alignment.CenterHorizontally
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painterResource(R.drawable.hvv),
                                "HVV",
                                height(FontSize.LARGE.size.toDp()),
                                Theme.onPrimaryContainer
                            )
                            Text(
                                "HVV App",
                                style = TypoStyle(Theme.onPrimaryContainer, FontSize.MEDIUM)
                            )
                        }
                        Row(
                            Modifier.Companion
                                .weight(1f)
                                .background(Theme.primaryContainer, CircleShape)
                                .clip(CircleShape)
                                .rippleClick {
                                    LocationSyncable.openInGoogleMaps(
                                        context,
                                        location
                                    )
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(
                                10.dp,
                                Alignment.CenterHorizontally
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painterResource(R.drawable.gmaps),
                                "Maps",
                                height(FontSize.LARGE.size.toDp()),
                                Theme.onPrimaryContainer
                            )
                            Text(
                                "Google Maps",
                                style = TypoStyle(Theme.onPrimaryContainer, FontSize.MEDIUM)
                            )
                        }
                    }
                }
            }
            Column(
                Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RenderLocation(from, true)
                Row(
                    Modifier
                        .height(IntrinsicSize.Min)
                        .fillMaxWidth()
                    ,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .padding(vertical = 2.dp)
                                .clip(CircleShape)
                                .background(Theme.primaryContainer).rippleClick {
                                    LocationSyncable.openRouteInGoogleMaps(
                                        context,
                                        from?.toAddress()?.substringBeforeLast(","),
                                        from?.lat,
                                        from?.longitude,
                                        to?.toAddress()?.substringBeforeLast(","),
                                        to?.lat,
                                        to?.longitude,
                                    )
                                }.padding(5.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.gmaps),
                                "Maps",
                                size(size)
                                ,
                                Theme.onPrimaryContainer
                            )
                        }
                        val calendar = remember { Calendar.getInstance() }
                        Box(
                            Modifier
                                .padding(vertical = 2.dp)
                                .clip(CircleShape)
                                .background(Theme.primaryContainer).rippleClick {
                                    AndroidUtils.openLink(
                                        context, HVV.constructLink(
                                            from,
                                            to,
                                            if(fullEvent.raw.end > System.currentTimeMillis())
                                                fullEvent.raw.start.formatTimeStamp(calendar)
                                            else
                                                null
                                        )
                                    )
                                }.padding(5.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.hvv),
                                "HVV",
                                size(size)
                                ,
                                Theme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .drawArrowBehind(ArrowDirection.Down, 10.dp.toPx(), Theme.primary)
                        ,
                    )
                    Spacer(Modifier.width(5.dp))
                    TimedTagLikeBar(fullEvent.raw.vehicles)
                }
                RenderLocation(to, false)
            }
        }
    }
}