package com.myxoz.life.calendar

import android.icu.util.Calendar
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.core.content.edit
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalStorage
import com.myxoz.life.R
import com.myxoz.life.api.Location
import com.myxoz.life.api.PersonSyncable
import com.myxoz.life.calendar.feed.msToDisplay
import com.myxoz.life.events.DigSocEvent
import com.myxoz.life.events.EmptyEvent
import com.myxoz.life.events.HobbyEvent
import com.myxoz.life.events.LearnEvent
import com.myxoz.life.events.ProposedEvent
import com.myxoz.life.events.SleepEvent
import com.myxoz.life.events.SocialEvent
import com.myxoz.life.events.SpontEvent
import com.myxoz.life.events.TravelEvent
import com.myxoz.life.events.additionals.DetailsEvent
import com.myxoz.life.events.additionals.DigSocPlatform
import com.myxoz.life.events.additionals.EventTag
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.events.additionals.PeopleEvent
import com.myxoz.life.events.additionals.TagEvent
import com.myxoz.life.events.additionals.TagLike
import com.myxoz.life.events.additionals.TimedTagLikeContainer
import com.myxoz.life.events.additionals.TitleEvent
import com.myxoz.life.events.additionals.Vehicle
import com.myxoz.life.integration.HVV
import com.myxoz.life.ui.ArrowDirection
import com.myxoz.life.ui.Chip
import com.myxoz.life.ui.drawArrowBehind
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.AndroidUtils
import com.myxoz.life.utils.filteredWith
import com.myxoz.life.utils.formatMinutesToVisual
import com.myxoz.life.utils.formatTimeStamp
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.utils.toPx
import com.myxoz.life.viewmodels.InspectedEventViewModel
import com.myxoz.life.viewmodels.ProfileInfoModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModifyEvent(viewModel: InspectedEventViewModel, profileInfoModel: ProfileInfoModel){
    val nav = LocalNavController.current
    val db = LocalStorage.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val syncedEvent by viewModel.event.collectAsState()
    val event = syncedEvent.proposed
    val setEventTo: (ProposedEvent)->Unit = {
        viewModel.setInspectedEventTo(syncedEvent.copy(proposedEvent = it))
    }
    when (event) {
        is EmptyEvent -> {}
        is SleepEvent -> {}
        is SpontEvent -> {
            TagsBar(event.eventTags) {
                setEventTo(
                    SpontEvent(
                        event.start,
                        event.end,
                        event.uss,
                        event.usl,
                        it,
                        event.title
                    )
                )
            }
            InputField(event.title.ifEmpty { null }, "Titel") {
                setEventTo(
                    SpontEvent(
                        event.start,
                        event.end,
                        event.uss,
                        event.usl,
                        event.eventTags,
                        it
                    )
                )
            }
        }

        is HobbyEvent -> {
            TagsBar(event.eventTags) {
                setEventTo(
                    HobbyEvent(
                        event.start,
                        event.end,
                        event.uss,
                        event.usl,
                        it,
                        event.title,
                        event.details
                    )
                )
            }
            InputField(event.details?.ifEmpty { null }, "Details", multiline = true) {
                setEventTo(
                    HobbyEvent(
                        event.start,
                        event.end,
                        event.uss,
                        event.usl,
                        event.eventTags,
                        event.title,
                        it
                    )
                )
            }
            InputField(event.title.ifEmpty { null }, "Titel") {
                setEventTo(
                    HobbyEvent(
                        event.start,
                        event.end,
                        event.uss,
                        event.usl,
                        event.eventTags,
                        it,
                        event.details
                    )
                )
            }
        }

        is LearnEvent -> {
            TagsBar(event.eventTags) {
                setEventTo(
                    LearnEvent(
                        event.start,
                        event.end,
                        event.uss,
                        event.usl,
                        it,
                        event.title,
                        event.details
                    )
                )
            }
            InputField(event.details?.ifEmpty { null }, "Details", multiline = true) {
                setEventTo(
                    LearnEvent(
                        event.start,
                        event.end,
                        event.uss,
                        event.usl,
                        event.eventTags,
                        event.title,
                        it
                    )
                )
            }
            InputField(event.title.ifEmpty { null }, "Titel") {
                setEventTo(
                    LearnEvent(
                        event.start,
                        event.end,
                        event.uss,
                        event.usl,
                        event.eventTags,
                        it,
                        event.details
                    )
                )
            }
        }

        is SocialEvent -> {
            TagsBar(event.eventTags) {
                setEventTo(
                    SocialEvent(
                        event.start,
                        event.end,
                        event.uss,
                        event.usl,
                        it,
                        event.title,
                        event.people,
                        event.more
                    )
                )
            }
            PersonBar(event.people, event.more, true, {
                nav.navigate("contacts")
            }) { people, more ->
                setEventTo(
                    SocialEvent(
                        event.start,
                        event.end,
                        event.uss,
                        event.usl,
                        event.eventTags,
                        event.title,
                        people,
                        more
                    )
                )
            }
            InputField(event.title.ifEmpty { null }, "Titel") {
                setEventTo(
                    SocialEvent(
                        event.start,
                        event.end,
                        event.uss,
                        event.usl,
                        event.eventTags,
                        it,
                        event.people,
                        event.more
                    )
                )
            }
        }

        is DigSocEvent -> {
            PersonBar(event.people, false, false, {
                nav.navigate("contacts")
            }) { people, _ ->
                setEventTo(
                    DigSocEvent(
                        event.start,
                        event.end,
                        event.uss,
                        event.usl,
                        event.digSocEntries,
                        event.title,
                        people
                    )
                )
            }
            TimeBasedTagLikeSelection(DigSocPlatform.entries.toList(), event.digSocEntries) {
                setEventTo(
                    DigSocEvent(
                        event.start,
                        event.end,
                        event.uss,
                        event.usl,
                        it,
                        event.title,
                        event.people
                    )
                )
            }
            InputField(event.title.ifEmpty { null }, "Titel") {
                setEventTo(
                    DigSocEvent(
                        event.start,
                        event.end,
                        event.uss,
                        event.usl,
                        event.digSocEntries,
                        it,
                        event.people
                    )
                )
            }
        }

        is TravelEvent -> {
            LocationBar(event.from) {
                setEventTo(
                    TravelEvent(
                        event.start,
                        event.end,
                        event.uss,
                        event.usl,
                        it,
                        event.to,
                        event.vehicles
                    )
                )
            }
            VehicleSelection(event.vehicles, viewModel) {
                setEventTo(
                    TravelEvent(
                        event.start,
                        event.end,
                        event.uss,
                        event.usl,
                        event.from,
                        event.to,
                        it
                    )
                )
            }
            LocationBar(event.to) {
                setEventTo(
                    TravelEvent(
                        event.start,
                        event.end,
                        event.uss,
                        event.usl,
                        event.from,
                        it,
                        event.vehicles
                    )
                )
            }
        }
    }
    if (event.type != EventType.Sleep && event.type != EventType.Empty) HorizontalDivider(
        color = Colors.SECONDARY,
        thickness = 3.dp
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CalendarChip(
            EventType.Sleep,
            event is SleepEvent
        ) {
            setEventTo(SleepEvent(event.start, event.end, event.uss, event.usl))
        }
        CalendarChip(
            EventType.Spont,
            event is SpontEvent
        ) {
            setEventTo(
                SpontEvent(
                    event.start, event.end, event.uss, event.usl,
                    if (event is TagEvent) ((event as TagEvent).eventTags) else listOf(),
                    if (event is TitleEvent) (event as TitleEvent).title else ""
                )
            )
        }
        CalendarChip(
            EventType.Hobby,
            event is HobbyEvent
        ) {
            setEventTo(
                HobbyEvent(
                    event.start, event.end, event.uss, event.usl,
                    if (event is TagEvent) ((event as TagEvent).eventTags) else listOf(),
                    if (event is TitleEvent) (event as TitleEvent).title else "",
                    if (event is DetailsEvent) (event as DetailsEvent).details else ""
                )
            )
        }
        CalendarChip(
            EventType.Learn,
            event is LearnEvent
        ) {
            setEventTo(
                LearnEvent(
                    event.start, event.end, event.uss, event.usl,
                    if (event is TagEvent) ((event as TagEvent).eventTags) else listOf(),
                    if (event is TitleEvent) (event as TitleEvent).title else "",
                    if (event is DetailsEvent) (event as DetailsEvent).details else ""
                )
            )
        }
        CalendarChip(
            EventType.Social,
            event is SocialEvent
        ) {
            setEventTo(
                SocialEvent(
                    event.start, event.end, event.uss, event.usl,
                    if (event is TagEvent) ((event as TagEvent).eventTags) else listOf(),
                    if (event is TitleEvent) (event as TitleEvent).title else "",
                    if (event is PeopleEvent) (event as PeopleEvent).people else listOf(),
                    false
                )
            )
        }
        CalendarChip(
            EventType.Travel,
            event is TravelEvent
        ) {
            setEventTo(
                TravelEvent(
                    event.start, event.end, event.uss, event.usl,
                    0L,
                    0L,
                    listOf(),
                )
            )
        }
        CalendarChip(
            EventType.DigSoc,
            event is DigSocEvent
        ) {
            setEventTo(
                DigSocEvent(
                    event.start, event.end, event.uss, event.usl,
                    listOf(),
                    if (event is TitleEvent) (event as TitleEvent).title else "",
                    if (event is PeopleEvent) (event as PeopleEvent).people else listOf(),
                )
            )
        }
    }
}

