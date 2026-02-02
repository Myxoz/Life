package com.myxoz.life.screens.feed.dayoverview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalScreens
import com.myxoz.life.LocalSettings
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.dbwrapper.banking.formatCents
import com.myxoz.life.repositories.BankingRepo
import com.myxoz.life.screens.feed.main.formatMsToDuration
import com.myxoz.life.screens.feed.main.screenTimeGoal
import com.myxoz.life.screens.feed.main.stepsGoal
import com.myxoz.life.ui.SCREENMAXWIDTH
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.MaterialShapes
import com.myxoz.life.utils.diagrams.PieChart
import com.myxoz.life.utils.formatMToDistance
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toShape
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.DayOverviewViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.min

@Composable
fun DayOverviewComposable(date: LocalDate, dayOverviewViewModel: DayOverviewViewModel){
    val settings = LocalSettings.current
    val isToday = remember(LocalDate.now()) { LocalDate.now() == date }
    val birthdays by dayOverviewViewModel.getAllBirthdaysAt(date).collectAsState(listOf())
    val summary by dayOverviewViewModel.getDaySummary(date).collectAsState(null)
    val bankingDisplayEntitys by dayOverviewViewModel.getAllTransactions(date).collectAsState(listOf())
    val chart = remember { PieChart() }
    val chartData by dayOverviewViewModel.getPieChart(date).collectAsState(mapOf())
    LaunchedEffect(chartData) {
        chart.update(chartData)
        println("We definitly launched the chart?! $chartData")
    }
    val dateString = remember {
        "${getWeekDayByInt(date.dayOfWeek.value - 1)} ${date.dayOfMonth}.${date.month.value}.${date.year}"
    }
    val screenTime by dayOverviewViewModel.getScreentime(date).collectAsStateWithLifecycle(0L)
    val showSteps by settings.features.stepCounting.has.collectAsState()
    val innerPadding = windowPadding
    Box(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .edgeToEdgeGradient(Theme.background, innerPadding)
        ,
        Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .setMaxTabletWidth()
            ,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                dateString,
                style = TypoStyle(Theme.primary, FontSize.XLARGE, FontFamily.Display).copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding() + 10.dp, bottom = 10.dp)
            )
            FeelingsBlock(summary?.happyness, summary?.stress, summary?.successfulness)
            Spacer(Modifier)
            val steps = if(isToday) {
                val step by dayOverviewViewModel.getAllSteps.collectAsState()
                step
            } else {
                summary?.steps?.toLong()
            }
            if(showSteps) DisplayStepsBlock(steps)
            if(birthdays.isNotEmpty()) BirthdayBlock(birthdays,date, dayOverviewViewModel)
            val screentime by settings.features.screentime.has.collectAsState()
            val nav = LocalNavController.current
            if(screentime) DisplayTimeBlock(screenTime) { nav.navigate("day/${date.toEpochDay()}/screentime") }
            if(bankingDisplayEntitys.isNotEmpty()) BankingBlock(bankingDisplayEntitys) { nav.navigate("day/${date.toEpochDay()}/transactions") }
            Column (
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, Theme.outlineVariant,  RoundedCornerShape(25.dp))
                    .background(Theme.surfaceContainer, RoundedCornerShape(25.dp))
                    .clip(RoundedCornerShape(25.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Aufteilung", style = TypoStyle(Theme.primary, FontSize.MEDIUM), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                val screenWidth = min(LocalConfiguration.current.screenWidthDp, SCREENMAXWIDTH.value.toInt()).dp
                Box(Modifier.size(screenWidth*.7f)){
                    chart.Render()
                }
            }
            Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }
}

