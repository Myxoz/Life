package com.myxoz.life.calendar

import android.icu.util.Calendar
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalStorage
import com.myxoz.life.R
import com.myxoz.life.api.Location
import com.myxoz.life.api.PersonSyncable
import com.myxoz.life.api.SyncedEvent
import com.myxoz.life.events.DigSocEvent
import com.myxoz.life.events.SocialEvent
import com.myxoz.life.events.TravelEvent
import com.myxoz.life.events.Vehicle
import com.myxoz.life.events.additionals.DetailsEvent
import com.myxoz.life.events.additionals.PeopleEvent
import com.myxoz.life.events.additionals.TagEvent
import com.myxoz.life.events.additionals.TagLike
import com.myxoz.life.events.additionals.TimedTagLikeContainer
import com.myxoz.life.events.additionals.TitleEvent
import com.myxoz.life.integration.HVV
import com.myxoz.life.rippleClick
import com.myxoz.life.subscreens.formatTimeStamp
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.ui.theme.dp
import com.myxoz.life.viewmodels.ProfileInfoModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DisplayEvent(fullEvent: SyncedEvent, profileInfoModel: ProfileInfoModel){
    val context = LocalContext.current
    val db = LocalStorage.current
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        if (fullEvent.proposed is TitleEvent) {
            Text(
                fullEvent.proposed.title.takeIf { it.isNotBlank() } ?: "Kein Titel",
                Modifier
                    .fillMaxWidth(),
                style =
                    if (fullEvent.proposed.title.isNotBlank())
                        TypoStyle(FontColor.PRIMARY, FontSize.XXLARGE, FontFamily.Display)
                    else
                        TypoStyle(FontColor.SECONDARY, FontSize.XXLARGE).copy(fontStyle = FontStyle.Italic)
            )
        }
        if (fullEvent.proposed is DetailsEvent && fullEvent.proposed.details != null) {
            Text(
                fullEvent.proposed.details ?: "",
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM)
            )
        }
        Spacer(Modifier.height(10.dp))
        if (fullEvent.proposed is TagEvent) {
            val tagsHeight = FontSize.SMALL.size.dp + 2.dp
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                fullEvent.proposed.eventTags.forEach {
                    Row(
                        Modifier
                            .padding(vertical = 5.dp)
                            .background(fullEvent.proposed.type.color, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .clip(CircleShape),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(it.drawable),
                            it.displayName,
                            modifier = Modifier.height(tagsHeight),
                            tint = fullEvent.proposed.type.selectedColor
                        )
                        Text(
                            it.displayName,
                            color = fullEvent.proposed.type.selectedColor,
                            fontSize = FontSize.MEDIUM.size
                        )
                    }
                }
            }
        }
        if(fullEvent.proposed is DigSocEvent) {
            TimedTagLikeBar(fullEvent.proposed.digSocEntries)
        }
        if (fullEvent.proposed is PeopleEvent) {
            var displayedPeople by remember {
                mutableStateOf(
                    listOf<PersonSyncable>()
                )
            }
            PeopleEvent.GetFullNames(db, fullEvent.proposed.people) {
                displayedPeople = it
            }
            Spacer(Modifier.height(10.dp))
            Text("Mit:", style = TypoStyle(FontColor.SECONDARY, FontSize.LARGE))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                val nav = LocalNavController.current
                val coroutineScope = rememberCoroutineScope()
                displayedPeople.forEach {
                    Chip(
                        {
                            coroutineScope.launch {
                                profileInfoModel.openPersonDetails(it.id, nav, db, context)
                            }
                        }
                    ) {
                        Text(
                            it.name,
                            style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM)
                        )
                    }
                }
                if(fullEvent.proposed is SocialEvent && fullEvent.proposed.more)
                    Chip(null, null, color = Color.Transparent) {
                        Text("+ Weitere", style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM))
                    }
            }
        }
        if (fullEvent.proposed is TravelEvent) {
            var from: Location? by remember { mutableStateOf(null) }
            var to: Location? by remember { mutableStateOf(null) }
            val size = FontSize.MEDIUM.size.dp
            LaunchedEffect(Unit) {
                with(Dispatchers.IO) {
                    db.location.getLocation(fullEvent.proposed.from)
                        ?.also { from = Location.from(it) }
                    db.location.getLocation(fullEvent.proposed.to)
                        ?.also { to = Location.from(it) }
                }
            }
            Column(
                Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Colors.SECONDARY, RoundedCornerShape(20))
                        .padding(20.dp)
                ) {
                    Text(
                        from?.name ?: "",
                        style = TypoStyle(FontColor.PRIMARY, FontSize.XLARGE)
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        from?.toAddress() ?: "",
                        style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        from?.toCords() ?: "",
                        style = TypoStyle(FontColor.SECONDARY, FontSize.SMALLM)
                    )
                    Spacer(Modifier.height(15.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            Modifier
                                .weight(1f)
                                .background(Colors.TERTIARY, CircleShape)
                                .clip(CircleShape)
                                .rippleClick {
                                    Location.openLink(
                                        context,
                                        HVV.constructLink(null, from)
                                            .apply { println(this) })
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
                                Modifier.height(FontSize.LARGE.size.dp),
                                Colors.SECONDARYFONT
                            )
                            Text(
                                "HVV App",
                                style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM)
                            )
                        }
                        Row(
                            Modifier
                                .weight(1f)
                                .background(Colors.TERTIARY, CircleShape)
                                .clip(CircleShape)
                                .rippleClick {
                                    Location.openInGoogleMaps(
                                        context,
                                        from
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
                                Modifier.height(FontSize.LARGE.size.dp),
                                Colors.SECONDARYFONT
                            )
                            Text(
                                "Google Maps",
                                style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM)
                            )
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            Modifier.clip(CircleShape)
                                .background(Colors.TERTIARY, CircleShape).rippleClick {
                                    Location.openRouteInGoogleMaps(
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
                                Modifier.size(size),
                                Colors.SECONDARYFONT
                            )
                        }
                        val calendar = remember { Calendar.getInstance() }
                        Box(
                            Modifier.clip(CircleShape)
                                .background(Colors.TERTIARY, CircleShape).rippleClick {
                                    Location.openLink(
                                        context, HVV.constructLink(
                                            from,
                                            to,
                                            if(fullEvent.proposed.end > System.currentTimeMillis())
                                                fullEvent.proposed.end.formatTimeStamp(calendar)
                                            else
                                                null
                                        )
                                    )
                                }.padding(5.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.hvv),
                                "HVV",
                                Modifier.size(size),
                                Colors.SECONDARYFONT
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        painterResource(R.drawable.arrow_right),
                        "to",
                        Modifier.height(size * 2).rotate(90f),
                        Colors.TERTIARY
                    )
                    Spacer(Modifier.width(5.dp))
                    TimedTagLikeBar(fullEvent.proposed.vehicles)
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Colors.SECONDARY, RoundedCornerShape(20))
                        .padding(20.dp)
                ) {
                    Text(
                        to?.name ?: "",
                        style = TypoStyle(FontColor.PRIMARY, FontSize.XLARGE)
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        to?.toAddress() ?: "",
                        style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        to?.toCords() ?: "",
                        style = TypoStyle(FontColor.SECONDARY, FontSize.SMALLM)
                    )
                    Spacer(Modifier.height(15.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            Modifier
                                .weight(1f)
                                .background(Colors.TERTIARY, CircleShape)
                                .clip(CircleShape)
                                .rippleClick {
                                    Location.openLink(
                                        context,
                                        HVV.constructLink(null, to)
                                            .apply { println(this) })
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
                                Modifier.height(FontSize.LARGE.size.dp),
                                Colors.SECONDARYFONT
                            )
                            Text(
                                "HVV App",
                                style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM)
                            )
                        }
                        Row(
                            Modifier
                                .weight(1f)
                                .background(Colors.TERTIARY, CircleShape)
                                .clip(CircleShape)
                                .rippleClick {
                                    Location.openInGoogleMaps(
                                        context,
                                        to
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
                                Modifier.height(FontSize.LARGE.size.dp),
                                Colors.SECONDARYFONT
                            )
                            Text(
                                "Google Maps",
                                style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionBar(smallActionClick: (()->Unit)?, smallContent: (@Composable ()->Unit)?, color: Color, onLargeClick: ()->Unit, largeContent: @Composable ()->Unit){
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val gradientEnd by animateColorAsState(color, tween(2000, easing = LinearEasing))
        if(smallContent!=null){
            Box(
                Modifier
                    .size(75.dp)
                    .background(Colors.SECONDARY, RoundedCornerShape(25))
                    .clip(RoundedCornerShape(25))
                    .rippleClick{
                        smallActionClick?.invoke()
                    }
                    .padding(22.5.dp)
                ,
                contentAlignment = Alignment.Center
            ) {
                smallContent()
            }
        }
        Box(
            Modifier
                .padding(bottom = 10.dp)
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Colors.SECONDARY, gradientEnd),
                        startX = 250f
                    ),
                    RoundedCornerShape(25)
                )
                .clip(RoundedCornerShape(25))
                .rippleClick{
                    onLargeClick()
                }
                .height(75.dp)
            ,
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ){
                largeContent()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T: TagLike> TimedTagLikeBar(tags: List<TimedTagLikeContainer<T>>){
    val size = FontSize.MEDIUM.size.dp
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        tags.forEach {
            Chip(
                spacing = 5.dp
            ) {
                Icon(
                    painterResource(it.type.drawable),
                    null,
                    Modifier.run {
                        if (it.type == Vehicle.Bus) width(2f * size).height(
                            size
                        ) else size(size)
                    },
                    Colors.SECONDARYFONT
                )
                Text(
                    TimeBasedVisualTransformation.toTransformed((it.durationMs / (60 * 1000L)).toString()),
                    style = TypoStyle(FontColor.PRIMARY, FontSize.MEDIUM)
                )
            }
        }
    }
}