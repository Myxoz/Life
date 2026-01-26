package com.myxoz.life.screens.person.displayperson

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.myxoz.life.LocalScreens
import com.myxoz.life.LocalStorage
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.events.DigSocEvent
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.screens.feed.main.msToDisplay
import com.myxoz.life.ui.ActionBar
import com.myxoz.life.ui.SCREENMAXWIDTH
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.LargeDataCache
import com.myxoz.life.viewmodels.ProfileInfoModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileFullScreen(
    personId: Long,
    photoPicker: PhotoPicker,
    largeDataCache: LargeDataCache,
    profileInfoModel: ProfileInfoModel
){
    val screens = LocalScreens.current
    val db = LocalStorage.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isPickingBirthDay by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        profileInfoModel.updateStateIfOutdated(personId, context)
    }
    val verticalScrollState = rememberScrollState()
    Box(
        Modifier
            .background(Theme.background)
        ,
        contentAlignment = Alignment.TopCenter
    ){
        val fontSize = FontSize.XLARGE.size.toDp() + 20.dp
        val conf = LocalConfiguration.current
        val screenWidth = min(conf.screenWidthDp, SCREENMAXWIDTH.value.toInt())
        val smallerScreenDimension = min(screenWidth, conf.screenHeightDp).dp
        val isProfilePictureFullScreen by profileInfoModel.isProfilePictureFullScreen.collectAsState()
        val maxPbSize by animateDpAsState(smallerScreenDimension * if(isProfilePictureFullScreen) 1f else 0.5f)
        val topBarHeight = 100.dp + maxPbSize + fontSize
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
            ,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier
                    .setMaxTabletWidth()
                ,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Spacer(Modifier.height(topBarHeight+ windowPadding.calculateTopPadding()))
                ProfileInfo(largeDataCache, profileInfoModel)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    val birthday by profileInfoModel.birthday.collectAsState()
                    val dateString = remember(birthday) {
                        val bd = birthday ?: return@remember "Hinzufügen"
                        val date = LocalDate.ofEpochDay(bd)
                        val now = LocalDate.now()
                        val month = date.month.value
                        val year = date.year
                        val day = date.dayOfMonth
                        val age = now.year - year - 1 + if((now.month.value > month) || (now.month.value == month && now.dayOfMonth >= day /* If equals, birthday is today, Happy Birthday */)) 1 else 0
                        "$day.$month.$year · ${age}J"
                    }
                    val lastInteraction by profileInfoModel.lastInteraction.collectAsState()
                    val nextInteraction by profileInfoModel.nextInteraction.collectAsState()
                    val lastInteractionDisplay = remember(lastInteraction) {
                        ProfileInfoModel.formatTime((lastInteraction?.end?:return@remember null) - System.currentTimeMillis())
                    }
                    val nextInteractionDisplay = remember(nextInteraction) {
                        ProfileInfoModel.formatTime((nextInteraction?.start ?: return@remember null) - System.currentTimeMillis())
                    }
                    FlowRow(
                        maxItemsInEachRow = 2,
                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                        verticalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        FlowRowItem(
                            "Geburtstag",
                            R.drawable.birthday,
                            dateString
                        ) {
                            isPickingBirthDay = true
                        }
                        if(lastInteractionDisplay!=null){
                            val type: SyncedEvent? by produceState(null) {
                                value = SyncedEvent.from(db, lastInteraction?:return@produceState)
                            }
                            FlowRowItem(
                                "Zuletzt interagiert",
                                type?.let { if(it.proposed is DigSocEvent) it.proposed.digSocEntries.maxByOrNull { c -> c.durationMs }?.type?.drawable else null } ?: R.drawable.met,
                                lastInteractionDisplay
                            ) {
                                coroutineScope.launch {
                                    screens.openFullScreenEvent(
                                        SyncedEvent.from(db, lastInteraction?:return@launch)?:return@launch
                                    )
                                }
                            }
                        }
                        if(nextInteractionDisplay!=null){
                            FlowRowItem(
                                "Nächste Interaktion",
                                R.drawable.met,
                                nextInteractionDisplay
                            ) {
                                coroutineScope.launch {
                                    screens.openFullScreenEvent(
                                        SyncedEvent.from(db, nextInteraction?:return@launch)?:return@launch
                                    )
                                }
                            }
                        }
                    }
                }
                Column(
                    Modifier
                        .background(Theme.surfaceContainerHigh, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .rippleClick {}
                        .padding(10.dp)
                    ,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val width = smallerScreenDimension*.95f-10.dp*2
                    Text("Aufteilung", style = TypoStyle(Theme.secondary, FontSize.MEDIUM), modifier = Modifier.fillMaxWidth())
                    val boxWidth = width
                    ButtonGroup(
                        arrayOf("All-Time", "1 Jahr", "1 Monat", "1 Woche"),
                        boxWidth,
                        profileInfoModel.chartScale
                    ) {
                        profileInfoModel.chartScale.value = it
                        coroutineScope.launch {
                            profileInfoModel.renderPieChart()
                        }
                    }
                    Box(Modifier.size(width*.8f)){
                        profileInfoModel.chart.Render()
                    }
                    ButtonGroup(
                        arrayOf("Prozent", "Zeit"),
                        boxWidth,
                        profileInfoModel.chartUnit
                    ) {}
                    val fontSize = FontSize.MEDIUM.size.toDp()
                    Column(
                        Modifier
                            .width(width),
                        Arrangement.spacedBy(10.dp)
                    ) {
                        val components by profileInfoModel.chart.components.collectAsState()
                        val chartUnit by profileInfoModel.chartUnit.collectAsState()
                        val acc = components.values.sumOf { it.value }
                        components.forEach {
                            if(it.value.value <= 0.0) return@forEach
                            val calendar = EventType.getById(it.key.toIntOrNull()?:return@forEach)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(CircleShape)
                                    .rippleClick{
                                        screens.openCalendarWithSearch{
                                            selectedPeople.value = listOf(personId)
                                            selectedEventTypes.value = listOf(calendar?:return@openCalendarWithSearch)
                                        }
                                    }
                                ,
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .size(fontSize)
                                            .background(
                                                calendar?.color ?: it.value.color,
                                                CircleShape
                                            ) // Should be the same
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        calendar?.calendarName?:"NULL",
                                        style = TypoStyle(Theme.primary, FontSize.LARGE)
                                    )
                                }
                                Text(
                                    if(chartUnit==0) (it.value.value / acc).formatPercent(2) else it.value.value.toInt().msToDisplay(true),
                                    style = TypoStyle(Theme.primary, FontSize.LARGE)
                                )
                            }
                        }
                        HorizontalDivider(color = Theme.outline)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .rippleClick{
                                    screens.openCalendarWithSearch{
                                        selectedPeople.value = listOf(personId)
                                        selectedEventTypes.value = EventType.order.toList()
                                    }
                                }
                            ,
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(fontSize)
                                        .background(
                                            Brush.sweepGradient(EventType.order.map { it.color }),
                                            CircleShape
                                        ) // Should be the same
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Summe",
                                    style = TypoStyle(Theme.primary, FontSize.LARGE)
                                )
                            }
                            Text(
                                if(chartUnit==0) 1.0.formatPercent(2) else acc.toInt().msToDisplay(true),
                                style = TypoStyle(Theme.primary, FontSize.LARGE)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(windowPadding.calculateBottomPadding()))
            }
        }
        val scrollLength = with(LocalDensity.current){ verticalScrollState.value.toDp() }
        Box(
            Modifier
                .background(Theme.background)
                .padding(top = windowPadding.calculateTopPadding())
        ){
            ProfilePictureWithText(
                photoPicker,
                profileInfoModel,
                personId,
                scrollLength,
                fontSize,
                topBarHeight
            ){
                profileInfoModel.isEditing.value = true
                profileInfoModel.isExtended.value = true
                profileInfoModel.name.value = it
            }
        }
    }
    UnmodalBottomSheet(isPickingBirthDay, {isPickingBirthDay=false}) {
        val birthday by profileInfoModel.birthday.collectAsState()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = birthday?.let {
                LocalDate.ofEpochDay(it).atStartOfDay(ZoneId.of("UTC")).toEpochSecond()*1000L
            }
        )
        DatePicker(datePickerState, colors = datePickerColors())
        ActionBar({
            isPickingBirthDay = false
        }, {
            Icon(painterResource(R.drawable.close), "Close", Modifier.fillMaxSize(), Theme.onSecondaryContainer)
        }, Theme.primaryContainer, {
            profileInfoModel.isEditing.value = true
            profileInfoModel.isExtended.value = true
            profileInfoModel.birthday.value = datePickerState.selectedDateMillis?.let { millis ->
                Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.of("UTC"))
                    .toLocalDate()
                    .toEpochDay()
            }
            isPickingBirthDay = false
        }) {
            Text(
                "Eintragen",
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
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowScope.FlowRowItem(title: String, icon: Int, text: String, onClick: ()->Unit){
    Column(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(Theme.surfaceContainerHigh, RoundedCornerShape(25))
            .clip(RoundedCornerShape(25))
            .rippleClick{onClick()}
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        val iconSize = FontSize.MEDIUM.size.toDp() * 1.5f
        Text(title, style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(painterResource(icon), "Icon", Modifier.size(iconSize), Theme.primary)
            Text(text, style = TypoStyle(Theme.primary, FontSize.MEDIUM))
        }
    }
}
@Composable
fun UnmodalBottomSheet(isVisible: Boolean, close: ()->Unit, content: @Composable ()->Unit){
    AnimatedVisibility(
        isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.4f))
                .pointerInput(Unit) {} // Block underlying scroll
        )
    }
    AnimatedVisibility(
        isVisible,
        enter = slideInVertically{it},
        exit = slideOutVertically{it},
    ) {
        var progress by remember { mutableFloatStateOf(0f) }
        PredictiveBackHandler {
            it.collect { col ->
                progress = col.progress
            }
            close()
        }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(.5f, 1f)
                    scaleX = (1 - progress * .15f)
                    scaleY = (1 - progress * .15f)
                }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Theme.surfaceContainer, RoundedCornerShape(20.dp, 20.dp))
                    .padding(15.dp)
                    .align(Alignment.BottomCenter)
                ,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun datePickerColors() = DatePickerDefaults.colors(
    containerColor = Theme.surfaceContainer,
    titleContentColor = Theme.secondary,
    headlineContentColor = Theme.secondary,
    weekdayContentColor = Theme.secondary,
    subheadContentColor = Theme.secondary,
    navigationContentColor = Theme.secondary,
    yearContentColor = Theme.secondary,
    currentYearContentColor = Theme.primary,
    selectedYearContentColor = Theme.primary,
    selectedYearContainerColor = Theme.primaryContainer,
    dayContentColor = Theme.secondary,
    selectedDayContentColor = Theme.primary,
    selectedDayContainerColor = Theme.primaryContainer,
    todayContentColor = Theme.primary,
    todayDateBorderColor = Theme.primary,
    dividerColor = Theme.outline,
    dateTextFieldColors = TextFieldDefaults.colors(
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        focusedContainerColor = Theme.surfaceContainer,
        unfocusedContainerColor = Theme.surfaceContainer,
        cursorColor = Theme.primary,
        focusedTextColor = Theme.primary,
        unfocusedTextColor = Theme.primary,
        focusedLabelColor = Theme.primary,
        unfocusedLabelColor = Theme.primary,
        focusedSupportingTextColor = Theme.primary,
    ),
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun switchColors(
    selectedHandleColor: Color = Theme.onPrimary,
    selectedTrackColor: Color = Theme.primary,
    selectedIconColor: Color = Theme.onPrimaryContainer,

    unselectedHandleColor: Color = Theme.outline,
    unselectedTrackColor: Color = Theme.surfaceContainerHigh,
    unselectedIconColor: Color = Theme.surfaceContainerHigh,

    disabledSelectedHandleColor: Color =
        Theme.surface,
    disabledSelectedTrackColor: Color =
        Theme.onSurface.copy(alpha = 0.12f),

    disabledUnselectedHandleColor: Color =
        Theme.onSurface.copy(alpha = 0.38f),
    disabledUnselectedTrackColor: Color =
        Theme.surfaceContainerHigh.copy(alpha = 0.12f),

    borderColor: Color = Theme.outline,
): SwitchColors = SwitchColors(
    checkedThumbColor = selectedHandleColor,
    checkedTrackColor = selectedTrackColor,
    checkedIconColor = selectedIconColor,

    uncheckedThumbColor = unselectedHandleColor,
    uncheckedTrackColor = unselectedTrackColor,
    uncheckedIconColor = unselectedIconColor,

    disabledCheckedThumbColor = disabledSelectedHandleColor,
    disabledCheckedTrackColor = disabledSelectedTrackColor,
    disabledCheckedIconColor =
        Theme.onSurface.copy(alpha = 0.38f),

    disabledUncheckedThumbColor = disabledUnselectedHandleColor,
    disabledUncheckedTrackColor = disabledUnselectedTrackColor,
    disabledUncheckedIconColor =
        Theme.surfaceContainerHigh.copy(alpha = 0.38f),

    checkedBorderColor = Color.Transparent,
    uncheckedBorderColor = borderColor,
    disabledCheckedBorderColor = Color.Transparent,
    disabledUncheckedBorderColor =
        Theme.onSurface.copy(alpha = 0.12f),
)
@Composable
fun ButtonGroup(list: Array<String>, width: Dp, selectedItem: MutableStateFlow<Int>, onClick: (Int) -> Unit){
    Box(
        Modifier
            .width(width)
            .background(Theme.secondaryContainer, CircleShape)
    ) {
        Row {
            list.forEachIndexed { i, v ->
                Box(
                    Modifier
                        .clip(CircleShape)
                        .rippleClick {
                            selectedItem.value = i
                            onClick(i)
                        }
                        .width(width / list.size)
                        .padding(vertical = 5.dp)
                ) {
                    Text(
                        v,
                        Modifier.fillMaxWidth(),
                        style = TypoStyle(Theme.secondary, FontSize.MEDIUM),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        val selectedIndex by selectedItem.collectAsState()
        val offsetLeft by animateDpAsState(selectedIndex * width / list.size)
        Box(
            Modifier
                .offset(x = offsetLeft)
                .width(width / list.size)
                .border(1.dp, Theme.outline, CircleShape)
                .padding(5.dp)
        ) {
            Text(
                "",
                Modifier.fillMaxWidth(),
                style = TypoStyle(Theme.onSecondaryContainer, FontSize.MEDIUM),
                textAlign = TextAlign.Center
            )
        }
    }
}
fun Double.formatPercent(precision: Int): String = "${(this*100).toInt()}.${(this*100).toString().substringAfter(".").padStart(precision, '0').substring(0, precision)}%"