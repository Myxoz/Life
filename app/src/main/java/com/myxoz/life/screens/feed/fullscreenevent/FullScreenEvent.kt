package com.myxoz.life.screens.feed.fullscreenevent

import android.icu.util.Calendar
import android.widget.Toast
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalColors
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalScreens
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.ui.ActionBar
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.ui.theme.TypoStyleOld
import com.myxoz.life.utils.animateColorSchemeAsState
import com.myxoz.life.utils.formatTimeStamp
import com.myxoz.life.utils.rememberColorScemeFromColor
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.InspectedEventViewModel
import kotlinx.coroutines.launch

@Composable
fun FullScreenEvent(inspectedEventViewModel: InspectedEventViewModel){
    val event by inspectedEventViewModel.event.collectAsState()
    val calendar = remember { Calendar.getInstance() }
    val nav = LocalNavController.current
    val screens = LocalScreens.current
    val isEditing by inspectedEventViewModel.isEditing.collectAsState()
    val colorScheme = rememberColorScemeFromColor(event.proposed.type.color, event)
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
                val now by inspectedEventViewModel.timeFlow.collectAsState()
                TimeBar(
                    event.proposed,
                    if(now >= event.proposed.start) if(now <= event.proposed.end) (now-event.proposed.start) / (event.proposed.end-event.proposed.start).toFloat() else 1f else 0f,
                    Theme.surfaceContainer
                ) {
                    if(isEditing) inspectedEventViewModel.setInspectedEventTo(event.copy(proposedEvent = it))
                }
                Box(
                    Modifier.weight(1f),
                ){
                    androidx.compose.animation.AnimatedVisibility(
                        !isEditing,
                        enter = fadeIn() + slideInHorizontally { it/2 },
                        exit = fadeOut() + slideOutHorizontally { -it/2 }
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                            ,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            DisplayEvent(event)
                            Column(
                                Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Erstellt " + event.created.formatTimeStamp(calendar) +
                                            if (event.edited != null) " · Bearbeitet ${event.edited?.formatTimeStamp(calendar)}" else "",
                                    Modifier
                                        .fillMaxWidth(),
                                    style = TypoStyle(Theme.secondary, FontSize.SMALLM)
                                )
                                Spacer(Modifier.height(10.dp))
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(event.proposed.type.color, RoundedCornerShape(25))
                                        .clip(RoundedCornerShape(25))
                                        .rippleClick{
                                            screens.openCalendarWithSearch{
                                                selectedEventTypes.value = selectedEventTypes.value.toMutableList().apply { add(event.proposed.type) }
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        event.proposed.type.calendarName,
                                        color = event.proposed.type.selectedColor,
                                        fontSize = FontSize.MEDIUM.size
                                    )
                                }
                            }
                        }
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        isEditing,
                        enter = fadeIn() + slideInHorizontally { it/2 },
                        exit = fadeOut() + slideOutHorizontally { -it/2 }
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                            ,
                            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)
                        ) {
                            ModifyEvent(inspectedEventViewModel)
                        }
                    }
                }
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
                                isSending = true
                                affectsDelete = true
                                if(event.id != -1L) { // If the event is a new one, ignore everything, just close
                                    inspectedEventViewModel.removeSyncedEvent(event)
                                    inspectedEventViewModel.resync()
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
                                    CircularProgressIndicator(color = OldColors.SECONDARYFONT)
                                else if(wasSuccessful)
                                    Icon(painterResource(R.drawable.tick), "Done", Modifier.fillMaxSize(), Theme.onSecondaryContainer)
                        },
                        if(event.proposed.getInvalidReason()!=null) OldColors.SECONDARY else event.proposed.type.color,
                        {
                            if(isSending) return@ActionBar
                            if(!isEditing) {
                                inspectedEventViewModel.setEditing(true)
                                if(inspectedEventViewModel.popUpToHomeOnEdit.value) {
                                    nav.navigate("fullscreen_event") {
                                        popUpTo("home")
                                    }
                                    inspectedEventViewModel.popUpToHomeOnEdit.value = false
                                }
                            } else {
                                val valid = event.proposed.getInvalidReason()
                                if (valid != null) {
                                    Toast.makeText(
                                        context,
                                        valid,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    coroutineScope.launch {
                                        isSending = true
                                        inspectedEventViewModel.updateOrCreateSyncedEvent(event)
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
                                    if (!affectsDelete && isSending) CircularProgressIndicator(color = OldColors.PRIMARYFONT) else
                                        if (affectsDelete || !wasSuccessful) {
                                            Text(
                                                if (event.id != -1L) "Ändern" else "Hinzufügen",
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
        }
    }
}