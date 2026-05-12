package com.myxoz.life.screens.feed.fullscreenevent

import android.icu.util.Calendar
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalColors
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalScreens
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.api.Syncable
import com.myxoz.life.api.extensions.RepeatingEventsExtendable
import com.myxoz.life.api.extensions.RepeatingEventsExtendable.RepeatType.Daily
import com.myxoz.life.api.extensions.RepeatingEventsExtendable.RepeatType.Monthly
import com.myxoz.life.api.extensions.RepeatingEventsExtendable.RepeatType.Weekly
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.screens.NavPath
import com.myxoz.life.ui.ActionBar
import com.myxoz.life.ui.LifeProgressIndicator
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.ui.theme.TypoStyleOld
import com.myxoz.life.utils.animateColorSchemeAsState
import com.myxoz.life.utils.collectAsMutableState
import com.myxoz.life.utils.def
import com.myxoz.life.utils.formatTimeStamp
import com.myxoz.life.utils.rememberColorScemeFromColor
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.InspectedEventViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Composable
fun FullScreenEvent(inspectedEventViewModel: InspectedEventViewModel) {
    val event by inspectedEventViewModel.event.collectAsState()
    val isEditing by inspectedEventViewModel.isEditing.collectAsState()
    val colorScheme = rememberColorScemeFromColor(event.raw.type.colors.bg, event)
    val animatedColorScheme = animateColorSchemeAsState(colorScheme)
    CompositionLocalProvider(
        LocalColors provides animatedColorScheme,
        LocalTextSelectionColors provides TextSelectionColors(animatedColorScheme.primary,animatedColorScheme.primary.copy(alpha = 0.4f))
    ) {
        Box(
            Modifier
                .background(Theme.background)
                .fillMaxSize()
            ,
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                Modifier
                    .padding(windowPadding)
                    .setMaxTabletWidth()
                ,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val syncable by inspectedEventViewModel.editedSyncable.collectAsState()
                if(syncable == null) {
                    val now by inspectedEventViewModel.timeFlow.collectAsState(System.currentTimeMillis())
                    TimeBar(
                        event.raw,
                        if(now >= event.raw.start) if(now <= event.raw.end) (now-event.raw.start) / (event.raw.end-event.raw.start).toFloat() else 1f else 0f,
                        Theme.surfaceContainer
                    ) {
                        if(isEditing) inspectedEventViewModel.setInspectedEventTo(event.copy(rawEvent = it))
                    }
                }
                Box(
                    Modifier.weight(1f),
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        !isEditing,
                        enter = fadeIn() + slideInHorizontally { it/2 },
                        exit = fadeOut() + slideOutHorizontally { -it/2 }
                    ) {
                       DisplayFullScreenEvent(inspectedEventViewModel, event)
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        isEditing,
                        enter = fadeIn() + slideInHorizontally { it/2 },
                        exit = fadeOut() + slideOutHorizontally { -it/2 }
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState(), reverseScrolling = true)
                            ,
                            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)
                        ) {
                            ModifyEvent(inspectedEventViewModel)
                        }
                    }
                }
                BottomButton(inspectedEventViewModel, syncable, event, isEditing)
            }
            RepeatingEventPopup(inspectedEventViewModel, event)
        }
    }
}

