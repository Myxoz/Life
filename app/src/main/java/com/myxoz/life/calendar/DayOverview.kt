package com.myxoz.life.calendar

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.myxoz.life.LocalSettings
import com.myxoz.life.LocalStorage
import com.myxoz.life.R
import com.myxoz.life.calendar.feed.msToDisplay
import com.myxoz.life.calendar.feed.screenTimeGoal
import com.myxoz.life.calendar.feed.stepsGoal
import com.myxoz.life.dbwrapper.BankingEntity
import com.myxoz.life.dbwrapper.formatCents
import com.myxoz.life.diagrams.PieChart
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.options.getUsageDataBetween
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.rippleClick
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun DayOverviewComposable(navController: NavController, epochDay: Long){
    val db = LocalStorage.current
    val settings = LocalSettings.current
    val isToday = remember { LocalDate.now().toEpochDay() ==  epochDay }
    var happyness: Int? by remember { mutableStateOf(null) }
    var stress: Int? by remember { mutableStateOf(null) }
    var successfulness: Int? by remember { mutableStateOf(null) }
    var screenTime: Long? by remember { mutableStateOf(null) }
    var steps: Long? by remember { mutableStateOf(null) }
    val bankingEntries = remember {
        mutableStateListOf<BankingEntity>()
    }
    val pieChart = remember { PieChart() }
    val dateString = remember {
        val date = LocalDate.ofEpochDay(epochDay)
        "${getWeekDayByInt(date.dayOfWeek.value - 1)} ${date.dayOfMonth}.${date.month.value}.${date.year}"
    }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val day = db.days.getDay(epochDay.toInt())
        if(day != null) {
            happyness = day.happyness
            stress = day.stress
            successfulness = day.successfulness
            screenTime = day.screenTimeMs.toLong()
            steps = day.steps.toLong()
        }
        val date = LocalDate.ofEpochDay(epochDay)
        val zone = ZoneId.systemDefault()
        bankingEntries.clear()
        bankingEntries.addAll(
            db.banking.getFullDayTransactions(
                date.atStartOfDay(zone).toEpochSecond()*1000L,
                date.plusDays(1).atStartOfDay(zone).toEpochSecond()*1000L
            )
        )
        if(isToday && settings.features.screentime.has.value) {
            val zone = ZoneId.systemDefault()
            while (true){
                screenTime =
                    getUsageDataBetween(
                        context,
                        LocalDate.ofEpochDay(epochDay).atStartOfDay(zone).toEpochSecond()*1000L,
                        System.currentTimeMillis()
                    )
                delay(1000)
            }
        }
        val total = mutableMapOf<String, Long>()
        val startOfDay = LocalDate.ofEpochDay(epochDay).atStartOfDay(zone).toEpochSecond()*1000L
        val endOfDay = LocalDate.ofEpochDay(epochDay).plusDays(1).atStartOfDay(zone).toEpochSecond()*1000L
        db.events.getEventsBetween(startOfDay, endOfDay).sortedBy { it.start }.forEach {
            val duration = it.end.coerceAtMost(endOfDay) - it.start.coerceAtLeast(startOfDay)
            total[it.type.toString()] = total[it.type.toString()]?.plus(duration) ?: duration
        }
        pieChart.update(total.mapValues {
            val cal = EventType.getById(it.key.toIntOrNull()?:return@mapValues PieChart.Companion.PieChartPart(EventType.Empty.color, 0.0))
            PieChart.Companion.PieChartPart(cal?.color ?: EventType.Empty.color, it.value.toDouble())
        }) //  Also in SummarizeDay
    }
    val showSteps by settings.features.stepCounting.has.collectAsState()
    if(isToday && showSteps) StepCounterTrigger { steps = it }
    Scaffold(
        Modifier.fillMaxSize(),
        containerColor = Colors.BACKGROUND
    ) { innerPadding ->
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                dateString,
                style = TypoStyle(FontColor.SECONDARY, FontSize.XLARGE, FontFamily.Display).copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .padding(top = 20.dp)
                    .padding(top = innerPadding.calculateTopPadding())
            )
            Spacer(Modifier)
            FeelingsBlock(happyness, stress, successfulness)
            Spacer(Modifier)
            if(showSteps) DisplayStepsBlock(steps)
            val screentime by settings.features.screentime.has.collectAsState()
            if(screentime) DisplayTimeBlock(screenTime) { navController.navigate("day/$epochDay/screentime") }
            if (bankingEntries.isNotEmpty()) BankingBlock(bankingEntries) { navController.navigate("day/$epochDay/transactions") }
            Column (
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, Colors.TERTIARY,  RoundedCornerShape(25.dp))
                    .background(Colors.SECONDARY, RoundedCornerShape(25.dp))
                    .clip(RoundedCornerShape(25.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Aufteilung", style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                Box(Modifier.size(screenWidth*.7f)){
                    pieChart.Render()
                }
            }
            Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }
}