@Composable
fun CalendarChip(
    type: EventType,
    isSelected: Boolean,
    setBasicEvent: ()->Unit
) {
    val animationDuration = 500
    val dotSize = 10.dp
    val fontColor by animateColorAsState(if(isSelected) type.selectedColor else Colors.SECONDARYFONT, tween(animationDuration, easing = LinearEasing))
    val progress by animateFloatAsState(if(isSelected) 1f else 0f, tween(animationDuration, easing = EaseIn))
    val fontSize = FontSize.LARGE.size.toDp()
    val offsetX = (10.dp+dotSize).toPx()
    val offsetY = (10.dp+(fontSize+4.dp-dotSize)/2).toPx()
    val defaultSize = dotSize.toPx()
    Row(
        Modifier
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(25))
            .background(Colors.SECONDARY, RoundedCornerShape(25))
            .drawBehind{
                val maxWidth = size.width
                drawCircle(
                    type.color,
                    (maxWidth*progress)+defaultSize/2,
                    Offset(offsetX, offsetY)
                )
            }
            .rippleClick{
                setBasicEvent()
            }
            .padding(10.dp, 5.dp)
            .height(fontSize+4.dp)
            .padding(start = dotSize*2, end = dotSize)
        ,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            type.calendarName,
            color = fontColor,
            fontSize = FontSize.LARGE.size,
            modifier = Modifier.offset(x = -progress*dotSize/2)
        )
    }
}
@Composable
fun InputField(defaultValue: String?, placeholder: String, focusRequester: FocusRequester?=null, onImeAction: (()-> ImeActionClicked)?=null, multiline: Boolean = false, onChange: (String)->Unit){
    var value by remember(defaultValue, placeholder) {
        mutableStateOf(defaultValue?:"")
    }
    val focusManager = LocalFocusManager.current
    TextField(
        value,
        {
            @Suppress("AssignedValueIsNeverRead") // This is wrong
            value = it
            onChange(it)
        },
        Modifier
            .let{ if(focusRequester!=null) it.focusRequester(focusRequester) else it }
            .fillMaxWidth(),
        textStyle = TypoStyle(FontColor.PRIMARY, FontSize.LARGE),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = Colors.SECONDARY,
            unfocusedContainerColor = Colors.SECONDARY,
            cursorColor = Colors.PRIMARYFONT
        ),
        singleLine = !multiline,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions{
            if(onImeAction!=null) {
                val imeAction = onImeAction()
                if(imeAction.clearField) {
                    @Suppress("AssignedValueIsNeverRead") // Also wrong
                    value = ""; onChange("")}
                if(imeAction.unfocusField) focusManager.clearFocus()
            } else {
                focusManager.clearFocus()
            }
        },
        shape = RoundedCornerShape(10.dp),
        placeholder = {
            Text(placeholder, style = TypoStyle(FontColor.SECONDARY, FontSize.LARGE))
        }
    )
}

