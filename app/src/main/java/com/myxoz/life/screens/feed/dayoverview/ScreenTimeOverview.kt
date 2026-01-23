package com.myxoz.life.screens.feed.dayoverview

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.pm.PackageManager
import android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.myxoz.life.LocalStorage
import com.myxoz.life.Theme
import com.myxoz.life.screens.feed.main.msToDisplay
import com.myxoz.life.screens.options.getMappedUsageDataBetween
import com.myxoz.life.ui.rememberAsymmetricalVerticalCornerRadius
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

@Composable
fun ScreenTimeOverview(epochDay: Long){
    var totalExceptTopFive by remember { mutableLongStateOf(0L) }
    val db = LocalStorage.current
    val isToday = remember { LocalDate.now().toEpochDay() ==  epochDay }
    val screenTime: SnapshotStateList<AppItem> = remember { mutableStateListOf() }
    var showsMore by remember { mutableStateOf(false) }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if(isToday){
            val zone = ZoneId.systemDefault()
            showsMore = true
            while (true){
                val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)
                val events = usageStatsManager.queryEvents(0, System.currentTimeMillis())
                val e = UsageEvents.Event()

                if (events.hasNextEvent()) {
                    events.getNextEvent(e)
                    println("Earliest event at: ${Date(e.timeStamp)}")
                }
                getMappedUsageDataBetween(
                    context,
                    LocalDate.ofEpochDay(epochDay).atStartOfDay(zone).toInstant().toEpochMilli(),
                    System.currentTimeMillis()
                )
                    .entries
                    .sortedBy { -it.value }
                    .forEach {
                        screenTime.removeIf { a -> a.packageName == it.key }
                        screenTime.add(AppItem(it.key, it.value))
                    }
                delay(1000)
            }
        } else {
            db.dayScreenTime.getScreenTimesByDay(epochDay).sortedBy { -it.duration }.forEach {
                screenTime.add(AppItem(it.packagename, it.duration))
            }
            totalExceptTopFive = db.days.getDay(epochDay.toInt())?.screenTimeMs?.minus(db.dayScreenTime.getScreenTimesByDay(epochDay).sumOf { it.duration }) ?: 0L
        }
    }
    Scaffold(
        Modifier
            .fillMaxSize()
        ,
        containerColor = Theme.background
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
            ,
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .edgeToEdgeGradient(Theme.background, innerPadding)
                    .fillMaxHeight()
                    .fillMaxWidth(.95f)
                    .verticalScroll(rememberScrollState())
                ,
            ) {
                val date = LocalDate.ofEpochDay(epochDay)
                val dateStr = "${date.dayOfMonth}.${date.month.value}.${date.year}"
                Spacer(Modifier.height(innerPadding.calculateTopPadding()+ 10.dp))
                Text(
                    "Screentime $dateStr",
                    style = TypoStyle(Theme.primary, FontSize.XLARGE, FontFamily.Display).copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(Modifier.height(20.dp))
                val pm = remember { context.packageManager }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                    ,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    screenTime.forEachIndexed { i, it  ->
                        AppItemComposable(it, pm, i == 0, i == screenTime.size - 1 && showsMore)
                    }
                }
                Spacer(Modifier.height(3.dp))
                AnimatedVisibility(!showsMore) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(rememberAsymmetricalVerticalCornerRadius(screenTime.isEmpty(), true))
                            .rippleClick {
                                val zone = ZoneId.systemDefault()
                                getMappedUsageDataBetween(
                                    context,
                                    LocalDate.ofEpochDay(epochDay).atStartOfDay(zone)
                                        .toEpochSecond() * 1000L,
                                    LocalDate.ofEpochDay(epochDay + 1).atStartOfDay(zone)
                                        .toEpochSecond() * 1000L
                                )
                                    .entries
                                    .sortedBy { -it.value }
                                    .apply { screenTime.clear() }
                                    .forEach {
                                        screenTime.add(AppItem(it.key, it.value))
                                    }
                                showsMore = true
                            }
                            .background(Theme.surfaceContainerHigh)
                            .padding(10.dp)
                        ,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardArrowUp,
                            "Open",
                            Modifier.size(FontSize.LARGE.size.toDp()  + 10.dp),
                            Theme.primary
                        )
                        Row(
                            Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Other", style = TypoStyle(Theme.primary, FontSize.LARGE))
                        }
                        Text(totalExceptTopFive.toInt().msToDisplay(), style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
                    }
                }
                Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
            }
        }
    }
}
private data class AppItem(val packageName: String, val duration: Long)
@Composable
private fun AppItemComposable(item: AppItem, pm: PackageManager, isFirst: Boolean, isLast: Boolean){
    var icon: ImageBitmap? by remember(item.packageName) { mutableStateOf(null) }
    var recoveredName: String? by remember(item.packageName) { mutableStateOf(null) }
    var showsPackageName by remember(item.packageName) { mutableStateOf(false) }
    LaunchedEffect(item.packageName) {
        val scale = coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
        // Wait for animations to finish before trying to load icons
        // delay((300 * scale).toLong())
        val ic = withContext(Dispatchers.IO) {
            if(item.packageName=="") {
                recoveredName="Other"
                return@withContext null
            }
            try {
                val info = pm.getApplicationInfo(item.packageName, MATCH_UNINSTALLED_PACKAGES)
                recoveredName = info.loadLabel(pm).toString()
                val rawIcon = pm.getApplicationIcon(info)
                return@withContext rawIcon.toBitmap(64, 64).asImageBitmap()
            } catch(_: Exception){
                null
            }
        }
        icon = ic
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(rememberAsymmetricalVerticalCornerRadius(isFirst, isLast, 40))
            .background(Theme.surfaceContainer)
            .rippleClick{showsPackageName=!showsPackageName}
            .padding(10.dp)
        ,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            icon?.let {
                Image(it, recoveredName?:item.packageName, Modifier
                    .size(FontSize.LARGE.size.toDp() + 10.dp)
                )
            } ?: Box(
                Modifier.background(OldColors.SECONDARY, CircleShape).size(FontSize.LARGE.size.toDp() + 10.dp)
            )
            Text((if(showsPackageName) null else recoveredName)?:item.packageName, style = TypoStyle(Theme.primary, FontSize.LARGE))
        }
        Text(item.duration.toInt().msToDisplay(), style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
    }
}
fun Modifier.edgeToEdgeGradient(color: Color, innerPadding: PaddingValues): Modifier {
    val topPadding = innerPadding.calculateTopPadding()
    val bottomPadding = innerPadding.calculateBottomPadding()

    return this.drawWithContent {
        // Draw the content first
        drawContent()
        // Top gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(color, Color.Transparent),
                startY = 0f,
                endY = topPadding.toPx()
            ),
            size = Size(size.width, topPadding.toPx())
        )

        // Bottom gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, color),
                startY = size.height - bottomPadding.toPx(),
                endY = size.height
            ),
            topLeft = Offset(0f, size.height - bottomPadding.toPx()),
            size = Size(size.width, bottomPadding.toPx())
        )
    }
}