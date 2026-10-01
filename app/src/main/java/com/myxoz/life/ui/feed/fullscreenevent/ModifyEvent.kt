package com.myxoz.life.ui.feed.fullscreenevent

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
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
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalScreens
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.android.integration.HVV
import com.myxoz.life.api.Syncable
import com.myxoz.life.api.syncables.LocationSyncable
import com.myxoz.life.api.syncables.ManualTransactionSyncable
import com.myxoz.life.api.syncables.TodoSyncable
import com.myxoz.life.events.additionals.DigSocPlatform
import com.myxoz.life.ui.ArrowDirection
import com.myxoz.life.ui.Chip
import com.myxoz.life.ui.NavPath
import com.myxoz.life.ui.RowChip
import com.myxoz.life.ui.drawArrowBehind
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.ui.theme.TypoStyleOld
import com.myxoz.life.utils.AndroidUtils
import com.myxoz.life.utils.filteredWith
import com.myxoz.life.utils.formatDayTime
import com.myxoz.life.utils.formatMinutesToVisual
import com.myxoz.life.utils.formatMsToDuration
import com.myxoz.life.utils.formatTimeStamp
import com.myxoz.life.utils.formatToDate
import com.myxoz.life.core.getSafeLong
import com.myxoz.life.utils.matchInstrinsicHeight
import com.myxoz.life.utils.matchesNormalized
import com.myxoz.life.utils.nullIfEmpty
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.utils.toLocalDate
import com.myxoz.life.utils.toPx
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import kotlin.collections.filter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModifyEvent(viewModel: InspectedEventViewModel){
    val nav = LocalNavController.current
    val syncedEvent by viewModel.event.collectAsState()
    val event = syncedEvent.raw
    val syncableRaw by viewModel.editedSyncable.collectAsState()
    val syncable = syncableRaw
    val setEventTo: (com.myxoz.life.storage.events.RawEvent)->Unit = {
        viewModel.setInspectedEventTo(syncedEvent.copy(rawEvent = it))
    }
    val setSyncableTo: (Syncable.FeedInstantEventSyncable)->Unit = {
        viewModel.setEditedSyncableTo(it)
    }
    if(syncable!=null) {
        syncable.ModifyEventInputs{ setSyncableTo(it) }
    } else {
        when (event) {
            is com.myxoz.life.storage.events.EmptyEvent -> {}
            is com.myxoz.life.storage.events.SleepEvent -> {}
            is com.myxoz.life.storage.events.SpontEvent -> {
                TagsBar(event.eventTags) {
                    setEventTo(
                        _root_ide_package_.com.myxoz.life.storage.events.SpontEvent(
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
                        _root_ide_package_.com.myxoz.life.storage.events.SpontEvent(
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

            is com.myxoz.life.storage.events.HobbyEvent -> {
                TagsBar(event.eventTags) {
                    setEventTo(
                        _root_ide_package_.com.myxoz.life.storage.events.HobbyEvent(
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
                        _root_ide_package_.com.myxoz.life.storage.events.HobbyEvent(
                            event.start,
                            event.end,
                            event.uss,
                            event.usl,
                            event.eventTags,
                            event.title,
                            it.nullIfEmpty()
                        )
                    )
                }
                InputField(event.title.ifEmpty { null }, "Titel") {
                    setEventTo(
                        _root_ide_package_.com.myxoz.life.storage.events.HobbyEvent(
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

            is com.myxoz.life.storage.events.WorkEvent -> {
                TagsBar(event.eventTags) {
                    setEventTo(
                        _root_ide_package_.com.myxoz.life.storage.events.WorkEvent(
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
                        _root_ide_package_.com.myxoz.life.storage.events.WorkEvent(
                            event.start,
                            event.end,
                            event.uss,
                            event.usl,
                            event.eventTags,
                            event.title,
                            it.nullIfEmpty()
                        )
                    )
                }
                InputField(event.title.ifEmpty { null }, "Titel") {
                    setEventTo(
                        _root_ide_package_.com.myxoz.life.storage.events.WorkEvent(
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

            is com.myxoz.life.storage.events.LearnEvent -> {
                TagsBar(event.eventTags) {
                    setEventTo(
                        _root_ide_package_.com.myxoz.life.storage.events.LearnEvent(
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
                        _root_ide_package_.com.myxoz.life.storage.events.LearnEvent(
                            event.start,
                            event.end,
                            event.uss,
                            event.usl,
                            event.eventTags,
                            event.title,
                            it.nullIfEmpty()
                        )
                    )
                }
                InputField(event.title.ifEmpty { null }, "Titel") {
                    setEventTo(
                        _root_ide_package_.com.myxoz.life.storage.events.LearnEvent(
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

            is com.myxoz.life.storage.events.SocialEvent -> {
                TagsBar(event.eventTags) {
                    setEventTo(
                        _root_ide_package_.com.myxoz.life.storage.events.SocialEvent(
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
                    nav.navigate(NavPath.Menu.CONTACTS)
                }) { people, more ->
                    setEventTo(
                        _root_ide_package_.com.myxoz.life.storage.events.SocialEvent(
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
                        _root_ide_package_.com.myxoz.life.storage.events.SocialEvent(
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

            is com.myxoz.life.storage.events.DigSocEvent -> {
                PersonBar(event.people, false, false, {
                    nav.navigate(NavPath.Menu.CONTACTS)
                }) { people, _ ->
                    setEventTo(
                        _root_ide_package_.com.myxoz.life.storage.events.DigSocEvent(
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
                        _root_ide_package_.com.myxoz.life.storage.events.DigSocEvent(
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
                        _root_ide_package_.com.myxoz.life.storage.events.DigSocEvent(
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
            is com.myxoz.life.storage.events.TimewasteEvent -> {
                TimeBasedTagLikeSelection(_root_ide_package_.com.myxoz.life.storage.events.additionals.TimewastePlatform.entries.toList(), event.timewastePlatforms) {
                    setEventTo(
                        _root_ide_package_.com.myxoz.life.storage.events.TimewasteEvent(
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
                        _root_ide_package_.com.myxoz.life.storage.events.TimewasteEvent(
                            event.start,
                            event.end,
                            event.uss,
                            event.usl,
                            event.timewastePlatforms,
                            it
                        )
                    )
                }
            }

            is com.myxoz.life.storage.events.TravelEvent -> {
                LocationBar(event.from) {
                    setEventTo(
                        _root_ide_package_.com.myxoz.life.storage.events.TravelEvent(
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
                        _root_ide_package_.com.myxoz.life.storage.events.TravelEvent(
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
                        _root_ide_package_.com.myxoz.life.storage.events.TravelEvent(
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
    }
    if (event.type != _root_ide_package_.com.myxoz.life.storage.events.additionals.EventType.Sleep && event.type != _root_ide_package_.com.myxoz.life.storage.events.additionals.EventType.Empty || syncable != null) HorizontalDivider(
        color = OldColors.SECONDARY,
        thickness = 3.dp
    )
    if(event.type == _root_ide_package_.com.myxoz.life.storage.events.additionals.EventType.Empty) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            @Composable
            fun SyncableChip(icon: Int, title: String, selected: Boolean, select: ()-> Unit){
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(Theme.tertiaryContainer)
                        .then(
                            if (selected) Modifier.border(
                                1.dp,
                                Theme.outline,
                                CircleShape
                            ) else Modifier
                        )
                        .padding(15.dp, 5.dp)
                        .height(IntrinsicSize.Min)
                        .rippleClick {
                            if(!selected) select()
                        },
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        painterResource(icon),
                        "Option",
                        Modifier
                            .matchInstrinsicHeight()
                        ,
                        Theme.onTertiaryContainer
                    )
                    Text(title, style = TypoStyle(Theme.onTertiaryContainer, FontSize.SMALL))
                }
            }
            SyncableChip(R.drawable.cash, "Echtgeld", syncable is ManualTransactionSyncable) {
                viewModel.setEditedSyncableTo(
                    ManualTransactionSyncable(
                        -1L,
                        false,
                        false,
                        0,
                        "",
                        viewModel.event.value.raw.start,
                        null
                    )
                )
            }
            SyncableChip(R.drawable.todos_icon, "Todo", syncable is TodoSyncable) {
                viewModel.setEditedSyncableTo(
                    TodoSyncable(
                        -1L,
                        "",
                        null,
                        false,
                        viewModel.event.value.raw.start,
                    )
                )
            }
        }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CalendarChip(
            _root_ide_package_.com.myxoz.life.storage.events.additionals.EventType.Sleep,
            event is com.myxoz.life.storage.events.SleepEvent
        ) {
            setEventTo(
                _root_ide_package_.com.myxoz.life.storage.events.SleepEvent(
                    event.start,
                    event.end,
                    event.uss,
                    event.usl
                )
            )
        }
        CalendarChip(
            _root_ide_package_.com.myxoz.life.storage.events.additionals.EventType.Spont,
            event is com.myxoz.life.storage.events.SpontEvent
        ) {
            setEventTo(
                _root_ide_package_.com.myxoz.life.storage.events.SpontEvent(
                    event.start, event.end, event.uss, event.usl,
                    if (event is com.myxoz.life.storage.events.additionals.TagEvent) ((event as com.myxoz.life.storage.events.additionals.TagEvent).eventTags) else listOf(),
                    if (event is com.myxoz.life.storage.events.additionals.TitleEvent) (event as com.myxoz.life.storage.events.additionals.TitleEvent).title else ""
                )
            )
        }
        CalendarChip(
            _root_ide_package_.com.myxoz.life.storage.events.additionals.EventType.Hobby,
            event is com.myxoz.life.storage.events.HobbyEvent
        ) {
            setEventTo(
                _root_ide_package_.com.myxoz.life.storage.events.HobbyEvent(
                    event.start, event.end, event.uss, event.usl,
                    if (event is com.myxoz.life.storage.events.additionals.TagEvent) ((event as com.myxoz.life.storage.events.additionals.TagEvent).eventTags) else listOf(),
                    if (event is com.myxoz.life.storage.events.additionals.TitleEvent) (event as com.myxoz.life.storage.events.additionals.TitleEvent).title else "",
                    if (event is com.myxoz.life.storage.events.additionals.DetailsEvent) (event as com.myxoz.life.storage.events.additionals.DetailsEvent).details else ""
                )
            )
        }
        CalendarChip(
            _root_ide_package_.com.myxoz.life.storage.events.additionals.EventType.Learn,
            event is com.myxoz.life.storage.events.LearnEvent
        ) {
            setEventTo(
                _root_ide_package_.com.myxoz.life.storage.events.LearnEvent(
                    event.start, event.end, event.uss, event.usl,
                    if (event is com.myxoz.life.storage.events.additionals.TagEvent) ((event as com.myxoz.life.storage.events.additionals.TagEvent).eventTags) else listOf(),
                    if (event is com.myxoz.life.storage.events.additionals.TitleEvent) (event as com.myxoz.life.storage.events.additionals.TitleEvent).title else "",
                    if (event is com.myxoz.life.storage.events.additionals.DetailsEvent) (event as com.myxoz.life.storage.events.additionals.DetailsEvent).details else ""
                )
            )
        }
        CalendarChip(
            _root_ide_package_.com.myxoz.life.storage.events.additionals.EventType.Social,
            event is com.myxoz.life.storage.events.SocialEvent
        ) {
            setEventTo(
                _root_ide_package_.com.myxoz.life.storage.events.SocialEvent(
                    event.start, event.end, event.uss, event.usl,
                    if (event is com.myxoz.life.storage.events.additionals.TagEvent) ((event as com.myxoz.life.storage.events.additionals.TagEvent).eventTags) else listOf(),
                    if (event is com.myxoz.life.storage.events.additionals.TitleEvent) (event as com.myxoz.life.storage.events.additionals.TitleEvent).title else "",
                    if (event is com.myxoz.life.storage.events.additionals.PeopleEvent) (event as com.myxoz.life.storage.events.additionals.PeopleEvent).people else listOf(),
                    false
                )
            )
        }
        CalendarChip(
            _root_ide_package_.com.myxoz.life.storage.events.additionals.EventType.Travel,
            event is com.myxoz.life.storage.events.TravelEvent
        ) {
            setEventTo(
                _root_ide_package_.com.myxoz.life.storage.events.TravelEvent(
                    event.start, event.end, event.uss, event.usl,
                    0L,
                    0L,
                    listOf(),
                )
            )
        }
        CalendarChip(
            _root_ide_package_.com.myxoz.life.storage.events.additionals.EventType.DigSoc,
            event is com.myxoz.life.storage.events.DigSocEvent
        ) {
            setEventTo(
                _root_ide_package_.com.myxoz.life.storage.events.DigSocEvent(
                    event.start, event.end, event.uss, event.usl,
                    listOf(),
                    if (event is com.myxoz.life.storage.events.additionals.TitleEvent) event.title else "",
                    if (event is com.myxoz.life.storage.events.additionals.PeopleEvent) event.people else listOf(),
                )
            )
        }
        CalendarChip(
            _root_ide_package_.com.myxoz.life.storage.events.additionals.EventType.Work,
            event is com.myxoz.life.storage.events.WorkEvent
        ) {
            setEventTo(
                _root_ide_package_.com.myxoz.life.storage.events.WorkEvent(
                    event.start, event.end, event.uss, event.usl,
                    if (event is com.myxoz.life.storage.events.additionals.TagEvent) event.eventTags else listOf(),
                    if (event is com.myxoz.life.storage.events.additionals.TitleEvent) event.title else "",
                    if (event is com.myxoz.life.storage.events.additionals.DetailsEvent) event.details else ""
                )
            )
        }
        CalendarChip(
            _root_ide_package_.com.myxoz.life.storage.events.additionals.EventType.Timewaste,
            event is com.myxoz.life.storage.events.TimewasteEvent
        ) {
            setEventTo(
                _root_ide_package_.com.myxoz.life.storage.events.TimewasteEvent(
                    event.start, event.end, event.uss, event.usl,
                    listOf(),
                    if (event is com.myxoz.life.storage.events.additionals.TitleEvent) (event as com.myxoz.life.storage.events.additionals.TitleEvent).title else ""
                )
            )
        }
    }
}

@Composable
fun CalendarChip(
    type: com.myxoz.life.storage.events.additionals.EventType,
    isSelected: Boolean,
    setBasicEvent: ()->Unit
) {
    val animationDuration = 500
    val dotSize = 10.dp
    val fontColor by animateColorAsState(if(isSelected) type.selectedColor else Theme.onTertiaryContainer, tween(animationDuration, easing = LinearEasing))
    val progress by animateFloatAsState(if(isSelected) 1f else 0f, tween(animationDuration, easing = EaseIn))
    val fontSize = FontSize.LARGE.size.toDp()
    val offsetX = (10.dp+dotSize).toPx()
    val offsetY = (10.dp+(fontSize+4.dp-dotSize)/2).toPx()
    val defaultSize = dotSize.toPx()
    Row(
        Modifier
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape((50 * (progress/2 + .5f)).toInt()))
            .background(Theme.tertiaryContainer, RoundedCornerShape(25))
            .drawBehind{
                val maxWidth = size.width
                drawCircle(
                    type.colors.bg,
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
fun InputField(
    defaultValue: String?,
    placeholder: String,
    focusRequester: FocusRequester?=null,
    onImeAction: (()-> ImeActionClicked)?=null,
    multiline: Boolean = false,
    background: Color = Theme.secondaryContainer,
    visualTransformation: VisualTransformation? = null,
    keyboardType: KeyboardType? = null,
    onChange: (String)->Unit
){
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
        textStyle = TypoStyle(Theme.primary, FontSize.LARGE),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = background,
            unfocusedContainerColor = background,
            cursorColor = Theme.primary,
        ),
        singleLine = !multiline,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done, keyboardType = keyboardType ?: KeyboardType.Unspecified),
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
        visualTransformation = visualTransformation ?: VisualTransformation.None,
        shape = RoundedCornerShape(10.dp),
        placeholder = {
            Text(placeholder, style = TypoStyle(Theme.secondary, FontSize.LARGE))
        }
    )
}

@Composable
fun TagsBar(ev: List<com.myxoz.life.storage.events.additionals.EventTag>, updateEvent: (List<com.myxoz.life.storage.events.additionals.EventTag>)->Unit){
    val tagsHeight = FontSize.SMALL.size.toDp() + 2.dp
    val selectedTags = remember { ev.toMutableStateList() }
    var search: String? by remember {
        mutableStateOf(null)
    }
    val allEventTags = _root_ide_package_.com.myxoz.life.storage.events.additionals.EventTag.entries.filter { it != _root_ide_package_.com.myxoz.life.storage.events.additionals.EventTag.S }
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
                        .background(Theme.primaryContainer, CircleShape)
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
                            tint = Theme.onPrimaryContainer,
                        )
                    }
                }
                Box(
                    Modifier
                        .padding(horizontal = 10.dp)
                        .background(Theme.outlineVariant, CircleShape)
                        .padding(horizontal = 1.dp)
                        .height(20.dp)
                )
            }
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .weight(1f)
                ,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leftItems.forEachIndexed{ i, it ->
                    RowChip ({
                        selectedTags.add(it)
                        updateEvent(selectedTags)
                    }, spacing = 3.dp, isFirst = i == 0, isLast = i == leftItems.size - 1) {
                        Icon(
                            painterResource(it.drawable),
                            it.displayName,
                            modifier = Modifier.height(tagsHeight),
                            tint = Theme.onSecondaryContainer,
                        )
                        val displayString = if(it.displayName.matchesNormalized(search?:""))
                            it.displayName
                        else
                            "${it.displayName} (${it.queryString.filteredWith(search?:"\na", {""}){ l -> l}.firstOrNull() ?:"NULL"})"
                        Text(displayString, style = TypoStyle(Theme.onSecondaryContainer, FontSize.SMALL))
                    }
                }
            }
        }
        InputField(null, "Tags", onImeAction = {
            return@InputField if(search!=null) {
                when (search) {
                    "xkwreS".removeRange(1..3).reversed() -> { selectedTags.add(_root_ide_package_.com.myxoz.life.storage.events.additionals.EventTag.S); updateEvent(selectedTags) }
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
/** Gets the "id"-key of a JSON object without ridiculous rounding errors */

@Composable
fun TimeBar(event: com.myxoz.life.storage.events.RawEvent, progress: Float = 0f, color: Color, setEventTo: (com.myxoz.life.storage.events.RawEvent) -> Unit){
    val screens = LocalScreens.current
    val verticalOffset = FontSize.SMALLM.size.toDp()
    Box(
        Modifier
            .padding(bottom = verticalOffset - 4.dp /* 2*2 */ - 2.dp + 4.dp /* Visual padding */)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp)
                .border(3.dp, Theme.outlineVariant, RoundedCornerShape(15.dp))
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
                val zone = ZoneId.systemDefault()
                val startTime = remember(event.start) {
                    event.start.formatDayTime(calendar)
                }
                val endTime = remember(event.end) {
                    event.end.formatDayTime(calendar)
                }
                val startDate = remember(event.start) {
                    event.start.toLocalDate(zone).formatToDate()
                }
                val endDate = remember(event.end) {
                    event.end.toLocalDate(zone).formatToDate()
                }
                Row {
                    Text(startTime, style = TypoStyle(Theme.primary, FontSize.LARGE, FontFamily.Display), modifier = Modifier.clickable(null, null) {
                        setEventTo(event.copyWithTimes(event.start, event.end, !event.uss, event.usl))
                    })
                    Text("~", color = if(!uss) Color.Transparent else Theme.primary, fontSize = FontSize.LARGE.size, fontFamily = FontFamily.Display.family)
                }
                Text(startDate, style = TypoStyle(Theme.secondary, FontSize.MEDIUMM), modifier = Modifier.clip(CircleShape).rippleClick{
                    screens.openCalendarAt(Instant.ofEpochMilli(event.start).atZone(ZoneId.systemDefault()).toLocalDate())
                }.padding(2.dp))
                Icon(painterResource(R.drawable.drop_down), "Till", Modifier.rotate(-90f), tint = Theme.tertiary)
                Text(endDate, style = TypoStyle(Theme.secondary, FontSize.MEDIUMM), modifier = Modifier.clip(CircleShape).rippleClick{
                    screens.openCalendarAt(Instant.ofEpochMilli(event.end).atZone(ZoneId.systemDefault()).toLocalDate())
                }.padding(2.dp))
                Row {
                    Text("~", color = if(!usl) Color.Transparent else Theme.primary, fontSize = FontSize.LARGE.size, fontFamily = FontFamily.Display.family)
                    Text(endTime, style = TypoStyle(Theme.primary, FontSize.LARGE, FontFamily.Display), modifier = Modifier.clickable(null, null) {
                        setEventTo(event.copyWithTimes(event.start, event.end, event.uss, !event.usl))
                    })
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Theme.tertiaryContainer, CircleShape)
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
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(y = verticalOffset - 4.dp /* 2*2 */ - 2.dp, x = (-15).dp)
                .background(Theme.outlineVariant, shape = RoundedCornerShape(0.dp, 0.dp, 10.dp, 10.dp))
        ) {
            Text(
                (event.end - event.start).formatMsToDuration(true),
                Modifier
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                ,
                style = TypoStyle(Theme.secondary, FontSize.MEDIUM, FontFamily.Display)
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
    val profileViewModel = LocalScreens.current.appRepos.crossRepoSuper
    val allPeople by profileViewModel.getAllPeopleFlow.collectAsState()
    val selectedPeople = remember { defaultSelectedPeople.toMutableStateList() }
    var more by remember { mutableStateOf(if(isMoreEvent) defaultMore else false) }
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
                        style = TypoStyle(Theme.onSecondaryContainer, FontSize.SMALL)
                    )
                }
            }
            if(isMoreEvent) Chip({
                more = !more
                updateEvent(selectedPeople, more)
            }) {
                Text(
                    "+ weitere",
                    style = TypoStyle(if(more) Theme.onSecondaryContainer else Theme.outline, FontSize.SMALL)
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Chip({
                openAddMenu()
            }) {
                Text(
                    "Neu",
                    style = TypoStyle(Theme.onSecondaryContainer, FontSize.SMALL)
                )
            }
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .weight(1f)
                ,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                leftItems.forEachIndexed { i, person ->
                    RowChip ({
                        selectedPeople.add(person.id)
                        updateEvent(selectedPeople, more)
                    },
                        isFirst = i == 0,
                        isLast = i == leftItems.size - 1
                    ) {
                        Text(
                            person.name,
                            style = TypoStyle(Theme.onSecondaryContainer, FontSize.SMALL)
                        )
                    }
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
    val screens = LocalScreens.current
    val allLocations by screens.appRepos.crossRepoSuper.allLocations.collectAsState()
    val decodedLocation: LocationSyncable? by remember {
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
    val coroutineScope = rememberCoroutineScope()
    val setLocationTo = { it: Long ->
        selectedLocation = it
        setLocation(it)
    }
    val editLocation = { it: LocationSyncable ->
        coroutineScope.launch {
            screens.editLocation(it)
        }
    }
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (decodedLocation != null) {
                Chip({
                    setLocationTo(0L)
                },{
                    editLocation(decodedLocation?:return@Chip)
                },
                    color = Theme.primaryContainer
                    ){
                    Text(
                        decodedLocation?.name ?: "NULL",
                        style = TypoStyle(Theme.onPrimaryContainer, FontSize.SMALL)
                    )
                }
            } else {
                Chip({
                    navController.navigate(NavPath.Menu.MAP)
                },
                    color = Theme.secondaryContainer
                ) {
                    Text(
                        "Neu",
                        style = TypoStyle(Theme.onSecondaryContainer, FontSize.SMALL)
                    )
                }
            }
            Box(
                Modifier
                    .padding(horizontal = 10.dp)
                    .background(Theme.outlineVariant, CircleShape)
                    .padding(horizontal = 1.dp)
                    .height(20.dp)
            )
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .weight(1f)
                ,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                leftItems.forEachIndexed { i, location ->
                    RowChip ({
                        setLocationTo(location.id)
                    }, {
                        editLocation(location)
                    },
                        isFirst = i == 0,
                        isLast = i == leftItems.size - 1
                    ) {
                        Text(
                            location.name,
                            style = TypoStyle(Theme.onSecondaryContainer, FontSize.SMALL)
                        )
                    }
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
fun VehicleSelection(
    defSelected: List<com.myxoz.life.storage.events.additionals.TimedTagLikeContainer<com.myxoz.life.storage.events.additionals.Vehicle>>,
    inspectViewModel: InspectedEventViewModel? = null,
    setVehiclesTo: (List<com.myxoz.life.storage.events.additionals.TimedTagLikeContainer<com.myxoz.life.storage.events.additionals.Vehicle>>) -> Unit
) {
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
                .drawArrowBehind(ArrowDirection.Down, iconHeight.toPx(density), Theme.primary)
        )
        Column(Modifier.fillMaxWidth()) {
            TimeBasedTagLikeSelection(_root_ide_package_.com.myxoz.life.storage.events.additionals.Vehicle.entries.toList(), defSelected) {
                setVehiclesTo(it)
            }
            Spacer(Modifier.height(5.dp))
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val calendar = remember { Calendar.getInstance() }
            val crossRepo = LocalScreens.current.appRepos.crossRepoSuper
            Row(
                Modifier
                    .clip(CircleShape)
                    .rippleClick{
                        coroutineScope.launch {
                            val event = inspectViewModel?.event?.value ?: return@launch
                            if(event.raw !is com.myxoz.life.storage.events.TravelEvent) return@launch
                            val from = crossRepo.getCachedLocation(event.raw.from)
                            val to = crossRepo.getCachedLocation(event.raw.to)
                            AndroidUtils.openLink(context, HVV.constructLink(from, to, (event.raw.start-15*1000L*60L).formatTimeStamp(calendar)))
                        }
                    }
                    .border(2.dp, Theme.outlineVariant, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                ,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(painterResource(R.drawable.hvv), "HVV", Modifier.size(FontSize.MEDIUM.size.toDp()), tint = Theme.primary)
                Text("Mit HVV App ermitteln", style = TypoStyleOld(FontColor.PRIMARY, FontSize.MEDIUM))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T: com.myxoz.life.storage.events.additionals.TagLike> TimeBasedTagLikeSelection(allSelectables: List<T>, defSelection: List<com.myxoz.life.storage.events.additionals.TimedTagLikeContainer<T>>, setVehiclesTo: (List<com.myxoz.life.storage.events.additionals.TimedTagLikeContainer<T>>) -> Unit){
    val iconHeight = FontSize.MEDIUM.size.toDp()
    val selectedTagLike = remember { defSelection.toMutableStateList() }
    val textMeasurer = rememberTextMeasurer()
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
                        .background(Theme.primaryContainer, CircleShape)
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
                        tint = Theme.onPrimaryContainer
                    )
                    val textWidth = with(LocalDensity.current){
                        textMeasurer.measure(
                            TimeBasedVisualTransformation.toTransformed(text),
                            style = TypoStyleOld(FontColor.PRIMARY, FontSize.MEDIUM),
                        ).size.width.toDp()
                    }
                    val focusManager = LocalFocusManager.current
                    BasicTextField(
                        if(layoutComplete) text else "LD",
                        {
                            text = it
                            val index = selectedTagLike.indexOf(item)
                            if(index==-1) return@BasicTextField
                            selectedTagLike[index] =
                                _root_ide_package_.com.myxoz.life.storage.events.additionals.TimedTagLikeContainer(
                                    item.type,
                                    TimeBasedVisualTransformation.displayMinutesToMinutes(it) * 60 * 1000L
                                )
                            setVehiclesTo(selectedTagLike)
                        },
                        textStyle = TypoStyle(Theme.onPrimaryContainer, FontSize.MEDIUM),
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
                        cursorBrush = SolidColor(Theme.onPrimaryContainer),
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
                        .background(Theme.secondaryContainer, CircleShape)
                        .clip(CircleShape)
                        .rippleClick{
                            selectedTagLike.add(
                                _root_ide_package_.com.myxoz.life.storage.events.additionals.TimedTagLikeContainer(
                                    item,
                                    0L
                                )
                            )
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
                        tint = Theme.onSecondaryContainer
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
            if(original == "0") return transformed.length - 1
            // 1005
            // to 10h 05m
            return transformed.length - when(original.length-offset){ // 0  is at the end
                0 -> 1
                1 -> 2
                2 -> if(original.length > 2) 5 else 3
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
            if(text == "0") return "m"
            return if(text.length > 2) "${text.take(text.length-2)}h ${text.takeLast(2)}m" else "${text.takeLast(2)}m"
        }
        fun displayMinutesToMinutes(text: String) = (text.toIntOrNull()?:0).let { minutes -> (minutes/100)*60+(minutes%100) }
    }
}