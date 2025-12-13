package com.myxoz.life.events

import android.content.Context
import android.content.Context.MODE_PRIVATE
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.core.content.edit
import com.myxoz.life.R
import com.myxoz.life.dbwrapper.EventEntity
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.ui.theme.Colors
import org.json.JSONObject

@Composable
fun BoxScope.SleepEventComposable(event: SleepEvent, oneHourDp: Dp, startOfDay: Long, endOfDay: Long, isSmall: Boolean, blockHeight: Int) {
    val blockEvent = event.getBlockLength()
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
            tint = Colors.SECONDARYFONT
        )
        Text(
            "${if(blockEvent>=4) "${blockEvent/4}h " else ""}${if(blockEvent%4!=0) "${(blockEvent%4)*15}m" else ""}",
            fontSize = ((blockHeight*.9f).coerceIn(0f, if(!isSmall) 2f else 1.25f)*oneHourDp/4).toSp(),
            color = Colors.Calendar.Sleep.FG,
            fontWeight = FontWeight.Bold
        )
    }
}
class SleepEvent(start: Long, end: Long, uss: Boolean, usl: Boolean): ProposedEvent(start, end, EventType.Sleep, uss, usl) {
    override suspend fun saveEventSpecifics(db: StorageManager, id: Long): Boolean = true
    override suspend fun eraseEventSpecificsFromDB(db: StorageManager, id: Long) = Unit
    override fun addEventSpecifics(jsonObject: JSONObject): JSONObject = jsonObject // No specific parts
    override fun copyWithTimes(start: Long, end: Long, uss: Boolean, usl: Boolean) = SleepEvent(start, end, uss, usl)
    override fun ignoreProposed(db: StorageManager, context: Context) {
        val declinedSleep = context.getSharedPreferences("autodetect", MODE_PRIVATE)
        val declinedSleepSet = (declinedSleep.getStringSet("declined_sleep", null) ?: setOf<String>()).toMutableSet()
        declinedSleepSet.add("$start;$end")
        declinedSleep.edit {
            putStringSet("declined_sleep", declinedSleepSet)
        }
    }

    override fun getInvalidReason(): String? = null // Sleep cant be invalid

    companion object {
        fun fromJson(json: JSONObject, start: Long, end: Long, uss: Boolean, usl: Boolean): ProposedEvent = SleepEvent(
            start, end, uss, usl
        )
        fun from(db: StorageManager, event: EventEntity) = SleepEvent(event.start, event.end, event.uss, event.usl)
    }
}
@Composable
fun Dp.toSp() = with(LocalDensity.current) { this@toSp.toPx() / fontScale / density }.sp

fun Dp.toSp(density: Density) = with(density) { this@toSp.toPx() / fontScale / this.density }.sp

@Composable
fun Dp.toPx() = with(LocalDensity.current) {this@toPx.toPx()}

fun Dp.toPx(density: Density) = with(density) {this@toPx.toPx()}