@Composable
fun FeelingsBlock(happyness: Int?, stress: Int?, successfulness: Int?){
    Column (
        Modifier
            .fillMaxWidth()
        ,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FeelingsBlockItem("Zufriedenheit", happyness, Colors.HAPPYNESS, true)
        FeelingsBlockItem("Stress", stress, Colors.STRESS, false)
        FeelingsBlockItem("Produktivität", successfulness, Colors.PRODUCTIVITY, true)
    }
}
@Composable
fun FeelingsBlockItem(name: String, value: Int?, color: Color, fromLeft: Boolean){
    Box (
        Modifier
            .fillMaxWidth()
            .background(Colors.SECONDARY, CircleShape)
            .height(70.dp)
            .clip(CircleShape)
    ) {
        Box(
            Modifier
                .fillMaxWidth(value?.div(100f)?:0f)
                .background(color, CircleShape)
                .fillMaxHeight()
                .align(
                    if(fromLeft) Alignment.CenterStart else Alignment.CenterEnd
                )
        )
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 35.dp)
            ,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if(fromLeft) {
                Text(name, style = TypoStyle(FontColor.SECONDARY, FontSize.LARGE))
                Text(value?.toString()?:"--", style = TypoStyle(FontColor.SECONDARY, FontSize.DISPLAY, FontFamily.Display).copy(fontWeight = FontWeight.Bold))
            } else {
                Text(value?.toString()?:"--", style = TypoStyle(FontColor.SECONDARY, FontSize.DISPLAY, FontFamily.Display).copy(fontWeight = FontWeight.Bold))
                Text(name, style = TypoStyle(FontColor.SECONDARY, FontSize.LARGE))
            }
        }
    }
}
@Composable
fun DisplayTimeBlock(timeInMs: Long?, openScreenTime: ()->Unit){
    Column (
        Modifier
            .fillMaxWidth()
            .border(1.dp, Colors.TERTIARY,  RoundedCornerShape(25.dp))
            .background(Colors.SECONDARY, RoundedCornerShape(25.dp))
            .clip(RoundedCornerShape(25.dp))
            .rippleClick(function = openScreenTime)
            .padding(20.dp)
    ) {
        Text("Bildschirmzeit", style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM))
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .clip(CircleShape)
        ) {
            Box( // Used for slight rounding errors at the edge
                Modifier
                    .fillMaxSize()
                    .height(50.dp)
                    .background(Colors.TERTIARY)
            )
            Box(
                Modifier
                    .height(50.dp)
                    .fillMaxWidth(timeInMs?.div(screenTimeGoal)?:0f)
                    .background(Colors.SCREENTIME, CircleShape)
            )
            Text(
                timeInMs?.toInt()?.msToDisplay()?:"--h --m --s",
                style = TypoStyle(FontColor.SECONDARY, FontSize.XLARGE, FontFamily.Display),
                modifier =
                    Modifier
                        .padding(horizontal = 25.dp)
                        .align(Alignment.CenterStart)
            )
        }
    }
}

@Composable
fun DisplayStepsBlock(steps: Long?){
    Column (
        Modifier
            .fillMaxWidth()
            .border(1.dp, Colors.TERTIARY,  RoundedCornerShape(25.dp))
            .background(Colors.SECONDARY, RoundedCornerShape(25.dp))
            .clip(RoundedCornerShape(25.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(painterResource(R.drawable.walk), "Walk", Modifier.size(50.dp, 50.dp), Colors.PRIMARYFONT)
            Box(
                Modifier
                    .height(50.dp)
                    .fillMaxWidth()
                    .background(Colors.TERTIARY, CircleShape)
                    .clip(CircleShape)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(steps?.div(stepsGoal)?.coerceIn(0f, 1f)?:0f)
                        .height(50.dp)
                        .background(Colors.STEPS, CircleShape)
                )
                Text(
                    steps?.toString()?:"Keine",
                    Modifier
                        .align(
                            Alignment.CenterEnd
                        )
                        .padding(horizontal = 25.dp)
                    ,
                    style = TypoStyle(FontColor.SECONDARY, FontSize.XLARGE, FontFamily.Display)
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
            ,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Schritte", style = TypoStyle(FontColor.PRIMARY, FontSize.MEDIUM).copy(fontWeight = FontWeight.Bold))
            Text("${if(steps!=null) "ca. ${(steps*.7).toInt().mToDistance()} · " else ""}${((steps?:0L)/ stepsGoal *100).toInt()}%", style = TypoStyle(FontColor.PRIMARY, FontSize.MEDIUM).copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun BankingBlock(bankingEntries: List<BankingEntity>, openBankingMenu: ()->Unit){
    Column (
        Modifier
            .fillMaxWidth()
            .border(1.dp, Colors.TERTIARY,  RoundedCornerShape(25.dp))
            .background(Colors.SECONDARY, RoundedCornerShape(25.dp))
            .clip(RoundedCornerShape(25.dp))
            .rippleClick(function = openBankingMenu)
            .padding(20.dp)
    ) {
        Text("Überweisungen", style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM))
        Spacer(Modifier.height(10.dp))
        Text(
            bankingEntries.sumOf { it.amountCents }.formatCents(),
            style = TypoStyle(FontColor.SECONDARY, FontSize.XLARGE, FontFamily.Display),
        )
    }
}

fun Int.mToDistance(): String =
    when(this) {
        in 0..1000 -> "${this}m"
        in 1000..10_000 -> "${this/1000},${(this/10)%100} km"
        in 10_000..100_000 -> "${this/1000},${(this/100)%10} km"
        else -> "${this}m"
    }
@Composable
fun StepCounterTrigger(trigger: (Long)->Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val stepSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }
    val midnight = remember { context.getSharedPreferences("steps", MODE_PRIVATE).getLong("steps_at_midnight", 0L) }

    var steps: Long by rememberSaveable { mutableLongStateOf(0L) }

    DisposableEffect(stepSensor) {
        if (stepSensor == null) {
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    steps = event.values[0].toLong()-midnight
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)

            onDispose {
                sensorManager.unregisterListener(listener)
            }
        }
    }

    trigger(steps)
}
fun getMonthByCalendarMonth(month: Int): String{
    return listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Juni", "Juli", "Aug", "Sep", "Okt", "Nov", "Dez")[month]
}
fun getWeekDayByInt(day: Int): String {
    return listOf("Mon", "Di", "Mi", "Do", "Fr", "Sa", "So")[day]
}