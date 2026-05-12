package com.myxoz.life.screens.streaks

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalNavController
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.api.extensions.StreakExtendable
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.screens.NavPath
import com.myxoz.life.screens.feed.fullscreenevent.CalendarChip
import com.myxoz.life.screens.feed.fullscreenevent.InputField
import com.myxoz.life.screens.feed.fullscreenevent.PersonBar
import com.myxoz.life.screens.feed.fullscreenevent.TagsBar
import com.myxoz.life.screens.person.displayperson.ButtonGroup
import com.myxoz.life.ui.getMaxTabletScreenWidth
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.collectAsMutableState
import com.myxoz.life.utils.nullIfEmpty
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.StreakViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun EditStreaksScreen(streakViewModel: StreakViewModel){
    var rawStreak by streakViewModel.editingStreak.collectAsMutableState()
    val streak = rawStreak ?: return
    val nav = LocalNavController.current
    Box(
        Modifier
            .background(Theme.background)
        ,
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .setMaxTabletWidth()
            ,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                ,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(Modifier.height(windowPadding.calculateTopPadding()))
                InputField(
                   streak.title.nullIfEmpty(),
                   "Titel"
                ) {
                    rawStreak = rawStreak?.copy(title = it) ?: return@InputField
                }
                InputField(
                    streak.smallTitle?.nullIfEmpty(),
                    "Kurzer Titel (Widgets)"
                ) {
                    rawStreak = streak.copy(smallTitle = it.nullIfEmpty())
                }
                ButtonGroup(
                    StreakExtendable.Timespan.entries.map { it.displayName },
                    getMaxTabletScreenWidth(),
                    remember { MutableStateFlow(streak.timespan.ordinal) }
                ) { newTimespan ->
                    rawStreak = rawStreak?.copy(timespan = StreakExtendable.Timespan.entries.first { it.ordinal == newTimespan })
                }
                ButtonGroup(
                    listOf("Mindestens", "Maximal"),
                    getMaxTabletScreenWidth(),
                    remember { MutableStateFlow(if(streak.atLeast) 0 else 1) }
                ) {
                    rawStreak = rawStreak?.copy(atLeast = it == 0)
                }
                InputField(
                    streak.target.thresholdToString(streak.threshold),
                    "Grenze",
                    visualTransformation = streak.target.visualTransformation.value,
                    keyboardType = KeyboardType.Number
                ) {
                    rawStreak = rawStreak?.copy(threshold = streak.target.stringToThreshold(it))
                }
                ButtonGroup(
                    listOf("Event", "Tageszusammenfassung"),
                    getMaxTabletScreenWidth(),
                    remember { MutableStateFlow(if(streak.target is StreakExtendable.StreakTarget.EventTarget) 0 else 1) }
                ) {
                    rawStreak = rawStreak?.copy(target =
                        if(it == 0)
                            StreakExtendable.StreakTarget.EventTarget(
                                false,
                                EventType.Empty,
                                listOf(),
                                listOf(),
                                null
                            )
                        else
                            StreakExtendable.StreakTarget.DayTarget(
                                StreakExtendable.DayTargetType.Steps
                            )
                    )
                }
                when(streak.target) {
                    is StreakExtendable.StreakTarget.DayTarget -> {
                        ButtonGroup(
                            StreakExtendable.DayTargetType.entries.map { it.displayName },
                            getMaxTabletScreenWidth(),
                            remember { MutableStateFlow(StreakExtendable.DayTargetType.Steps.ordinal) }
                        ) { target ->
                           rawStreak = rawStreak?.copy(target = StreakExtendable.StreakTarget.DayTarget(StreakExtendable.DayTargetType.entries.first {it.ordinal == target}))
                        }
                    }
                    is StreakExtendable.StreakTarget.EventTarget -> {
                        ButtonGroup(
                            listOf("Eventlänge", "Eventanzahl"),
                            getMaxTabletScreenWidth(),
                            remember { MutableStateFlow(if(streak.target.searchEventAmount) 1 else 0) }
                        ) {
                            rawStreak = rawStreak?.copy(target = streak.target.copy(searchEventAmount = it == 1))
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            for (type in EventType.order) {
                                CalendarChip(type, streak.target.type == type) {
                                    rawStreak = rawStreak?.copy(target = streak.target.copy(type = type))
                                }
                            }
                        }
                        TagsBar(streak.target.tags) {
                            rawStreak = rawStreak?.copy(target = streak.target.copy(tags = it))
                        }
                        PersonBar(
                            streak.target.people,
                            false,
                            false,
                            { nav.navigate(NavPath.Menu.CONTACTS) }
                        ) { people, _ ->
                            rawStreak = rawStreak?.copy(target = streak.target.copy(people = people))
                        }
                        InputField(
                            streak.target.title,
                            "Eventtitel"
                        ) {
                            rawStreak = rawStreak?.copy(target = streak.target.copy(title = it.nullIfEmpty()))
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                ,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val nav = LocalNavController.current
                Box(
                    Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .rippleClick{
                            nav.popBackStack()
                        }
                        .padding(vertical = 20.dp)
                    ,
                    contentAlignment = Alignment.Center
                ) {
                    Text("Abbrechen", style = TypoStyle(Theme.primary, FontSize.LARGE))
                }
                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current
                Box(
                    Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(Theme.primary)
                        .rippleClick {
                            val invalid = streak.getInvalidReason()
                            if(invalid != null) {
                                Toast.makeText(context, invalid, Toast.LENGTH_SHORT).show()
                                return@rippleClick
                            }
                            coroutineScope.launch {
                                streakViewModel.saveAndSyncStreak(rawStreak ?: return@launch)
                            }
                            nav.popBackStack()
                        }
                        .padding(vertical = 20.dp)
                    ,
                    contentAlignment = Alignment.Center
                ) {
                    Text("Speichern", style = TypoStyle(Theme.onPrimary, FontSize.LARGE, FontFamily.Display))
                }
            }
            Spacer(Modifier.height(windowPadding.calculateBottomPadding()))
        }
        val coroutineScope = rememberCoroutineScope()
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(windowPadding)
                .padding(10.dp)
                .background(Theme.secondaryContainer, CircleShape)
                .border(1.dp, Theme.outline, CircleShape)
                .clip(CircleShape)
                .rippleClick {
                    coroutineScope.launch {
                        streakViewModel.saveAndSyncStreak(streak, true)
                        nav.popBackStack(NavPath.Menu.STREAK, false)
                    }
                }
                .padding(10.dp)
        ) {
            Icon(
                painterResource(R.drawable.delete),
                "Toggle Icons",
                Modifier.size(20.dp),
                Theme.onSecondaryContainer
            )
        }
    }
}