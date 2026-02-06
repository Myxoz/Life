package com.myxoz.life.screens.feed.dayoverview

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.dbwrapper.DayScreenTimeEntity
import com.myxoz.life.screens.feed.main.formatMsToDuration
import com.myxoz.life.ui.rememberAsymmetricalVerticalCornerRadius
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.DayOverviewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun ScreenTimeOverview(date: LocalDate, dayOverviewViewModel: DayOverviewViewModel){
    val isToday = remember { LocalDate.now() == date }
    val daySummary by dayOverviewViewModel.getDaySummary(date).collectAsState(null)
    var showsMore by remember { mutableStateOf(isToday) }
    val displayedEntries by if(showsMore) {
        dayOverviewViewModel.getScreentimeLive(date)
    } else {
        remember(daySummary) { MutableStateFlow(daySummary?.topApps?.sortedByDescending { it.duration }?:listOf()) }
        // Thats how you ductape stuff
    }.collectAsStateWithLifecycle(listOf())
    val accumulatedTime by dayOverviewViewModel.getScreentime(date).collectAsStateWithLifecycle(0L)
    val context = LocalContext.current
    val innerPadding = windowPadding
    Box(
        Modifier
            .edgeToEdgeGradient(Theme.background, innerPadding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
        ,
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier
                .setMaxTabletWidth()
                .fillMaxHeight()
            ,
        ) {
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
                displayedEntries.forEachIndexed { i, it  ->
                    AppItemComposable(it, pm, i == 0, i == displayedEntries.size - 1 && showsMore)
                }
            }
            Spacer(Modifier.height(3.dp))
            AnimatedVisibility(!showsMore) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(rememberAsymmetricalVerticalCornerRadius(displayedEntries.isEmpty(), true))
                        .rippleClick {
                            showsMore = true
                        }
                        .background(Theme.surfaceContainerHigh)
                        .padding(10.dp)
                    ,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painterResource(R.drawable.drop_down),
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
                    Text((accumulatedTime - displayedEntries.sumOf { it.duration }).formatMsToDuration(), style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
                }
            }
            Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }
}
@Composable
private fun AppItemComposable(item: DayScreenTimeEntity, pm: PackageManager, isFirst: Boolean, isLast: Boolean){
    var icon: ImageBitmap? by remember(item.packagename) { mutableStateOf(null) }
    var recoveredName: String? by remember(item.packagename) { mutableStateOf(null) }
    var showsPackageName by remember(item.packagename) { mutableStateOf(false) }
    LaunchedEffect(item.packagename) {
        val scale = coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
        // Wait for animations to finish before trying to load icons
        // delay((300 * scale).toLong())
        val ic = withContext(Dispatchers.IO) {
            if(item.packagename=="") {
                recoveredName="Other"
                return@withContext null
            }
            try {
                val info = pm.getApplicationInfo(item.packagename, MATCH_UNINSTALLED_PACKAGES)
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
                Image(it, recoveredName?:item.packagename, Modifier
                    .size(FontSize.LARGE.size.toDp() + 10.dp)
                )
            } ?: Box(
                Modifier.background(OldColors.SECONDARY, CircleShape).size(FontSize.LARGE.size.toDp() + 10.dp)
            )
            Text((if(showsPackageName) null else recoveredName)?:item.packagename, style = TypoStyle(Theme.primary, FontSize.LARGE))
        }
        Text(item.duration.formatMsToDuration(), style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
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