@Composable
fun TagsBar(ev: List<EventTag>, updateEvent: (List<EventTag>)->Unit){
    val tagsHeight = FontSize.SMALL.size.toDp() + 2.dp
    val selectedTags = remember { ev.toMutableStateList() }
    var search: String? by remember {
        mutableStateOf(null)
    }
    val allEventTags = EventTag.entries.filter { it != EventTag.P && it != EventTag.S }
    val leftItems by remember {
        derivedStateOf {
            val tags = allEventTags.filter {
                !selectedTags.contains(it)
            }
            val search = search
            if(search != null)
                tags.filteredWith(search, {it.queryString.joinToString(";")}){it.displayName}
            else
                tags
        }
    }
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if(selectedTags.isNotEmpty()) {
                Row(
                    Modifier
                        .background(Colors.SECONDARY, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    selectedTags.forEach {
                        Icon(
                            painterResource(it.drawable),
                            it.displayName,
                            modifier = Modifier
                                .height(tagsHeight)
                                .clickable(null, null){
                                    selectedTags.remove(it)
                                    updateEvent(selectedTags)
                                },
                            tint = Colors.PRIMARYFONT
                        )
                    }
                }
                Box(
                    Modifier
                        .padding(horizontal = 10.dp)
                        .background(Colors.SECONDARY, CircleShape)
                        .padding(horizontal = 1.dp)
                        .height(20.dp)
                )
            }
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .weight(1f)
                ,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leftItems.forEach{
                    Chip({
                        selectedTags.add(it)
                        updateEvent(selectedTags)
                    }, spacing = 3.dp) {
                        Icon(
                            painterResource(it.drawable),
                            it.displayName,
                            modifier = Modifier.height(tagsHeight),
                            tint = Colors.SECONDARYFONT
                        )
                        val displayString = if(it.displayName.lowercase().contains(search?.lowercase()?:""))
                            it.displayName
                        else
                            "${it.displayName} (${it.queryString.filteredWith(search?:"\na", {""}){ l -> l}.firstOrNull() ?:"NULL"})"
                        Text(displayString, style = TypoStyle(FontColor.SECONDARY, FontSize.SMALL))
                    }
                }
            }
        }
        InputField(null, "Tags", onImeAction = {
            return@InputField if(search!=null) {
                when (search) {
                    "nrfedoP".removeRange(2..4).reversed() -> selectedTags.add(EventTag.P)
                    "xkwreS".removeRange(1..3).reversed() -> selectedTags.add(EventTag.S)
                    else -> leftItems.getOrNull(0)?.apply { selectedTags.add(this); updateEvent(selectedTags) }
                }
                ImeActionClicked(false, clearField = true)
            } else {
                ImeActionClicked(true, clearField = false)
            }
        }) {
            search = it.ifEmpty { null }
        }
    }
}
data class ImeActionClicked(val unfocusField: Boolean, val clearField: Boolean)
fun JSONObject.getEventId() = getString("id").toLong()

