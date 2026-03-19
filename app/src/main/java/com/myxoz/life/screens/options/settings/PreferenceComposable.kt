package com.myxoz.life.screens.options.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalSettings
import com.myxoz.life.Theme
import com.myxoz.life.screens.feed.dayoverview.edgeToEdgeGradient
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.Settings

@Composable
fun PreferenceComposable() {
    val innerPadding = windowPadding
    Box(
        Modifier
            .background(Theme.background)
            .fillMaxSize()
            .edgeToEdgeGradient(Theme.background, innerPadding)
            .verticalScroll(rememberScrollState())
        ,
        Alignment.TopCenter
    ) {
        Column(
            Modifier
                .setMaxTabletWidth()
            ,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(Modifier.height(innerPadding.calculateTopPadding()))
            val settings = LocalSettings.current
            DayDisplaySelector(settings)
            Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }
}

@Composable
private fun DayDisplaySelector(settings: Settings){
    val displayedDaysOption by settings.preferences.displayedDaysOptions.flow.collectAsState()
    Text("Mögliche Tagauswahl", style = TypoStyle(Theme.primary, FontSize.LARGE))
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        repeat(20) { num ->
            if(num == 0) return@repeat
            val isOn = (1.shl(num).and(displayedDaysOption) != 0)
            Box(
                Modifier
                    .padding(10.dp)
                    .aspectRatio(1f)
                    .background(if(isOn) Theme.primary else Theme.primaryContainer, CircleShape)
                    .clip(CircleShape)
                    .rippleClick{
                        val mask = 1.shl(num)
                        settings.preferences.displayedDaysOptions.put(if(isOn) displayedDaysOption.and(mask.inv()) else displayedDaysOption.or(mask))
                    }
                    .padding(10.dp)
                ,
                contentAlignment = Alignment.Center
            ) {
               Text(num.toString(), style = TypoStyle(if(isOn) Theme.onPrimary else Theme.onPrimaryContainer, FontSize.MEDIUM))
            }
        }
    }
}