@Composable
fun BirthdayBlock(birthdays: List<PersonSyncable>, selectedDate: LocalDate, dayOverviewViewModel: DayOverviewViewModel){
    val screens = LocalScreens.current
    Column (
        Modifier
            .fillMaxWidth()
            .border(1.dp, Theme.outlineVariant,  RoundedCornerShape(25.dp))
            .background(Theme.surfaceContainer, RoundedCornerShape(25.dp))
            .clip(RoundedCornerShape(25.dp))
            .confetti(selectedDate == LocalDate.now())
            .padding(20.dp)
    ) {
        Text("Geburtstage", style = TypoStyle(Theme.primary, FontSize.MEDIUM))
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            birthdays.forEach { person ->
                val profilePicture by dayOverviewViewModel.getProfilePicture(person.id).collectAsState(null)
                Box {
                    Column(
                        Modifier
                            .width(80.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .rippleClick{
                                screens.openPersonDetails(
                                    person.id
                                )
                            }
                        ,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .size(70.dp)
                                .clip(MaterialShapes.Cookie9Sided.toShape())
                        ) {
                            val image = remember(profilePicture) { profilePicture?.asImageBitmap() }
                            if(image != null) {
                                Image(
                                    image,
                                    person.name,
                                    Modifier
                                        .fillMaxSize()
                                )
                            } else {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(OldColors.TERTIARY)
                                )
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(person.name, style = TypoStyle(Theme.secondary, FontSize.MEDIUM), textAlign = TextAlign.Center)
                    }
                    Text(
                        "${ChronoUnit.YEARS.between(LocalDate.ofEpochDay(person.birthday?:0L), selectedDate)}." ,
                        style = TypoStyle(Theme.secondary, FontSize.MLARGE, FontFamily.Display),
                        textAlign = TextAlign.Center
                    )
                }
            }
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
        FeelingsBlockItem("Zufriedenheit", happyness, OldColors.HAPPYNESS, true)
        FeelingsBlockItem("Stress", stress, OldColors.STRESS, false)
        FeelingsBlockItem("Produktivität", successfulness, OldColors.PRODUCTIVITY, true)
    }
}
@Composable
fun FeelingsBlockItem(name: String, value: Int?, color: Color, fromLeft: Boolean){
    Box (
        Modifier
            .fillMaxWidth()
            .background(Theme.surfaceContainer, CircleShape)
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
                Text(name, style = TypoStyle(Theme.onPrimaryContainer, FontSize.LARGE))
                Text(value?.toString()?:"--", style = TypoStyle(Theme.onPrimaryContainer, FontSize.DISPLAY, FontFamily.Display).copy(fontWeight = FontWeight.Bold))
            } else {
                Text(value?.toString()?:"--", style = TypoStyle(Theme.onPrimaryContainer, FontSize.DISPLAY, FontFamily.Display).copy(fontWeight = FontWeight.Bold))
                Text(name, style = TypoStyle(Theme.onPrimaryContainer, FontSize.LARGE))
            }
        }
    }
}
@Composable
fun DisplayTimeBlock(timeInMs: Long?, openScreenTime: ()->Unit){
    Column (
        Modifier
            .fillMaxWidth()
            .border(1.dp, Theme.outlineVariant,  RoundedCornerShape(25.dp))
            .background(Theme.surfaceContainer, RoundedCornerShape(25.dp))
            .clip(RoundedCornerShape(25.dp))
            .rippleClick(run = openScreenTime)
            .padding(20.dp)
    ) {
        Text("Bildschirmzeit", style = TypoStyle(Theme.primary, FontSize.MEDIUM))
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .clip(CircleShape)
        ) {
            Box( // Used for slight rounding errors at the edge
                Modifier
                    .fillMaxSize()
                    .height(50.dp)
                    .background(Theme.surfaceContainerHighest)
            )
            Box(
                Modifier
                    .height(50.dp)
                    .fillMaxWidth(timeInMs?.div(screenTimeGoal)?:0f)
                    .background(OldColors.SCREENTIME, CircleShape)
            )
            Text(
                timeInMs?.formatMsToDuration()?:"--h --m --s",
                style = TypoStyle(Theme.onPrimaryContainer, FontSize.XLARGE, FontFamily.Display),
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
            .border(1.dp, Theme.outlineVariant,  RoundedCornerShape(25.dp))
            .background(Theme.surfaceContainer, RoundedCornerShape(25.dp))
            .clip(RoundedCornerShape(25.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(painterResource(R.drawable.walk), "Walk", Modifier.size(50.dp, 50.dp), Theme.primary)
            Box(
                Modifier
                    .height(50.dp)
                    .fillMaxWidth()
                    .background(Theme.surfaceContainerHighest, CircleShape)
                    .clip(CircleShape)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(steps?.div(stepsGoal)?.coerceIn(0f, 1f)?:0f)
                        .height(50.dp)
                        .background(OldColors.STEPS, CircleShape)
                )
                Text(
                    steps?.toString()?:"Keine",
                    Modifier
                        .align(
                            Alignment.CenterEnd
                        )
                        .padding(horizontal = 25.dp)
                    ,
                    style = TypoStyle(Theme.onPrimaryContainer, FontSize.XLARGE, FontFamily.Display)
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
            ,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Schritte", style = TypoStyle(Theme.primary, FontSize.MEDIUM).copy(fontWeight = FontWeight.Bold))
            Text("${if(steps!=null) "ca. ${(steps*.7).toInt().formatMToDistance()} · " else ""}${((steps?:0L)/ stepsGoal *100).toInt()}%", style = TypoStyle(Theme.primary, FontSize.MEDIUM).copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun BankingBlock(bankingEntries: List<BankingRepo.BankingDisplayEntity>, openBankingMenu: ()->Unit){
    Column (
        Modifier
            .fillMaxWidth()
            .border(1.dp, Theme.outlineVariant,  RoundedCornerShape(25.dp))
            .background(Theme.surfaceContainer, RoundedCornerShape(25.dp))
            .clip(RoundedCornerShape(25.dp))
            .rippleClick(run = openBankingMenu)
            .padding(20.dp)
    ) {
        Text("Überweisungen", style = TypoStyle(Theme.primary, FontSize.MEDIUM))
        Spacer(Modifier.height(10.dp))
        val sum = bankingEntries.sumOf { it.entity.amountCents }
        Text(
            sum.formatCents(),
            style = TypoStyle(if(sum >= 0) OldColors.Transactions.PLUS else OldColors.Transactions.MINUS, FontSize.XLARGE, FontFamily.Display),
        )
    }
}
fun getMonthByCalendarMonth(month: Int): String{
    return listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Juni", "Juli", "Aug", "Sep", "Okt", "Nov", "Dez")[month]
}
fun getWeekDayByInt(day: Int): String {
    return listOf("Mon", "Di", "Mi", "Do", "Fr", "Sa", "So")[day]
}