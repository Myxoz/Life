package com.myxoz.life.ui.alarm.screens.composables

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.myxoz.life.MainActivity
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.events.DigSocEvent
import com.myxoz.life.events.TravelEvent
import com.myxoz.life.events.additionals.TagEvent
import com.myxoz.life.events.additionals.TitleEvent
import com.myxoz.life.repositories.supermodule.CrossRepoSuper
import com.myxoz.life.ui.AlarmUI
import com.myxoz.life.ui.ArrowDirection
import com.myxoz.life.ui.LocalScreensProvider
import com.myxoz.life.ui.NavPath
import com.myxoz.life.ui.drawArrowBehind
import com.myxoz.life.ui.feed.main.RenderTagAndTitleBar
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toPx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AlarmUI.RenderEventPreview(syncedEvent: SyncedEvent, screens: LocalScreensProvider?, crossRepoSuper: CrossRepoSuper) {
    val height = 40.dp
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val openFullScreenEvent: () -> Unit = if(screens != null)
        ({ screens.openFullScreenEvent(syncedEvent) })
    else
        ({
            coroutineScope.launch {
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra("targetRoute", NavPath.FULLSCREEN_EVENT.with(syncedEvent.id))
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
            .background(syncedEvent.raw.type.colors.bg, CircleShape)
            .clip(CircleShape)
            .rippleClick{
                openFullScreenEvent()
            }
            .padding(5.dp, 5.dp, 10.dp, 5.dp)
    ) {
        when(val event = syncedEvent.raw) {
            is TagEvent -> RenderTagAndTitleBar(
                event.eventTags,
                if (event is TitleEvent) event.title else null,
                height,
                3,
                event.type.colors
            )
            is DigSocEvent -> {
                val people by crossRepoSuper.getPeople(event.people).collectAsState()
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
                val from by crossRepoSuper.getLocationById(event.from).collectAsState()
                val to by crossRepoSuper.getLocationById(event.to).collectAsState()
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