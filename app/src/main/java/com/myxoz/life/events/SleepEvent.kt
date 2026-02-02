package com.myxoz.life.events

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.myxoz.life.R
import com.myxoz.life.android.autodetect.AutoDetect
import com.myxoz.life.android.autodetect.AutoDetectSleep
import com.myxoz.life.dbwrapper.events.EventEntity
import com.myxoz.life.dbwrapper.events.ReadEventDetailsDao
import com.myxoz.life.dbwrapper.events.WriteEventDetailsDao
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.utils.toSp
import org.json.JSONObject

class SleepEvent(start: Long, end: Long, uss: Boolean, usl: Boolean): ProposedEvent(start, end, EventType.Sleep, uss, usl), AutoDetect.AutoDetectEvent {
    override suspend fun saveEventSpecifics(writeEventDetailsDao: WriteEventDetailsDao, id: Long): Boolean = true

    @Composable
    override fun BoxScope.RenderContent(
        oneHourDp: Dp,
        startOfDay: Long,
        endOfDay: Long,
        isSmall: Boolean,
        blockHeight: Int
    ) {
        val blockEvent = getBlockLength()
        Row(
            Modifier
                .padding(horizontal = if(isSmall) 2.dp else 5.dp)
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(oneHourDp/4*blockHeight.coerceIn(0,2)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if(isSmall) Arrangement.Center else Arrangement.SpaceBetween
        ) {
            if(!isSmall) Icon(
                painterResource(if(blockHeight == 1) R.drawable.sleep_horr else R.drawable.sleep),
                "Sleep",
                Modifier
                    .height(((blockHeight*.9f).coerceIn(0f, 2f)*oneHourDp/4)),
                tint = OldColors.SECONDARYFONT
            )
            Text(
                "${if(blockEvent>=4) "${blockEvent/4}h " else ""}${if(blockEvent%4!=0) "${(blockEvent%4)*15}m" else ""}",
                fontSize = ((blockHeight*.9f).coerceIn(0f, if(!isSmall) 2f else 1.25f)*oneHourDp/4).toSp(),
                color = OldColors.Calendar.Sleep.FG,
                fontWeight = FontWeight.Bold
            )
        }
    }

    override suspend fun eraseEventSpecificsFromDB(db: WriteEventDetailsDao, id: Long) = Unit
    override fun addEventSpecifics(jsonObject: JSONObject): JSONObject = jsonObject // No specific parts
    override fun copyWithTimes(start: Long, end: Long, uss: Boolean, usl: Boolean) = SleepEvent(start, end, uss, usl)
    override fun ignoreProposed(prefs: SharedPreferences) = ingoreAutoDetectable(this, AutoDetectSleep.SPK, prefs)

    override fun getInvalidReason(): String? = null // Sleep cant be invalid

    companion object {
        fun fromJson(json: JSONObject, start: Long, end: Long, uss: Boolean, usl: Boolean): ProposedEvent = SleepEvent(
            start, end, uss, usl
        )
        fun from(db: ReadEventDetailsDao, event: EventEntity) = SleepEvent(event.start, event.end, event.uss, event.usl)
    }
}