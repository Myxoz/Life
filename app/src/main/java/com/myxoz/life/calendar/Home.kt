package com.myxoz.life.calendar

import android.content.Context.MODE_PRIVATE
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.myxoz.life.search.LifeBottomBar
import com.myxoz.life.LocalAPI
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalSettings
import com.myxoz.life.LocalStorage
import com.myxoz.life.R
import com.myxoz.life.calendar.feed.CalendarComposable
import com.myxoz.life.calendar.feed.msToDisplay
import com.myxoz.life.options.LiffyFace
import com.myxoz.life.options.getUsageDataBetween
import com.myxoz.life.subscreens.displayperson.UnmodalBottomSheet
import com.myxoz.life.subscreens.displayperson.datePickerColors
import com.myxoz.life.ui.ActionBar
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.viewmodels.CalendarViewModel
import com.myxoz.life.viewmodels.InspectedEventViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeComposable(calendarViewModel: CalendarViewModel, inspectedEventViewModel: InspectedEventViewModel){
    var showDayPopup by remember { mutableStateOf(false) }

    val db = LocalStorage.current
    val api = LocalAPI.current
    val nav = LocalNavController.current
    val currentBackStackEntry by nav.currentBackStackEntryAsState()
    LaunchedEffect(currentBackStackEntry) {
        val currentRoute = currentBackStackEntry?.destination?.route
        if (currentRoute == "home") {
            showDayPopup = db.days.getDay(LocalDate.now().minusDays(1).toEpochDay().toInt()) == null
        }
    }
    LaunchedEffect(Unit) {
        if(api.resyncLastDays() != 0) calendarViewModel.refreshEvents()
    }

    Scaffold(
        Modifier.fillMaxSize(),
        containerColor = Colors.BACKGROUND
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.weight(1f)) {
                CalendarComposable(calendarViewModel, inspectedEventViewModel)
            }
            LifeBottomBar(calendarViewModel)
        }
        if (showDayPopup) {
            ModalBottomSheet(
                {
                    showDayPopup = false
                },
                sheetState = rememberModalBottomSheetState(true),
                containerColor = Colors.BACKGROUND
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(70.dp)
                    ) {
                        Icon(
                            painterResource(R.drawable.liffy_outer),
                            "Liffy",
                            Modifier
                                .size(70.dp),
                            tint = Colors.PRIMARYFONT
                        )
                        LiffyFace(70.dp)
                    }
                    Text("Hey!", style = TypoStyle(FontColor.PRIMARY, FontSize.LARGE))
                    Text(
                        "Trage deinen gestrigen Tag endgültig ein",
                        style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM)
                    )
                    Spacer(Modifier.height(20.dp))
                    val day = remember { LocalDate.now().minusDays(1) }
                    Text(
                        "${day.dayOfMonth}.${day.month.value}.${day.year}",
                        style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM)
                    )
                    val settings = LocalSettings.current
                    val screenTimeEnabled by settings.features.screentime.has.collectAsState()
                    val stepCountEnabled by settings.features.stepCounting.has.collectAsState()
                    if(screenTimeEnabled || stepCountEnabled)
                        Column(
                            Modifier
                            .fillMaxWidth()
                            .border(1.dp, Colors.TERTIARY, RoundedCornerShape(25.dp))
                            .padding(horizontal = 25.dp, vertical = 20.dp)
                    ) {
                        val context = LocalContext.current
                        if(stepCountEnabled) {
                            var steps by remember {
                                mutableIntStateOf(0)
                            }
                            LaunchedEffect(Unit) {
                                steps = db.proposedSteps.getStepsByDay(day.toEpochDay())?.steps
                                    ?: context.getSharedPreferences("steps", MODE_PRIVATE).run {
                                        getLong("saved_steps", 0L) - getLong("steps_at_midnight", 0L)
                                    }.toInt()
                            }
                            Text(
                                "Schritte",
                                style = TypoStyle(FontColor.PRIMARY, FontSize.SMALL)
                            )
                            Text(
                                steps.toString(),
                                style = TypoStyle(
                                    FontColor.PRIMARY,
                                    FontSize.XLARGE,
                                    FontFamily.Display
                                )
                            )
                        }
                        if(screenTimeEnabled) {
                            val zone = remember { ZoneId.systemDefault() }
                            val screenTime = remember {
                                getUsageDataBetween(
                                    context,
                                    day.atStartOfDay(zone).toEpochSecond() * 1000L,
                                    day.plusDays(1).atStartOfDay(zone).toEpochSecond() * 1000L
                                ).toInt()
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                "Bildschirmzeit",
                                style = TypoStyle(FontColor.PRIMARY, FontSize.SMALL)
                            )
                            Text(
                                screenTime.msToDisplay(),
                                style = TypoStyle(
                                    FontColor.PRIMARY,
                                    FontSize.XLARGE,
                                    FontFamily.Display
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = Colors.SECONDARY, thickness = 3.dp, modifier = Modifier.clip(CircleShape))
                    Spacer(Modifier)
                    val navController = LocalNavController.current
                    ActionBar(
                        {
                            showDayPopup = false
                        },
                        {
                            Icon(painterResource(R.drawable.close),"Close",Modifier.fillMaxSize(),Colors.SECONDARYFONT)
                        },
                        Colors.SELECTED,
                        {
                            showDayPopup = false
                            navController.navigate("summarize_day")
                        },
                    ) {
                        Text(
                            "Eintragen",
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
        val isSelectDayVisible by calendarViewModel.selectDayPopup.collectAsState()
        UnmodalBottomSheet(isSelectDayVisible, {calendarViewModel.selectDayPopup.value = false}) {
            val datePickerState = rememberDatePickerState()
            DatePicker(datePickerState, colors = datePickerColors())
            ActionBar({
                calendarViewModel.selectDayPopup.value = false
            }, {
                Icon(painterResource(R.drawable.close), "Close", Modifier.fillMaxSize(), tint = Colors.SECONDARYFONT)
            }, Colors.SELECTED, {
                val selectedDay = datePickerState.selectedDateMillis?.let { millis ->
                    Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.of("UTC"))
                        .toLocalDate()
                }?: return@ActionBar
                calendarViewModel.setDay(selectedDay)
                calendarViewModel.selectDayPopup.value = false
            }) {
                Text(
                    "Auswählen",
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