@Composable
fun TimeBar(event: ProposedEvent, progress: Float = 0f, color: Color, openDay: (LocalDate)->Unit, setEventTo: (ProposedEvent) -> Unit){
    Box(
        Modifier
            .fillMaxWidth(.95f)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp)
                .border(3.dp, Colors.TERTIARY, RoundedCornerShape(15.dp))
                .padding(vertical = 3.dp)
                .clip(RoundedCornerShape(25))
            ,
        ){
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(15.dp, 10.dp)
                ,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val uss = event.uss
                val usl = event.usl
                val calendar = remember {
                    Calendar.getInstance()
                }
                val startTime = remember {
                    calendar.timeInMillis = event.start
                    calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2,'0')+":"+
                            calendar.get(Calendar.MINUTE).toString().padStart(2,'0')
                }
                val endTime = remember {
                    calendar.timeInMillis = event.end
                    calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2,'0')+":"+
                            calendar.get(Calendar.MINUTE).toString().padStart(2,'0')
                }
                val startDate = remember {
                    calendar.timeInMillis = event.start
                    calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2,'0')+"."+
                            (calendar.get(Calendar.MONTH) + 1).toString().padStart(2,'0')+"."+
                            calendar.get(Calendar.YEAR)
                }
                val endDate = remember {
                    calendar.timeInMillis = event.end
                    calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2,'0')+"."+
                            (calendar.get(Calendar.MONTH) + 1).toString().padStart(2,'0')+"."+
                            calendar.get(Calendar.YEAR)
                }
                Row {
                    Text(startTime, style = TypoStyle(FontColor.PRIMARY, FontSize.LARGE), modifier = Modifier.clickable(null, null) {
                        setEventTo(event.copyWithTimes(event.start, event.end, !event.uss, event.usl))
                    })
                    Text("~", color = if(!uss) Color.Transparent else Colors.PRIMARYFONT, fontSize = FontSize.LARGE.size)
                }
                Text(startDate, style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM), modifier = Modifier.clip(CircleShape).rippleClick{
                    openDay(Instant.ofEpochMilli(event.start).atZone(ZoneId.systemDefault()).toLocalDate())
                }.padding(5.dp))
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Till", tint = Colors.SECONDARYFONT)
                Text(endDate, style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM), modifier = Modifier.clip(CircleShape).rippleClick{
                    openDay(Instant.ofEpochMilli(event.end).atZone(ZoneId.systemDefault()).toLocalDate())
                }.padding(5.dp))
                Row {
                    Text("~", color = if(!usl) Color.Transparent else Colors.PRIMARYFONT, fontSize = FontSize.LARGE.size)
                    Text(endTime, style = TypoStyle(FontColor.PRIMARY, FontSize.LARGE), modifier = Modifier.clickable(null, null) {
                        setEventTo(event.copyWithTimes(event.start, event.end, event.uss, !event.usl))
                    })
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Colors.SECONDARY, CircleShape)
                    .height(6.dp)
            ){
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .background(
                            if(!event.usl)
                                SolidColor(color)
                            else
                                Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.7f), color.copy(alpha = 0.0f)))
                            , CircleShape)
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                )
            }
        }
        val verticalOffset = FontSize.SMALLM.size.toDp()
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(y = verticalOffset - 4.dp - 5.dp, x = (-15).dp)
                .background(Colors.TERTIARY, shape = RoundedCornerShape(0.dp, 0.dp, 10.dp, 10.dp))
        ) {
            Text(
                (event.end - event.start).toInt().msToDisplay(true),
                Modifier
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                ,
                style = TypoStyle(FontColor.PRIMARY, FontSize.SMALLM)
            )
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonBar(
    defaultSelectedPeople: List<Long>,
    defaultMore: Boolean,
    isMoreEvent: Boolean,
    openAddMenu: ()->Unit,
    updateEvent: (List<Long>, Boolean)->Unit
){
    val db = LocalStorage.current
    val selectedPeople = remember { defaultSelectedPeople.toMutableStateList() }
    val allPeople = remember { mutableStateListOf<PersonSyncable>() }
    var more by remember { mutableStateOf(if(isMoreEvent) defaultMore else false) }
    LaunchedEffect(Unit) {
        val people = db.people.getAllPeople()
        allPeople.clear()
        allPeople.addAll(
            people.map{
                PersonSyncable.from(db, it)
            }
        )
    }
    var search: String? by remember {
        mutableStateOf(null)
    }
    val leftItems by remember {
        derivedStateOf {
            val people = allPeople.filter {
                !selectedPeople.contains(it.id)
            }
            val search = search
            if(search != null)
                people.filteredWith(search, {it.fullName?:""}){it.name}
            else
                people
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
            selectedPeople.forEach { person ->
                val decPerson = allPeople.firstOrNull { it.id == person }
                Chip({
                    selectedPeople.remove(person)
                    updateEvent(selectedPeople, more)
                }) {
                    Text(
                        decPerson?.name?:"NULL",
                        style = TypoStyle(FontColor.PRIMARY, FontSize.SMALL)
                    )
                }
            }
            if(isMoreEvent) Chip({
                more = !more
                updateEvent(selectedPeople, more)
            }) {
                Text(
                    "+ weitere",
                    style = TypoStyle(if(more) FontColor.PRIMARY else FontColor.SECONDARY, FontSize.SMALL)
                )
            }
        }
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Chip({
                openAddMenu()
            }) {
                Text(
                    "Neu",
                    style = TypoStyle(FontColor.PRIMARY, FontSize.SMALL)
                )
            }
            leftItems.forEach { person ->
                Chip({
                    selectedPeople.add(person.id)
                    updateEvent(selectedPeople, more)
                }) {
                    Text(
                        person.name,
                        style = TypoStyle(FontColor.SECONDARY, FontSize.SMALL)
                    )
                }
            }
        }
        InputField(null, "Name", onImeAction = {
            return@InputField if(search!=null) {
                 leftItems.getOrNull(0)?.apply { selectedPeople.add(this.id); updateEvent(selectedPeople, more) }
                ImeActionClicked(false, clearField = true)
            } else {
                ImeActionClicked(true, clearField = false)
            }
        }) {
            search = it.ifEmpty { null }
        }
    }
}

