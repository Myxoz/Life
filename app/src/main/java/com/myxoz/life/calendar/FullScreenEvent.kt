package com.myxoz.life.calendar

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.myxoz.life.LocalAPI
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalStorage
import com.myxoz.life.R
import com.myxoz.life.api.API
import com.myxoz.life.dbwrapper.WaitingSyncEntity
import com.myxoz.life.subscreens.formatTimeStamp
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.viewmodels.CalendarViewModel
import com.myxoz.life.viewmodels.InspectedEventViewModel
import com.myxoz.life.viewmodels.ProfileInfoModel
import kotlinx.coroutines.launch

@Composable
fun FullScreenEvent(inspectedEventViewModel: InspectedEventViewModel, profileInfoModel: ProfileInfoModel, calendarViewModel: CalendarViewModel){
    val event by inspectedEventViewModel.event.collectAsState()
    val calendar = remember { Calendar.getInstance() }
    val nav = LocalNavController.current
    val db = LocalStorage.current
    val api = LocalAPI.current
    val isEditing by inspectedEventViewModel.isEditing.collectAsState()
    Scaffold(
        Modifier.fillMaxSize(),
        containerColor = Colors.BACKGROUND
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
            ,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val now by inspectedEventViewModel.timeFlow.collectAsState()
            TimeBar(
                event.proposed,
                if(now >= event.proposed.start) if(now <= event.proposed.end) (now-event.proposed.start) / (event.proposed.end-event.proposed.start).toFloat() else 1f else 0f,
                event.proposed.type.color,
                {calendarViewModel.setDay(it); nav.popBackStack("home", false)}
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
                            .fillMaxHeight()
                            .fillMaxWidth(.95f),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        DisplayEvent(event, profileInfoModel)
                        Column(
                            Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Erstellt " + event.created.formatTimeStamp(calendar) +
                                        if (event.edited != null) " · Bearbeitet ${event.edited?.formatTimeStamp(calendar)}" else "",
                                Modifier
                                    .fillMaxWidth(),
                                style = TypoStyle(FontColor.SECONDARY, FontSize.SMALLM)
                            )
                            Spacer(Modifier.height(10.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .background(event.proposed.type.color, RoundedCornerShape(25))
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
                            .fillMaxHeight()
                            .fillMaxWidth(.95f),
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)
                    ) {
                        ModifyEvent(inspectedEventViewModel, profileInfoModel)
                    }
                }
            }
            Column(
                Modifier
                    .fillMaxWidth(.95f)
            ) {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(Modifier.clip(CircleShape), color = Colors.SECONDARY, thickness = 3.dp)
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
                                db.waitingSync.deleteWaitingSync( // If the event is jet to be synced, discard the sync request
                                    event.id,
                                    event.proposed.type.id
                                )
                                db.waitingSync.insertWaitingSync(
                                    WaitingSyncEntity(
                                        event.id,
                                        -event.proposed.type.id,
                                        System.currentTimeMillis()
                                    )
                                )
                                event.proposed.eraseFromDB(db, event.id) // Delete if causes troubles
                                api.resyncLastDays()
                            }
                            inspectedEventViewModel.setEditing(false)
                            isSending = false
                            wasSuccessful = true
                            nav.popBackStack()
                        }
                    },
                    {
                        if(!affectsDelete)
                            Icon(painterResource(R.drawable.delete), "Delete", Modifier.fillMaxSize(), Colors.SECONDARYFONT)
                        else
                            if(isSending)
                                CircularProgressIndicator(color = Colors.SECONDARYFONT)
                            else if(wasSuccessful)
                                Icon(painterResource(R.drawable.tick), "Done", Modifier.fillMaxSize(), Colors.SECONDARYFONT)
                    },
                    if(event.proposed.getInvalidReason()!=null) Colors.SECONDARY else event.proposed.type.color,
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
                            if (valid != null) Toast.makeText(
                                context,
                                valid,
                                Toast.LENGTH_SHORT
                            ).show()
                            if (valid == null) coroutineScope.launch {
                                isSending = true
                                if (event.id != -1L) {
                                    event.proposed.eraseFromDB(db, event.id)
                                }
                                event.proposed.insertAndSyncEvent(
                                    db,
                                    api,
                                    event.id.takeIf { it != -1L } ?: API.generateId(),
                                    System.currentTimeMillis(),
                                    if(event.id == -1L) null else System.currentTimeMillis() // Adjusted on server side, but if offline / sync is off
                                )
                                inspectedEventViewModel.setEditing(false)
                                isSending = false
                                wasSuccessful = true
                                nav.popBackStack()
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
                                if (!affectsDelete && isSending) CircularProgressIndicator(color = Colors.PRIMARYFONT) else
                                    if (affectsDelete || !wasSuccessful) {
                                        Text(
                                            if (event.id != -1L) "Ändern" else "Hinzufügen",
                                            style = TypoStyle(
                                                FontColor.PRIMARY,
                                                FontSize.LARGE
                                            ).copy(fontWeight = FontWeight.W900)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Icon(
                                            painterResource(R.drawable.arrow_right),
                                            "Continue",
                                            tint = Colors.PRIMARYFONT,
                                            modifier = Modifier.height(20.dp)
                                        )
                                    } else {
                                        Icon(
                                            painterResource(R.drawable.tick),
                                            "Done",
                                            tint = Colors.PRIMARYFONT,
                                            modifier = Modifier.height(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Fertig",
                                            style = TypoStyle(
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
                                    style = TypoStyle(
                                        FontColor.PRIMARY,
                                        FontSize.LARGE
                                    ).copy(fontWeight = FontWeight.W900)
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    painterResource(R.drawable.arrow_right),
                                    "Continue",
                                    tint = Colors.PRIMARYFONT,
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