@Composable
private fun BoxScope.RepeatingEventPopup(viewModel: InspectedEventViewModel, event: SyncedEvent) {
    var visible by viewModel.showRepeatPopup.collectAsMutableState()
    BackHandler(visible) {
        visible = !visible
    }
    AnimatedVisibility(
        visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Theme.background.copy(.5f))
                .clickable(null, null) {
                    viewModel.showRepeatPopup.update { false }
                }
        )
    }
    AnimatedVisibility(
        visible,
        Modifier
            .align(Alignment.Center)
            .pointerInput(Unit) {}
        ,
        enter = fadeIn() + scaleIn(initialScale = .9f),
        exit = fadeOut() + scaleOut(targetScale = .9f)
    ) {
        val repeatingEvents by viewModel.repeatingEvents.collectAsState(null)
        val associated by remember {
            derivedStateOf {
                repeatingEvents?.events?.find {
                    event.id in it.associatedEvents
                }
            }
        }
        var type by remember(associated) { mutableStateOf(associated?.repeat) }
        Column(
            Modifier
                .shadow(10.dp, RoundedCornerShape(20.dp), false)
                .background(Theme.secondaryContainer, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .width(IntrinsicSize.Max)
        ) {
            Text(
                "Wähle einen Wiederholungstyp",
                style = TypoStyle(
                    Theme.primary,
                    FontSize.LARGE
                )
            )
            @Composable
            fun Item(isSelected: Boolean, defValue: Int?, prefixLabel: String, suffixLabel: String, onSelect: (int: Int)-> RepeatingEventsExtendable.RepeatType){
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    var value by remember { mutableStateOf(defValue.def(1).toString()) }
                    RadioButton(isSelected, {
                        type = onSelect(value.toIntOrNull()?:1)
                    },
                        colors = RadioButtonDefaults.colors().copy(Theme.primary, Theme.outlineVariant)
                    )
                    Text(prefixLabel, style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
                    val secondary = Theme.secondary
                    BasicTextField(
                        value,
                        {
                            value = it
                            type = onSelect(value.toIntOrNull()?:1)
                        },
                        Modifier
                            .width(20.dp)
                            .drawWithContent {
                                drawContent()
                                drawLine(
                                    color = secondary,
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 1f,
                                )
                            },
                        textStyle = TypoStyle(Theme.primary, FontSize.MEDIUM),
                        cursorBrush = SolidColor(Theme.primary)
                    )
                    Text(suffixLabel, style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
                }
            }
            Item(type is Monthly, (type as? Monthly)?.dayOfMonth, "Jeden ", "ten den Monats") { Monthly(it) }
            Item(type is Daily, (type as? Daily)?.nDays, "Jede ", " Tage") { Daily(it) }
            Item(type is Weekly, (type as? Weekly)?.nWeeks, "Jede ", " Wochen") { Weekly(it) }
            Spacer(Modifier.height(20.dp))
            val coroutineScope = rememberCoroutineScope()
            Row(
                Modifier
                    .fillMaxWidth()
                ,
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if(associated==null)
                    Spacer(Modifier)
                else
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(Theme.primary)
                            .rippleClick {
                                coroutineScope.launch {
                                    viewModel.updateRepeatingEvent(event, null, associated?.id)
                                }
                                viewModel.showRepeatPopup.update { false }
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("Beenden", style = TypoStyle(Theme.background, FontSize.LARGE))
                    }
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(Theme.primary)
                        .rippleClick {
                            coroutineScope.launch {
                                if(associated?.id != null)
                                    viewModel.updateRepeatingEvent(event, type ?: return@launch, associated?.id)
                                else
                                    viewModel.updateRepeatingEvent(event, type ?: return@launch, null)
                                viewModel.showRepeatPopup.update { false }
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("Erstellen", style = TypoStyle(Theme.background, FontSize.LARGE))
                }
            }
        }
    }
}

@Composable
private fun BottomButton(
    inspectedEventViewModel: InspectedEventViewModel,
    syncable: Syncable.FeedInstantEventSyncable?,
    event: SyncedEvent,
    isEditing: Boolean
) {
    val nav = LocalNavController.current
    Column(
        Modifier
            .fillMaxWidth(.95f)
    ) {
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(Modifier.clip(CircleShape), color = OldColors.SECONDARY, thickness = 3.dp)
        Spacer(Modifier.height(20.dp))
        var isSending by remember { mutableStateOf(false) }
        var wasSuccessful by remember { mutableStateOf(false) }
        var affectsDelete by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        ActionBar(
            {
                if(isSending) return@ActionBar
                coroutineScope.launch {
                    affectsDelete = true
                    isSending = true
                    if(syncable != null) {
                        if(syncable.id != -1L) {
                            inspectedEventViewModel.updateOrCreateSynced(syncable, true)
                            inspectedEventViewModel.resync()
                        }
                    } else {
                        if(event.id != -1L) { // If the event is a new one, ignore everything, just close
                            inspectedEventViewModel.removeSyncedEvent(event)
                            inspectedEventViewModel.resync()
                        }
                    }
                    inspectedEventViewModel.setEditing(false)
                    isSending = false
                    wasSuccessful = true
                    nav.popBackStack()
                }
            },
            {
                if(!affectsDelete)
                    Icon(painterResource(R.drawable.delete), "Delete", Modifier.fillMaxSize(), Theme.onSecondaryContainer)
                else
                    if(isSending)
                        LifeProgressIndicator(Modifier.fillMaxHeight(), color = Theme.primary)
                    else if(wasSuccessful)
                        Icon(painterResource(R.drawable.tick), "Done", Modifier.fillMaxSize(), Theme.onSecondaryContainer)
            },
            if(syncable != null && syncable.getInvalidReason() == null) Theme.primaryContainer
            else if(event.raw.getInvalidReason()!=null) OldColors.SECONDARY else event.raw.type.colors.bg,
            {
                if(isSending) return@ActionBar
                if(!isEditing) {
                    inspectedEventViewModel.setEditing(true)
                    if(inspectedEventViewModel.popUpToHomeOnEdit.value) {
                        nav.navigate(NavPath.FULLSCREEN_EVENT) {
                            popUpTo(NavPath.HOME) { }
                        }
                        inspectedEventViewModel.popUpToHomeOnEdit.value = false
                    }
                } else {
                    val valid = if(syncable != null) syncable.getInvalidReason() else event.getInvalidReason()
                    if (valid != null) {
                        Toast.makeText(
                            context,
                            valid,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        coroutineScope.launch {
                            isSending = true
                            if(syncable != null) {
                                inspectedEventViewModel.updateOrCreateSynced(syncable)
                            } else {
                                inspectedEventViewModel.updateOrCreateSyncedEvent(event, event.isSynced())
                            }
                            inspectedEventViewModel.resync()
                            inspectedEventViewModel.setEditing(false)
                            isSending = false
                            wasSuccessful = true
                            nav.popBackStack()
                        }
                    }
                }
            }
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) { // We need this to keep the size up
                androidx.compose.animation.AnimatedVisibility(
                    isEditing,
                    Modifier.fillMaxSize(),
                    enter = fadeIn() + slideInHorizontally { it / 2 },
                    exit = fadeOut() + slideOutHorizontally { -it / 2 }
                ) {
                    Row(
                        Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (!affectsDelete && isSending){
                            LifeProgressIndicator(Modifier.fillMaxHeight(), color = Theme.primary)}
                        else
                            if (affectsDelete || !wasSuccessful) {
                                Text(
                                    if (event.isSynced() || syncable?.isSynced() == true) "Ändern" else "Hinzufügen",
                                    style = TypoStyleOld(
                                        FontColor.PRIMARY,
                                        FontSize.LARGE
                                    ).copy(fontWeight = FontWeight.W900)
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    painterResource(R.drawable.arrow_right),
                                    "Continue",
                                    tint = OldColors.PRIMARYFONT,
                                    modifier = Modifier.height(20.dp)
                                )
                            } else {
                                Icon(
                                    painterResource(R.drawable.tick),
                                    "Done",
                                    tint = OldColors.PRIMARYFONT,
                                    modifier = Modifier.height(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Fertig",
                                    style = TypoStyleOld(
                                        FontColor.PRIMARY,
                                        FontSize.LARGE
                                    ).copy(fontWeight = FontWeight.W900)
                                )
                            }
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    !isEditing,
                    Modifier.fillMaxSize(),
                    enter = fadeIn() + slideInHorizontally { it / 2 },
                    exit = fadeOut() + slideOutHorizontally { -it / 2 }
                ) {
                    Row(
                        Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Bearbeiten",
                            style = TypoStyleOld(
                                FontColor.PRIMARY,
                                FontSize.LARGE
                            ).copy(fontWeight = FontWeight.W900)
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            painterResource(R.drawable.arrow_right),
                            "Continue",
                            tint = OldColors.PRIMARYFONT,
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DisplayFullScreenEvent(inspectedEventViewModel: InspectedEventViewModel, event: SyncedEvent){
    val screens = LocalScreens.current
    val calendar = remember { Calendar.getInstance() }
    Column(
        Modifier
            .fillMaxSize()
        ,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        DisplayEvent(event)
        val coroutineScope = rememberCoroutineScope()
        Column(
            Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth()
                ,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                val repeatingEvents by inspectedEventViewModel.repeatingEvents.collectAsState(null)
                val isRepeating by remember {
                    derivedStateOf {
                        repeatingEvents?.events?.any {
                            event.id in it.associatedEvents
                        } ?: false
                    }
                }
                Row(
                    Modifier
                        .clip(CircleShape)
                        .rippleClick {
                            coroutineScope.launch {
                                inspectedEventViewModel.showRepeatPopup.update { true }
                            }
                        }
                        .background(Theme.surfaceContainerHighest)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .height(IntrinsicSize.Min)
                    ,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.repeat),
                        "Repeat",
                        Modifier
                            .aspectRatio(1f)
                            .size(FontSize.SMALLM.size.times(1.5f).toDp())
                        ,
                        Theme.primary
                    )
                    Text(
                        if(isRepeating) "Beenden" else "Wiederholen",
                        style = TypoStyle(
                            Theme.secondary,
                            FontSize.SMALL
                        )
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Erstellt " + event.created.formatTimeStamp(calendar) +
                        if (event.edited != null) " · Bearbeitet ${event.edited.formatTimeStamp(calendar)}" else "",
                Modifier
                    .fillMaxWidth(),
                style = TypoStyle(Theme.secondary, FontSize.SMALLM)
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(event.raw.type.colors.bg, RoundedCornerShape(25))
                    .clip(RoundedCornerShape(25))
                    .rippleClick {
                        screens.openCalendarWithSearch {
                            selectedEventTypes.value = selectedEventTypes.value.toMutableList()
                                .apply { add(event.raw.type) }
                        }
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    event.raw.type.calendarName,
                    color = event.raw.type.selectedColor,
                    fontSize = FontSize.MEDIUM.size
                )
            }
        }
    }
}