@Composable
fun LocationBar(defaultLocation: Long, setLocation: (Long)->Unit){
    var selectedLocation by remember { mutableLongStateOf(defaultLocation) }
    val navController = LocalNavController.current
    val storage =  LocalStorage.current
    val allLocations by produceState(initialValue = emptyList()) {
        val locations = storage.location.getAllLocations()
        value = locations.map { Location.from(it) }
    }
    val decodedLocation: Location? by remember {
        derivedStateOf {
            allLocations.find { it.id == selectedLocation }
        }
    }
    var search: String? by remember {
        mutableStateOf(null)
    }
    val leftItems by remember {
        derivedStateOf {
            val locs = allLocations.filter {
                it.id != selectedLocation
            }
            val search = search
            if(search != null)
                locs.filteredWith(search, {it.toAddress(true)}){ it.name }
            else
                locs
        }
    }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val setLocationTo = { it: Long ->
        selectedLocation = it
        setLocation(it)
        coroutineScope.launch {
            scrollState.animateScrollTo(0, tween(300))
        }
    }
    val editLocation = { it: Location ->
        coroutineScope.launch {
            val location = it.toJson(storage)?.toString()  ?: return@launch
            storage.prefs.edit {
                putString("editing_location", location)
            }
            navController.navigate("modify_event/add_location")
        }
    }
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier
                .horizontalScroll(scrollState)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (decodedLocation != null) {
                Chip({
                    setLocationTo(0L)
                },{
                    editLocation(decodedLocation?:return@Chip)
                }){
                    Text(
                        decodedLocation?.name ?: "NULL",
                        style = TypoStyle(FontColor.PRIMARY, FontSize.SMALL)
                    )
                }
            } else {
                Chip({
                    navController.navigate("modify_event/add_location")
                }) {
                    Text(
                        "Neu",
                        style = TypoStyle(FontColor.PRIMARY, FontSize.SMALL)
                    )
                }
            }
            leftItems.forEach { location ->
                Chip({
                    setLocationTo(location.id)
                }, {
                    editLocation(location)
                }) {
                    Text(
                        location.name,
                        style = TypoStyle(FontColor.SECONDARY, FontSize.SMALL)
                    )
                }
            }
        }
        InputField(null, decodedLocation?.name ?: "Standort", onImeAction = {
            return@InputField if(search!=null) {
                 leftItems.getOrNull(0)?.apply { setLocationTo(this.id) }
                ImeActionClicked(true, clearField = true)
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
fun VehicleSelection(defSelected: List<TimedTagLikeContainer<Vehicle>>, inspectViewModel: InspectedEventViewModel? = null, setVehiclesTo: (List<TimedTagLikeContainer<Vehicle>>) -> Unit){
    val iconHeight = FontSize.MEDIUM.size.toDp()
    val density = LocalDensity.current
    Row(
        Modifier
            .height(IntrinsicSize.Min)
            .padding(end = 20.dp)
        ,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(iconHeight*3)
                .drawArrowBehind(ArrowDirection.Down, iconHeight.toPx(density), Colors.SECONDARY)
        )
        Column(Modifier.fillMaxWidth()) {
            TimeBasedTagLikeSelection(Vehicle.entries.toList(), defSelected) {
                setVehiclesTo(it)
            }
            Spacer(Modifier.height(5.dp))
            val context = LocalContext.current
            val db = LocalStorage.current
            val coroutineScope = rememberCoroutineScope()
            val calendar = remember { Calendar.getInstance() }
            Row(
                Modifier
                    .clip(CircleShape)
                    .rippleClick{
                        coroutineScope.launch {
                            val event = inspectViewModel?.event?.value ?: return@launch
                            if(event.proposed !is TravelEvent) return@launch
                            val from = Location.fromDB(db, event.proposed.from)
                            val to = Location.fromDB(db, event.proposed.to)
                            AndroidUtils.openLink(context, HVV.constructLink(from, to, (event.proposed.start-15*1000L*60L).formatTimeStamp(calendar)))
                        }
                    }
                    .border(2.dp, Colors.TERTIARY, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                ,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(painterResource(R.drawable.hvv), "HVV", Modifier.size(FontSize.MEDIUM.size.toDp()), tint = Colors.PRIMARYFONT)
                Text("Mit HVV App ermitteln", style = TypoStyle(FontColor.PRIMARY, FontSize.MEDIUM))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T: TagLike> TimeBasedTagLikeSelection(allSelectables: List<T>, defSelection: List<TimedTagLikeContainer<T>>, setVehiclesTo: (List<TimedTagLikeContainer<T>>) -> Unit){
    val iconHeight = FontSize.MEDIUM.size.toDp()
    val selectedTagLike = remember { defSelection.toMutableStateList() }
    val textMessurer = rememberTextMeasurer()
    var itemWasAdded by remember { mutableStateOf(false) }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        selectedTagLike.forEach { item ->
            key(item.type){
                val focusRequester = remember {
                    FocusRequester()
                }
                Row(
                    Modifier
                        .background(Colors.SECONDARY, CircleShape)
                        .clip(CircleShape)
                        .rippleClick{
                            selectedTagLike.removeIf { it.type == item.type}
                            setVehiclesTo(selectedTagLike)
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                    ,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ){
                    var text by remember { mutableStateOf((item.durationMs/(60*1000L)).takeIf { it!=0L }?.formatMinutesToVisual()?:"") }
                    var layoutComplete by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        if(itemWasAdded) focusRequester.requestFocus()
                    }
                    Icon(
                        painterResource(item.type.drawable),
                        null,
                        Modifier.height(iconHeight),
                        tint = Colors.PRIMARYFONT
                    )
                    val textWidth = with(LocalDensity.current){
                        textMessurer.measure(
                            TimeBasedVisualTransformation.toTransformed(text),
                            style = TypoStyle(FontColor.PRIMARY, FontSize.MEDIUM),
                        ).size.width.toDp()
                    }
                    val focusManager = LocalFocusManager.current
                    BasicTextField(
                        if(layoutComplete) text else "LD",
                        {
                            text = it
                            val index = selectedTagLike.indexOf(item)
                            if(index==-1) return@BasicTextField
                            selectedTagLike[index] = TimedTagLikeContainer(
                                item.type,
                                (it.toIntOrNull()?:0).let { minutes ->
                                    (minutes/100)*60+(minutes%100)
                                }*60*1000L
                            )
                            setVehiclesTo(selectedTagLike)
                        },
                        textStyle = TypoStyle(FontColor.PRIMARY, FontSize.MEDIUM),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions{
                            focusManager.clearFocus(true)
                        },
                        modifier = Modifier
                            .height(iconHeight)
                            .focusRequester(focusRequester)
                            .offset(y = if(layoutComplete) iconHeight/8 else 0.dp)
                            .width(if(textWidth < 1.dp) 1.dp else textWidth),
                        cursorBrush = SolidColor(Colors.PRIMARYFONT),
                        visualTransformation = TimeBasedVisualTransformation(),
                        onTextLayout = { layoutComplete = true }
                    )
                }
            }
        }
        allSelectables.filter { selectable -> selectedTagLike.none{it.type==selectable} }.forEach { item ->
            key(item){
                Row(
                    Modifier
                        .background(Colors.SECONDARY, CircleShape)
                        .clip(CircleShape)
                        .rippleClick{
                            selectedTagLike.add(TimedTagLikeContainer(
                                item,
                                0L
                            ))
                            setVehiclesTo(selectedTagLike)
                            itemWasAdded=true
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                    ,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ){
                    Icon(
                        painterResource(item.drawable),
                        null,
                        Modifier.height(iconHeight),
                        tint = Colors.SECONDARYFONT
                    )
                }
            }
        }
    }
}

class TimeBasedVisualTransformation: VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val anotatedText = toTransformed(text.text)
        return TransformedText(
            AnnotatedString(
                anotatedText
            ),
            TimeBasedOffsetMapping(anotatedText, text.text)
        )
    }
    class TimeBasedOffsetMapping(val transformed: String, val original: String): OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            // 1005
            // to 10h 05m
            return transformed.length - when(original.length-offset){ // 0  is at the end
                0 -> 1
                1 -> 2
                2 -> if(original.length > 2) 5 else 2
                else -> 3+original.length-offset
            }
        }

        override fun transformedToOriginal(offset: Int): Int {
            // 10h 05m
            // to 1005
            return original.length - when(transformed.length-offset){ // 0  is at the end
                0 -> 0 // 10h 05m| -> 1005|
                1 -> 0 // 10h 05|m -> 1005|
                2 -> 1 // 10h 0|5m -> 100|5
                3 -> 2 // 10h |05m -> 10|05
                4 -> 2 // 10h| 05m -> 10|05
                5 -> 2 // 10|h 05m -> 10|05
                else -> transformed.length-offset - 3
            }
        }
    }
    companion object {
        fun toTransformed(text: String): String{
            return if(text.length > 2) "${text.take(text.length-2)}h ${text.takeLast(2)}m" else "${text.takeLast(2)}m"
        }
    }
}