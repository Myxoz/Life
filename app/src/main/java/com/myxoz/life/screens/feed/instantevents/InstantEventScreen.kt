package com.myxoz.life.screens.feed.instantevents

import android.icu.util.Calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalStorage
import com.myxoz.life.Theme
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.formatMinutes
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.viewmodels.CalendarViewModel
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InstantEventsScreen(start: Long, end: Long, calendarViewModel: CalendarViewModel){
    Scaffold(
        containerColor = Theme.background
    ) { innerPadding ->
        val calendar = Calendar.getInstance()
        val nav = LocalNavController.current
        val db = LocalStorage.current
        val instantEvents = remember {
            runBlocking {
                InstantEvent.getEntriesBetween(
                    db,
                    start - 1,
                    end + 1,
                    calendarViewModel
                ).flatMap { it.instantEvents }
            }
        }
        FlowRow(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
            ,
            maxItemsInEachRow = 2,
            verticalArrangement = Arrangement.Bottom,
            horizontalArrangement = Arrangement.End
        ) {
            instantEvents.forEachIndexed { i, it ->
                val mod = Modifier
                    .padding(8.dp)
                    .border(1.dp, Theme.outlineVariant, RoundedCornerShape(20.dp))
                    .background(Theme.surfaceContainer, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .rippleClick{
                        it.openDetails(nav)
                    }
                    .padding(vertical = 20.dp)
                Column(
                    if(i==0 && instantEvents.size % 2 != 0) {
                        mod.fillMaxWidth()
                    } else {
                        mod.weight(1f)
                    },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val fontSize = FontSize.DISPLAY.size.toDp()
                    Icon(painterResource(it.icon), it.subText, Modifier.size(fontSize), Theme.secondary)
                    Spacer(Modifier.height(10.dp))
                    Text(it.timestamp.formatMinutes(calendar), style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
                    Spacer(Modifier.height(5.dp))
                    Text(it.subText, style = TypoStyle(Theme.primary, FontSize.LARGE, FontFamily.Display))
                }
            }
        }
    }
}