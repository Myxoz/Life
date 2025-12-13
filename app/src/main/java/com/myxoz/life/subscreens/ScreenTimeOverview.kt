package com.myxoz.life.subscreens

import android.content.pm.PackageManager
import android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.myxoz.life.calendar.feed.msToDisplay
import com.myxoz.life.options.getMappedUsageDataBetween
import com.myxoz.life.rippleClick
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ScreenTimeOverview(epochDay: Long){
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
                getMappedUsageDataBetween(
                    context,
                    LocalDate.ofEpochDay(epochDay).atStartOfDay(zone).toEpochSecond()*1000L,
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
            screenTime.add(
                AppItem(
                    "",
                    db.days.getDay(epochDay.toInt())
                        ?.screenTimeMs?.minus(db.dayScreenTime.getScreenTimesByDay(epochDay).sumOf { it.duration }) ?: 0L
                )
            )
        }
    }
    Scaffold(
        Modifier.fillMaxSize(),
        containerColor = Colors.BACKGROUND
    ) { innerPadding ->
        Column(
            Modifier
                .edgeToEdgeGradient(Colors.BACKGROUND, innerPadding)
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val date = LocalDate.ofEpochDay(epochDay)
            val dateStr = "${date.dayOfMonth}.${date.month.value}.${date.year}"
            Spacer(Modifier.height(innerPadding.calculateTopPadding()))
            Text(
                "Screentime $dateStr",
                style = TypoStyle(FontColor.SECONDARY, FontSize.XLARGE, FontFamily.Display).copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                if (!showsMore) "Top 5 Apps" else "Alle Apps",
                style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM)
            )
            val pm = remember { context.packageManager }
            screenTime.forEach {
                AppItemComposable(it, pm, !showsMore || isToday)
            }
            val context = LocalContext.current
            if (!showsMore) Text(
                "Alle berechnen",
                style = TypoStyle(FontColor.SELECTED, FontSize.MEDIUM),
                modifier = Modifier
                    .padding(10.dp)
                    .clip(CircleShape)
                    .rippleClick {
                        val zone = ZoneId.systemDefault()
                        screenTime.clear()
                        getMappedUsageDataBetween(
                            context,
                            LocalDate.ofEpochDay(epochDay).atStartOfDay(zone)
                                .toEpochSecond() * 1000L,
                            LocalDate.ofEpochDay(epochDay + 1).atStartOfDay(zone)
                                .toEpochSecond() * 1000L
                        )
                            .entries
                            .sortedBy { -it.value }
                            .forEach {
                                screenTime.add(AppItem(it.key, it.value))
                            }
                        showsMore = true
                    }
            )
            Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }
}
data class AppItem(val packageName: String, val duration: Long)
@Composable
fun AppItemComposable(item: AppItem, pm: PackageManager, shouldWait: Boolean){
    var icon: ImageBitmap? by remember(item.packageName) { mutableStateOf(null) }
    var recoveredName: String? by remember(item.packageName) { mutableStateOf(null) }
    var showsPackageName by remember(item.packageName) { mutableStateOf(false) }
    LaunchedEffect(item.packageName) {
        if(shouldWait) delay(100L)
        if(item.packageName=="") {
            recoveredName="Other"
            return@LaunchedEffect
        }
        try {
            val info = pm.getApplicationInfo(item.packageName, MATCH_UNINSTALLED_PACKAGES)
            recoveredName = info.loadLabel(pm).toString()
            val rawIcon = pm.getApplicationIcon(info)
            icon = rawIcon.toBitmap(64, 64).asImageBitmap()
        } catch(_: Exception){ }
    }
    Row(
        Modifier.fillMaxWidth().rippleClick{showsPackageName=!showsPackageName},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            icon?.let {
                Image(icon!!, recoveredName?:item.packageName, Modifier
                    .size(40.dp)
                )
            } ?: Box(
                Modifier.background(Colors.SECONDARY, CircleShape).size(40.dp)
            )
            Text((if(showsPackageName) null else recoveredName)?:item.packageName, style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM))
        }
        Text(item.duration.toInt().msToDisplay(), style = TypoStyle(FontColor.PRIMARY, FontSize.MEDIUM))
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