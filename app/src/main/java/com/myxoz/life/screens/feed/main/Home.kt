package com.myxoz.life.screens.feed.main

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
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalSettings
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.screens.feed.search.LifeBottomBar
import com.myxoz.life.screens.feed.summarizeday.LiffyFace
import com.myxoz.life.screens.options.getUsageDataBetween
import com.myxoz.life.screens.person.displayperson.UnmodalBottomSheet
import com.myxoz.life.screens.person.displayperson.datePickerColors
import com.myxoz.life.ui.ActionBar
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
    var isSelectDayVisible by remember { mutableStateOf(false) }

    val nav = LocalNavController.current
    val currentBackStackEntry by nav.currentBackStackEntryAsState()
    LaunchedEffect(currentBackStackEntry) {
        val currentRoute = currentBackStackEntry?.destination?.route
        if (currentRoute == "home") {
            calendarViewModel.yesterdaySummaryAdded.collect {
                showDayPopup = (it == null) && !inspectedEventViewModel.isEditing.value
            }
        }
    }
    LaunchedEffect(Unit) {
        calendarViewModel.resync()
    }

    Scaffold(
        Modifier.fillMaxSize(),
        containerColor = Theme.surfaceContainer
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.weight(1f)) {
                CalendarComposable(calendarViewModel, inspectedEventViewModel) {showDayPopup = true}
            }
            LifeBottomBar(calendarViewModel)
        }
        if (showDayPopup) {
            ModalBottomSheet(
                {
                    showDayPopup = false
                },
                sheetState = rememberModalBottomSheetState(true),
                containerColor = Theme.surfaceContainer
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
                            tint = Theme.primary
                        )
                        LiffyFace(70.dp)
                    }
                    Text("Hey!", style = TypoStyle(Theme.primary, FontSize.LARGE))
                    Text(
                        "Trage deinen gestrigen Tag endgültig ein",
                        style = TypoStyle(Theme.secondary, FontSize.MEDIUM)
                    )
                    Spacer(Modifier.height(20.dp))
                    val day = remember { LocalDate.now().minusDays(1) }
                    Text(
                        "${day.dayOfMonth}.${day.month.value}.${day.year}",
                        style = TypoStyle(Theme.primary, FontSize.MEDIUM)
                    )
                    val settings = LocalSettings.current
                    val screenTimeEnabled by settings.features.screentime.has.collectAsState()
                    val stepCountEnabled by settings.features.stepCounting.has.collectAsState()
                    if(screenTimeEnabled || stepCountEnabled)
                        Column(
                            Modifier
                            .fillMaxWidth()
                            .border(1.dp, Theme.outlineVariant, RoundedCornerShape(25.dp))
                            .padding(horizontal = 25.dp, vertical = 20.dp)
                    ) {
                        val context = LocalContext.current
                        if(stepCountEnabled) {
                            val steps by calendarViewModel.lastInsertedSteps.collectAsState()
                            Text(
                                "Schritte",
                                style = TypoStyle(Theme.primary, FontSize.SMALL)
                            )
                            Text(
                                steps.toString(),
                                style = TypoStyle(
                                    Theme.secondary,
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
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                "Bildschirmzeit",
                                style = TypoStyle(Theme.primary, FontSize.SMALL)
                            )
                            Text(
                                screenTime.formatMsToDuration(),
                                style = TypoStyle(
                                    Theme.secondary,
                                    FontSize.XLARGE,
                                    FontFamily.Display
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = Theme.outlineVariant, thickness = 3.dp, modifier = Modifier.clip(CircleShape))
                    Spacer(Modifier)
                    val navController = LocalNavController.current
                    ActionBar(
                        {
                            showDayPopup = false
                        },
                        {
                            Icon(painterResource(R.drawable.close),"Close",Modifier.fillMaxSize(),Theme.onSecondaryContainer)
                        },
                        Theme.primaryContainer,
                        {
                            showDayPopup = false
                            navController.navigate("summarize_day")
                        },
                    ) {
                        Text(
                            "Eintragen",
                            style = TypoStyle(
                                Theme.onPrimaryContainer,
                                FontSize.LARGE
                            ).copy(fontWeight = FontWeight.W900)
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            painterResource(R.drawable.arrow_right),
                            "Continue",
                            tint = Theme.onPrimaryContainer,
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }
            }
        }
        UnmodalBottomSheet(isSelectDayVisible, {isSelectDayVisible = false}) {
            val datePickerState = rememberDatePickerState()
            DatePicker(datePickerState, colors = datePickerColors())
            ActionBar(
                {
                    isSelectDayVisible = false
                },
                {
                    Icon(painterResource(R.drawable.close), "Close", Modifier.fillMaxSize(), tint = Theme.onSecondaryContainer)
                },
                Theme.primaryContainer,
                {
                val selectedDay = datePickerState.selectedDateMillis?.let { millis ->
                    Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.of("UTC"))
                        .toLocalDate()
                }?: return@ActionBar
                calendarViewModel.setDay(selectedDay)
                isSelectDayVisible = false
            }) {
                Text(
                    "Auswählen",
                    style = TypoStyle(
                        Theme.onSecondaryContainer,
                        FontSize.LARGE
                    ).copy(fontWeight = FontWeight.W900)
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    painterResource(R.drawable.arrow_right),
                    "Continue",
                    tint = Theme.onSecondaryContainer,
                    modifier = Modifier.height(20.dp)
                )
            }
        }
    }
}