package com.myxoz.life.screens.alarm

import android.content.Intent
import android.icu.util.Calendar
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalScreens
import com.myxoz.life.LocalSettings
import com.myxoz.life.MainActivity
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.events.DigSocEvent
import com.myxoz.life.events.TravelEvent
import com.myxoz.life.events.additionals.TagEvent
import com.myxoz.life.events.additionals.TitleEvent
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.screens.LocalScreensProvider
import com.myxoz.life.screens.NavPath
import com.myxoz.life.screens.feed.dayoverview.edgeToEdgeGradient
import com.myxoz.life.screens.feed.fullscreenevent.TimeBasedVisualTransformation
import com.myxoz.life.screens.feed.main.RenderTagAndTitleBar
import com.myxoz.life.ui.ArrowDirection
import com.myxoz.life.ui.drawArrowBehind
import com.myxoz.life.ui.getMaxTabletScreenWidth
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.atStartAsMillis
import com.myxoz.life.utils.collectAsMutableState
import com.myxoz.life.utils.diagrams.chartBasedAnimation
import com.myxoz.life.utils.diagrams.chartBasedLongAnimation
import com.myxoz.life.utils.formatDayTime
import com.myxoz.life.utils.formatMinutesToVisual
import com.myxoz.life.utils.formatMsToDuration
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.utils.toLocalDate
import com.myxoz.life.utils.toPx
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.AlarmViewModel
import com.myxoz.life.viewmodels.ProfileInfoModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun AlarmScreen(alarmViewModel: AlarmViewModel){
    LaunchedEffect(Unit) {
        alarmViewModel.refresh()
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Theme.background)
            .padding(windowPadding)
        ,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        val nav = LocalNavController.current
        val rawNextEvent by alarmViewModel.nextEvent.collectAsState()
        val nextEvent = rawNextEvent
        if(nextEvent == null) {
            Text("Kein nächstes Event", style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
            return@Column
        }
        NextEventInformation(alarmViewModel, nextEvent, LocalScreens.current, LocalScreens.current.profileInfoModel)
        Spacer(Modifier.weight(1f))
        RenderClockVisual(alarmViewModel, nextEvent.proposed.start)
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .setMaxTabletWidth()
            ,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment =  Alignment.CenterVertically
        ) {
            Text("", style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
        }
        Row(
            Modifier
                .setMaxTabletWidth()
            ,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment =  Alignment.CenterVertically
        ) {
            val alarmSound by alarmViewModel.alarmSound.collectAsState()
            Text("Alarmton", style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
            Spacer(Modifier.width(10.dp))
            Row(
                Modifier
                    .background(Theme.secondaryContainer, CircleShape)
                    .clip(CircleShape)
                    .rippleClick{
                        nav.navigate(NavPath.Menu.Alarm.ALARM_SOUND_SETTINGS)
                    }
                    .padding(horizontal = 15.dp, vertical = 10.dp)
                    .weight(1f, false)
                ,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(alarmSound?.name ?: "Systemstandard", Modifier.weight(1f, false), style = TypoStyle(Theme.onSecondaryContainer, FontSize.SMALLM), overflow = TextOverflow.Ellipsis, maxLines = 1)
                Icon(painterResource(R.drawable.settings), "Settings", Modifier.size(FontSize.SMALLM.size.toDp()), Theme.onSecondaryContainer)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .setMaxTabletWidth()
            ,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment =  Alignment.CenterVertically
        ) {
            Text("Zeit zum Aufstehen", style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
            var timeToGetReadyMinutes by alarmViewModel.minutesToGetReady.collectAsMutableState()
            val focusManager = LocalFocusManager.current
            val textMeasurer = rememberTextMeasurer()
            var text = timeToGetReadyMinutes.formatMinutesToVisual()
            val textWidth = with(LocalDensity.current){
                textMeasurer.measure(
                    TimeBasedVisualTransformation.toTransformed(text),
                    style = TypoStyle(Theme.onSecondary, FontSize.MEDIUM),
                ).size.width.toDp()
            }
            BasicTextField(
                text,
                {
                    text = it.trimStart('0')
                    timeToGetReadyMinutes = TimeBasedVisualTransformation.displayMinutesToMinutes(it).toLong()
                },
                textStyle = TypoStyle(Theme.onSecondary, FontSize.MEDIUM),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions{
                    focusManager.clearFocus(true)
                },
                modifier = Modifier
                    .background(Theme.secondary, RoundedCornerShape(25))
                    .padding(horizontal = 15.dp, vertical = 10.dp)
                    .height(FontSize.MEDIUM.size.toDp())
                    .width(if(textWidth < 1.dp) 1.dp else textWidth),
                cursorBrush = SolidColor(Theme.onSecondary),
                visualTransformation = TimeBasedVisualTransformation(),
            )
        }
        Spacer(Modifier.height(10.dp))
        val settings = LocalSettings.current
        val hasAlarmsPermission by settings.features.lifeAlarmClock.has.collectAsState()
        val nextScheduledTs by alarmViewModel.nextScheduled.collectAsState()
        val alarmIsSet = nextScheduledTs > System.currentTimeMillis()
        Box(
            Modifier
                .setMaxTabletWidth()
                .padding(10.dp)
                .background(if(!hasAlarmsPermission) Theme.primary else if(!alarmIsSet) Theme.primary else Theme.secondary, CircleShape)
                .clip(CircleShape)
                .rippleClick{
                    if(!settings.features.lifeAlarmClock.hasAssured()){
                        nav.navigate(NavPath.Menu.More.Settings.PERMISSIONS)
                    } else if(!alarmIsSet) {
                        alarmViewModel.setAlarm(settings, nextEvent.proposed.start)
                    } else {
                        alarmViewModel.removeAlarm()
                    }
                }
                .padding(vertical = 20.dp)
            ,
            contentAlignment = Alignment.Center
        ) {
            Text(if(!hasAlarmsPermission) "Zu Wecker Berechtigung" else if(!alarmIsSet) "Wecker stellen" else "Alarm aufhalten", style = TypoStyle(Theme.onPrimary, FontSize.LARGE, FontFamily.Display))
        }
    }
}

@Composable
fun RenderEventPreview(syncedEvent: SyncedEvent, screens: LocalScreensProvider?, profileInfoModel: ProfileInfoModel){
    val height = 40.dp
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val openFullScreenEvent: () -> Unit = if(screens != null)
            ({ screens.openFullScreenEvent(syncedEvent) })
    else
            ({
                coroutineScope.launch {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        putExtra("targetRoute", NavPath.FULLSCREEN_EVENT)
                        putExtra("shared_event", syncedEvent.toJson().toString())
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    withContext(Dispatchers.Main){
                        context.startActivity(intent)
                    }
                }
            })
    Box(
        Modifier
            .background(syncedEvent.proposed.type.colors.bg, CircleShape)
            .clip(CircleShape)
            .rippleClick{
                openFullScreenEvent()
            }
            .padding(5.dp, 5.dp, 10.dp, 5.dp)
    ) {
        when(val event = syncedEvent.proposed) {
            is TagEvent -> RenderTagAndTitleBar(
                event.eventTags,
                if(event is TitleEvent) event.title else null,
                height,
                3,
                event.type.colors
            )
            is DigSocEvent -> {
                val people by profileInfoModel.getPeople(event.people).collectAsState()
                val displayText = people.joinToString(" · ") { it.name }
                RenderTagAndTitleBar(
                    event.digSocEntries.map { it.type },
                    event.title.ifBlank { displayText },
                    height,
                    3,
                    event.type.colors,
                )
            }
            is TravelEvent -> {
                val from by profileInfoModel.getLocationById(event.from).collectAsState()
                val to by profileInfoModel.getLocationById(event.to).collectAsState()
                val fromDisplay = from?.name ?: "Von"
                val toDisplay = to?.name ?: "Nach"
                Row(
                    Modifier
                        .padding(horizontal = 5.dp)
                        .height(IntrinsicSize.Min)
                    ,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(fromDisplay, style = TypoStyle(event.type.colors.textColor, FontSize.LARGE))
                    Box(
                        Modifier
                            .width(20.dp)
                            .fillMaxHeight()
                            .drawArrowBehind(ArrowDirection.Right, (height/5).toPx(), event.type.colors.secondary)
                    )
                    Text(toDisplay, style = TypoStyle(event.type.colors.textColor, FontSize.LARGE))
                }
            }
        }
    }
}

private val LongToAnimationVector = TwoWayConverter<Long, AnimationVector1D>(
    convertToVector = { AnimationVector1D(it.toFloat()) },
    convertFromVector = { it.value.toLong() }
)
@Composable
fun RenderClockVisual(alarmViewModel: AlarmViewModel, eventStart: Long) {
    val boxSize = getMaxTabletScreenWidth()
    val now by alarmViewModel.minuteFlow.collectAsState()
    val getReadyMinutes by alarmViewModel.minutesToGetReady.collectAsState()
    val zone = remember { ZoneId.systemDefault() }

    // Yes this can be 23 or 25, maybe I'll fix this later TODO optional
    val hoursInDay = 24
    val anglesInDay = (hoursInDay*1000f*3600/360f)
    val nowAngle = remember(now) { (now - now.toLocalDate(zone).atStartAsMillis(zone)) / anglesInDay - 90f }

    val wakeUpTarget = eventStart - getReadyMinutes * 60_000L
    val wakeUpAnim = remember { Animatable(System.currentTimeMillis() + 10_000L, LongToAnimationVector) }
    LaunchedEffect(wakeUpTarget) {
        wakeUpAnim.animateTo( wakeUpTarget, chartBasedLongAnimation)
    }
    val animatedWakeUp = wakeUpAnim.value

    val rotationAngle = remember(animatedWakeUp, now) {
        val goal = (animatedWakeUp - now) / anglesInDay
        // This might happen if you go to bed before midnight (so a really unlikely edgecase for me)
        if (animatedWakeUp < nowAngle) goal + 360f else goal
    }

    val calendar = remember { Calendar.getInstance() }
    val primaryFill = Theme.primary
    val secondaryFill = Theme.primaryContainer
    val clockDotColor = Theme.outlineVariant
    val secondaryText = Theme.outline
    val dotColor = Theme.background
    val textMeasurer = rememberTextMeasurer(hoursInDay)
    val onPrimaryContainer = Theme.onPrimaryContainer
    val messuredNowText = remember { textMeasurer.measure("Jetzt", TypoStyle(onPrimaryContainer, FontSize.SMALLM)) }
    val onPrimary = Theme.onPrimary
    val messuredWakeUpTimeText = remember(animatedWakeUp) {
        textMeasurer.measure(animatedWakeUp.formatDayTime(calendar), TypoStyle(onPrimary, FontSize.MEDIUM, FontFamily.Display))
    }
    Box(
        Modifier
            .width(boxSize)
            .aspectRatio(1f)
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val diameter = size.width - max(messuredWakeUpTimeText.size.width, messuredNowText.size.width) - size.width / 2.5f
            val offset = (size.width - diameter) / 2
            drawArc(
        secondaryFill,
                rotationAngle + nowAngle,
                360f - rotationAngle,
                false,
                Offset(offset, offset),
                Size(diameter, diameter),
                style = Stroke(size.width / 60f, cap = StrokeCap.Round)
            )
            drawArc(
                primaryFill,
                nowAngle,
                rotationAngle,
                false,
                Offset(offset, offset),
                Size(diameter, diameter),
                style = Stroke(size.width / 30f, cap = StrokeCap.Round)
            )
            val radius = diameter / 2f
            fun rad(angle: Float) = Math.toRadians(angle.toDouble()).toFloat()
            fun drawRotatedDot(angle: Float){
                drawCircle(
                    dotColor,
                    size.width / 90f,
                    Offset(
                        x = center.x + cos(rad(angle)) * radius,
                        y = center.y + sin(rad(angle)) * radius
                    )
                )
            }
            drawRotatedDot(nowAngle)
            drawRotatedDot(rotationAngle + nowAngle)
            val largeClockTextStyle = TypoStyle(primaryFill, FontSize.MEDIUM).copy(fontWeight = FontWeight.Bold)
            val smallClockTextStyle = TypoStyle(secondaryText, FontSize.MEDIUM)

            repeat(hoursInDay){ hour ->
                val angle = (hour / hoursInDay.toFloat() * 360f) - 90f
                val radius = diameter / 2.5f
                val center = Offset(
                        x = center.x + cos(rad(angle)) * radius,
                        y = center.y + sin(rad(angle)) * radius
                    )
                if(hour % 3 == 0) {
                    val result = textMeasurer.measure(hour.toString(), if(hour % 6 == 0) largeClockTextStyle else smallClockTextStyle)
                    drawText(result, topLeft = center - Offset(result.size.width.toFloat(), result.size.height.toFloat()) / 2f)
                } else {
                    drawCircle(clockDotColor, size.width / 130f, center)
                }
            }

            fun floatingText(text: TextLayoutResult, angle: Float, fill: Color){
                val radius = diameter / 2
                val center = Offset(
                    x = center.x + cos(rad(angle)) * (radius + text.size.width + size.width / 60f),
                    y = center.y + sin(rad(angle)) * (radius + text.size.height + size.width / 30f)
                )
                val horrizontalPadding = radius / 10f
                val verticalPadding = radius / 40f
                drawRoundRect(
                    fill,
                    Offset(
                        center.x - text.size.width / 2 - horrizontalPadding,
                        center.y - text.size.height / 2 - verticalPadding,
                    ),
                    Size(
                        text.size.width + 2 * horrizontalPadding,
                        text.size.height + 2 * verticalPadding,
                    ),
                    CornerRadius(
                        text.size.height + 2*verticalPadding,
                        text.size.height + 2*verticalPadding
                    )
                )
                drawText(text, topLeft = Offset(center.x - text.size.width / 2, center.y - text.size.height / 2))
            }
            floatingText(messuredNowText, nowAngle, secondaryFill)
            floatingText(messuredWakeUpTimeText, nowAngle + rotationAngle, primaryFill)
        }
    }
}
@Composable
fun NextEventInformation(alarmViewModel: AlarmViewModel, nextEvent: SyncedEvent, screens: LocalScreensProvider?, profileInfoModel: ProfileInfoModel){
    Column(
        Modifier
            .fillMaxWidth()
        ,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        val calendar = remember { Calendar.getInstance() }
        val currentTime by alarmViewModel.minuteFlow.collectAsState()
        Spacer(Modifier)
        Text("Nächstes Event:", style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
        RenderEventPreview(nextEvent, screens, profileInfoModel)
        Text("Um ${nextEvent.proposed.start.formatDayTime(calendar)} in", style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
        val animatedTime = remember { Animatable(0f) }
        LaunchedEffect(currentTime) {
            animatedTime.animateTo((nextEvent.proposed.start - currentTime).toFloat(), chartBasedAnimation)
        }
        Text(animatedTime.value.toLong().formatMsToDuration(true), style = TypoStyle(Theme.primary, FontSize.XXLARGE, FontFamily.Display))
    }
}
@Composable
fun AlarmingScreen(repos: AppRepositories, dimiss: ()->Unit, snooze: ()->Unit, profileInfoModel: ProfileInfoModel){
    val alarmViewModel: AlarmViewModel = remember { AlarmViewModel(repos) }
    LaunchedEffect(Unit) {
        alarmViewModel.refresh()
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Theme.primary)
            .padding(windowPadding)
        ,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Spacer(Modifier.weight(1f))
        Text("Guten Morgen!", style = TypoStyle(Theme.onPrimary, FontSize.XLARGE, FontFamily.Display))
        Spacer(Modifier.weight(1f))
        val nextRawEvent by alarmViewModel.nextEvent.collectAsState()
        val nextEvent = nextRawEvent
        if(nextEvent != null) {
            Box(
                Modifier
                    .setMaxTabletWidth()
                    .background(Theme.primaryContainer, RoundedCornerShape(30.dp))
                    .padding(20.dp)
                ,
            )  {
                NextEventInformation(alarmViewModel, nextEvent, null, profileInfoModel)
            }
        }
        Spacer(Modifier.weight(3f))
        Box(
            Modifier
                .setMaxTabletWidth()
                .background(Theme.primaryContainer, RoundedCornerShape(30.dp))
                .clip(RoundedCornerShape(30.dp))
                .rippleClick{ dimiss() }
                .padding(vertical = 30.dp)
            ,
            contentAlignment = Alignment.Center
        ) {
            Text("Alarm beenden", style = TypoStyle(Theme.onPrimaryContainer, FontSize.LARGE))
        }
        Spacer(Modifier.weight(.5f))
        Box(
            Modifier
                .setMaxTabletWidth()
                .background(Theme.primaryContainer, RoundedCornerShape(30.dp))
                .clip(RoundedCornerShape(30.dp))
                .rippleClick{ snooze() }
                .padding(vertical = 60.dp)
            ,
            contentAlignment = Alignment.Center
        ) {
            Text("5m Snooze", style = TypoStyle(Color.White, FontSize.XXLARGE, FontFamily.Display))
        }
        Spacer(Modifier.weight(.5f))
    }
}
@Composable
fun AlarmSoundSettings(alarmViewModel: AlarmViewModel){
    val context = LocalContext.current
    val allSound = remember {
        AlarmViewModel.getSystemAlarms(context)
    }
    var selectedAlarm by alarmViewModel.alarmSound.collectAsMutableState()
    val mediaPlayer = remember { MediaPlayer().apply {
        isLooping = true
    } }
    fun previewSound(sound: AlarmViewModel.Companion.AlarmSound){
        if(!alarmViewModel.hasWarnedFullSound) {
            Toast.makeText(context, "Der Alarm spielt immer auf voller Lautstärke", Toast.LENGTH_LONG).show()
            alarmViewModel.hasWarnedFullSound = true
        }
        mediaPlayer.apply {
            reset()
            setDataSource(context, sound.uri)
            prepare()
            start()
        }
    }
    DisposableEffect(Unit) {
        onDispose { mediaPlayer.release() }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Theme.background)
    ) {
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .edgeToEdgeGradient(Theme.background, windowPadding)
            ,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = windowPadding,
        ) {
            items(allSound, {it.uri}){
                Row(
                    Modifier
                        .setMaxTabletWidth()
                        .rippleClick{
                            selectedAlarm = it
                            previewSound(it)
                        }
                    ,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        it.name,
                        Modifier
                            .weight(1f)
                        ,
                        style = TypoStyle(Theme.primary, FontSize.MEDIUM)
                    )
                    RadioButton(
                        selectedAlarm == it,
                        {
                            selectedAlarm = it
                            previewSound(it)
                        },
                        colors = RadioButtonColors(
                            Theme.primary,
                            Theme.outline,
                            Theme.primary,
                            Theme.outline,
                        )
                    )
                }
            }
        }
   }
}