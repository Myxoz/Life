package com.myxoz.life.screens.feed.summarizeday

import android.content.Context.MODE_PRIVATE
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.myxoz.life.LocalAPI
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalSettings
import com.myxoz.life.LocalStorage
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.api.syncables.FullDaySyncable
import com.myxoz.life.dbwrapper.DayScreenTimeEntity
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.screens.options.getMappedUsageDataBetween
import com.myxoz.life.screens.options.getUsageDataBetween
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.ui.theme.TypoStyleOld
import com.myxoz.life.utils.diagrams.PieChart
import com.myxoz.life.utils.rippleClick
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

@Composable
fun SummarizeDay() {
    val api = LocalAPI.current
    val db = LocalStorage.current
    Scaffold(
        Modifier.fillMaxSize(),
        containerColor = Theme.background
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val fadeInDuration = 2000
            var currentStage by remember { mutableStateOf(Stage.HAPPYNESS) }
            var last30DaysSetter by remember { mutableFloatStateOf(0f) }
            var last3DaysSetter by remember { mutableFloatStateOf(0f) }
            val averageLast30Days by animateFloatAsState(last30DaysSetter, tween(fadeInDuration))
            val averageLast3Days by animateFloatAsState(last3DaysSetter, tween(fadeInDuration))
            var recordedHappyness by remember { mutableIntStateOf(0) }
            var recordedStress by remember { mutableIntStateOf(0) }
            var recordedSuccessfulness by remember { mutableIntStateOf(0) }
            Box(
                Modifier
                    .padding(50.dp)
                    .size(100.dp)
            ) {
                val rot = when (currentStage) {
                    Stage.HAPPYNESS -> 0
                    Stage.STRESS -> 1
                    Stage.SUCCESSFULNESS -> 3
                    Stage.DONE -> 6
                }
                val rotation by animateFloatAsState(
                    rot * 360f,
                    tween(fadeInDuration)
                )
                Box(
                    Modifier
                        .size(100.dp)
                        .rotate(rotation)
                ) {
                    val oneColor by animateColorAsState(
                        Theme.primary.copy(if(currentStage.value > 0) 1f else .2f),
                        tween(fadeInDuration)
                    )
                    val twoColor by animateColorAsState(
                        Theme.primary.copy(if(currentStage.value > 1) 1f else .2f),
                        tween(fadeInDuration)
                    )
                    val threeColor by animateColorAsState(
                        Theme.primary.copy(if(currentStage.value > 2) 1f else .2f),
                        tween(fadeInDuration)
                    )
                    Icon(
                        painterResource(R.drawable.liffy_outer_one),
                        "Liffy",
                        Modifier.size(100.dp),
                        tint = oneColor
                    )
                    Icon(
                        painterResource(R.drawable.liffy_outer_two),
                        "Liffy",
                        Modifier.size(100.dp),
                        tint = twoColor
                    )
                    Icon(
                        painterResource(R.drawable.liffy_outer_three),
                        "Liffy",
                        Modifier.size(100.dp),
                        tint = threeColor
                    )
                }
                LiffyFace(100.dp)
            }
            val enter = remember {
                slideIn(tween(fadeInDuration)) {
                    IntOffset(
                        0,
                        it.height
                    )
                } + fadeIn(tween(fadeInDuration))
            }
            val exit = remember {
                slideOut(tween(fadeInDuration)) {
                    IntOffset(
                        0,
                        -it.height / 2
                    )
                } + fadeOut(tween(fadeInDuration)) + scaleOut(tween(fadeInDuration * 2))
            }
            Box(
                Modifier.height(100.dp).fillMaxWidth().clip(RectangleShape)
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    currentStage == Stage.HAPPYNESS,
                    enter = enter,
                    exit = exit
                ) {
                    var liffySubText by remember { mutableStateOf("") }
                    LaunchedEffect(Unit) {
                        val last3Days = db.days.getLastNDays(3)
                        val last30Days = db.days.getLastNDays(30)
                        val average3Days = last3Days.map { it.happyness }.average().takeIf { !it.isNaN() } ?: 0.0
                        val average30Days = last30Days.map { it.happyness }.average().takeIf { !it.isNaN() } ?: 0.0
                        last3DaysSetter = average3Days.toFloat()
                        last30DaysSetter = average30Days.toFloat()
                        liffySubText = when (average3Days - average30Days) {
                            in 20.0..Double.MAX_VALUE -> "Alter, was machst du die letzten Tage, was dich so glücklich macht?"
                            in 10.0..20.0 -> "Hoffentlich wieder so geil, wie in den letzten Tagen!"
                            in 5.0..10.0 -> "Wieder besser als sonst?"
                            in -5.0..5.0 -> "So wie immer, nh?"
                            in -10.0..-5.0 -> "Ab heute wieder besser?"
                            in -20.0..-10.0 -> "Endlich wieder besser?!"
                            in Double.MIN_VALUE..-20.0 -> "Bitte besser als sonst, was ist denn das hier aktuell?"
                            else -> ""
                        }
                    }
                    val liffyText = "Wie zufrieden bist du?"
                    LiffyText(
                        liffyText,
                        liffySubText
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    currentStage == Stage.STRESS,
                    enter = enter,
                    exit = exit
                ) {
                    var liffySubText by remember { mutableStateOf("") }
                    LaunchedEffect(Unit) {
                        val last3Days = db.days.getLastNDays(3)
                        val last30Days = db.days.getLastNDays(30)
                        val average3Days = last3Days.map { it.stress }.average().takeIf { !it.isNaN() } ?: 0.0
                        val average30Days = last30Days.map { it.stress }.average().takeIf { !it.isNaN() } ?: 0.0
                        last3DaysSetter = average3Days.toFloat()
                        last30DaysSetter = average30Days.toFloat()
                        liffySubText = when (average3Days - average30Days) {
                            in 20.0..Double.MAX_VALUE -> "Bro, chill mal nen bisschen, das schon erheblich mehr stress als sonst"
                            in 10.0..20.0 -> "Man merkt schon, dass du deutlich gestresster bist!"
                            in 5.0..10.0 -> "Weiterhin gestresster als sonst?"
                            in -5.0..5.0 -> "So wie immer, nh?"
                            in -10.0..-5.0 -> "Stresstendenz sinkend"
                            in -20.0..-10.0 -> "Endlich wieder bisschen Ruhe"
                            in Double.MIN_VALUE..-20.0 -> "Kein Stress mehr, wie angenehm!"
                            else -> ""
                        }
                    }
                    val liffyText = "Wie gestresst?"
                    LiffyText(
                        liffyText,
                        liffySubText
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    currentStage == Stage.SUCCESSFULNESS,
                    enter = enter,
                    exit = exit
                ) {
                    var liffySubText by remember { mutableStateOf("") }
                    LaunchedEffect(Unit) {
                        val last3Days = db.days.getLastNDays(3)
                        val last30Days = db.days.getLastNDays(30)
                        val average3Days = last3Days.map { it.successfulness }.average().takeIf { !it.isNaN() } ?: 0.0
                        val average30Days = last30Days.map { it.successfulness }.average().takeIf { !it.isNaN() } ?: 0.0
                        last3DaysSetter = average3Days.toFloat()
                        last30DaysSetter = average30Days.toFloat()
                        liffySubText = when (average3Days - average30Days) {
                            in 20.0..Double.MAX_VALUE -> "Der Junge kriegt richtig was geschaft, heute wieder so viel?"
                            in 10.0..20.0 -> "Mmmmh, sehr stark gerade, wie wars heute?"
                            in 5.0..10.0 -> "Nen weiterer produktiverer Tag?"
                            in -5.0..5.0 -> "So wie immer, nh?"
                            in -10.0..-5.0 -> "Du bist aktuell nen Tick unproduktiver als sonst, heute wieder?"
                            in -20.0..-10.0 -> "Man chillt halt, check ich zwar, bissl was zu machen, schadet aber save nd"
                            in Double.MIN_VALUE..-20.0 -> "Beweg dein Arsch!"
                            else -> ""
                        }
                    }
                    val liffyText = "Und wie produktiv?"
                    LiffyText(
                        liffyText,
                        liffySubText
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    currentStage == Stage.DONE,
                    enter = enter,
                    exit = exit
                ) {
                    LiffyText(
                        "Und schon fertig!",
                        "So sah dein Tag heute aus"
                    )
                }
            }
            val boxHeight = 500.dp
            Box(
                Modifier
                    .fillMaxWidth(.9f)
                    .height(boxHeight)
            ) {
                val boxHeightPx = with(LocalDensity.current) { boxHeight.toPx() }
                var displayedPercentage by remember { mutableFloatStateOf(0f) }
                androidx.compose.animation.AnimatedVisibility(
                    currentStage != Stage.DONE,
                    exit = fadeOut()
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(.9f)
                            .height(boxHeight)
                            .pointerInput(currentStage) {
                                detectDragGestures(
                                    onDragStart = {
                                        val percentage = ((boxHeightPx - it.y) / boxHeightPx)
                                        displayedPercentage = percentage.coerceIn(0f, 1f)
                                    },
                                    onDrag = { change, _ ->
                                        val percentage =
                                            ((boxHeightPx - change.position.y) / boxHeightPx)
                                        displayedPercentage = percentage.coerceIn(0f, 1f)
                                    },
                                    onDragEnd = {
                                        currentStage = when (currentStage) {
                                            Stage.HAPPYNESS -> {
                                                recordedHappyness =
                                                    (displayedPercentage * 100).toInt()
                                                Stage.STRESS
                                            }

                                            Stage.STRESS -> {
                                                recordedStress = (displayedPercentage * 100).toInt()
                                                Stage.SUCCESSFULNESS
                                            }

                                            Stage.SUCCESSFULNESS -> {
                                                recordedSuccessfulness =
                                                    (displayedPercentage * 100).toInt()
                                                Stage.DONE
                                            }

                                            Stage.DONE -> Stage.DONE
                                        }
                                    }

                                )
                            }
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth(.5f)
                                .offset(y = -averageLast30Days / 100f * boxHeight)
                                .align(Alignment.BottomEnd),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                "Letzte 30 Tage",
                                style = TypoStyle(Theme.secondary, FontSize.MEDIUM)
                            )
                            Box(
                                Modifier.fillMaxWidth().height(2.dp).background(Theme.secondary)
                            )
                        }
                        Column(
                            Modifier
                                .fillMaxWidth(.5f)
                                .offset(y = -averageLast3Days / 100f * boxHeight)
                                .align(Alignment.BottomStart),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                "Letzte 3 Tage",
                                style = TypoStyle(Theme.secondary, FontSize.MEDIUM)
                            )
                            Box(
                                Modifier.fillMaxWidth().height(2.dp).background(Theme.secondary)
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxWidth(.3f)
                                .fillMaxHeight()
                                .align(Alignment.Center)
                                .background(Theme.surfaceContainerHigh, RoundedCornerShape(25.dp))
                        )
                        Box(
                            Modifier
                                .fillMaxWidth(.3f)
                                .height(displayedPercentage * boxHeight)
                                .align(Alignment.BottomCenter)
                                .background(Theme.primaryContainer, RoundedCornerShape(25.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (displayedPercentage * 100).toInt().toString(),
                                style = TypoStyle(Theme.onPrimaryContainer, FontSize.LARGE),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    currentStage == Stage.DONE,
                    enter = fadeIn(tween(fadeInDuration))
                ) {
                    val context = LocalContext.current
                    val stepsDao = db.proposedSteps
                    val settings = LocalSettings.current
                    val pieChart = remember { PieChart() }
                    LaunchedEffect(Unit) {
                        val zone = ZoneId.systemDefault()
                        val day = LocalDate.now().minusDays(1)
                        val fullDayEvent = FullDaySyncable(
                            recordedHappyness,
                            recordedStress,
                            recordedSuccessfulness,
                            if(settings.features.stepCounting.hasAssured())
                            stepsDao.getStepsByDay(day.toEpochDay())?.steps
                                ?: context.getSharedPreferences("steps", MODE_PRIVATE).run {
                                    getLong("saved_steps", 0L) - getLong("steps_at_midnight", 0L)
                                }.toInt() else 0,
                            if(settings.features.stepCounting.hasAssured()) getUsageDataBetween(
                                context,
                                day.atStartOfDay(zone).toEpochSecond() * 1000L,
                                day.plusDays(1).atStartOfDay(zone).toEpochSecond() * 1000L
                            ).toInt() else 0,
                            day.toEpochDay()
                        )
                        val usageData = getMappedUsageDataBetween(
                            context,
                            day.atStartOfDay(zone).toEpochSecond() * 1000L,
                            day.plusDays(1).atStartOfDay(zone).toEpochSecond() * 1000L
                        )
                        val dayScreenTimeDao = db.dayScreenTime
                        usageData
                            .entries
                            .sortedBy { -it.value }
                            .take(5)
                            .forEach {
                                dayScreenTimeDao.insertDayScreenTime(
                                    DayScreenTimeEntity(
                                        day.toEpochDay(),
                                        it.key,
                                        it.value
                                    )
                                )
                            }
                        fullDayEvent.saveAndSync(db)
                        api.resyncLastDays()
                        stepsDao.deleteDay(day.toEpochDay())
                        val total = mutableMapOf<String, Long>()
                        val startOfDay = day.atStartOfDay(zone).toEpochSecond()*1000L
                        val endOfDay = day.plusDays(1).atStartOfDay(zone).toEpochSecond()*1000L
                        db.events.getEventsBetween(startOfDay, endOfDay).sortedBy { it.start }.forEach {
                            val duration = it.end.coerceAtMost(endOfDay) - it.start.coerceAtLeast(startOfDay)
                            total[it.type.toString()] = total[it.type.toString()]?.plus(duration) ?: duration
                        }
                        pieChart.update(total.mapValues {
                            val cal = EventType.getById(it.key.toIntOrNull()?:return@mapValues PieChart.Companion.PieChartPart(EventType.Empty.color, 0.0))
                            PieChart.Companion.PieChartPart(cal?.color ?: EventType.Empty.color, it.value.toDouble())
                        })
                    }
                    Column(
                        Modifier
                            .height(boxHeight)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                        Box(Modifier.size(screenWidth*.7f)){
                            pieChart.Render()
                        }
                        Box(
                            Modifier
                                .background(OldColors.FLAGSLIDER.copy(.6f), CircleShape)
                                .padding(vertical = 30.dp)
                                .fillMaxWidth(.8f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Juhu Statistiken",
                                style = TypoStyleOld(FontColor.ONSLIDER, FontSize.LARGE),
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        val nav = LocalNavController.current
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .rippleClick {
                                    nav.popBackStack()
                                }
                                .background(OldColors.SELECTED.copy(.6f), CircleShape)
                                .padding(vertical = 30.dp)
                                .fillMaxWidth(.8f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Nächstes Mal vielleicht",
                                style = TypoStyleOld(FontColor.ONSLIDER, FontSize.LARGE),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
enum class Stage(val value: Int) {
    HAPPYNESS(0),
    STRESS(1),
    SUCCESSFULNESS(2),
    DONE(3)
}
@Composable
fun LiffyText(title: String, subText: String){
    Column(
        Modifier.height(100.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            title,
            Modifier
                .padding(horizontal = 15.dp, vertical = 5.dp)
            ,
            style = TypoStyle(Theme.primary, FontSize.LARGE)
        )
        Text(
            subText,
            Modifier
                .padding(horizontal = 15.dp, vertical = 5.dp)
            ,
            textAlign = TextAlign.Center,
            style = TypoStyle(Theme.secondary, FontSize.MEDIUM)
        )
    }
}

@Composable
fun LiffyFace(size: Dp){
    val animation = rememberInfiniteTransition()
    val vertical by animation.animateFloat(
        0f,
        1f,
        infiniteRepeatable(
            keyframes {
                val dur = 150
                val offset = 3000
                0f at 0+offset
                1f at dur+offset
                0f at 2*dur+offset
                -1f at 3*dur+offset
                0f at 4*dur+offset
                0f at 5000
                durationMillis = 5000
            }
        )
    )
    Icon(
        painterResource(R.drawable.liffy_face),
        "Liffy",
        Modifier
            .rotate(vertical*10)
            .offset(y=-abs(vertical)*.05*size)
            .size(size)
        ,
        tint = Theme.secondary
    )
}