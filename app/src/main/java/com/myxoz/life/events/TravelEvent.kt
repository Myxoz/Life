package com.myxoz.life.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.myxoz.life.LocalStorage
import com.myxoz.life.api.jsonObjArray
import com.myxoz.life.dbwrapper.EventEntity
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.dbwrapper.TravelEntity
import com.myxoz.life.dbwrapper.VehicleEntity
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.events.additionals.TimedTagLikeContainer
import com.myxoz.life.events.additionals.Vehicle
import com.myxoz.life.ui.ArrowDirection
import com.myxoz.life.ui.drawArrowBehind
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.utils.toPx
import com.myxoz.life.utils.toSp
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import org.json.JSONObject

class TravelEvent(
    start: Long,
    end: Long,
    uss: Boolean,
    usl: Boolean,
    val from: Long,
    val to: Long,
    val vehicles: List<TimedTagLikeContainer<Vehicle>>
): ProposedEvent(start, end, EventType.Travel, uss, usl) {
    override suspend fun saveEventSpecifics(db: StorageManager, id: Long): Boolean {
        db.travel.insertEvent(
            TravelEntity(
                id,
                from,
                to
            )
        )
        vehicles.forEach {
            db.vehicle.insertVehicle(
                VehicleEntity(
                    id,
                    it.type.id,
                    it.durationMs.toInt()
                )
            )
        }
        return true
    }

    @Composable
    override fun BoxScope.RenderContent(
        oneHourDp: Dp,
        startOfDay: Long,
        endOfDay: Long,
        isSmall: Boolean,
        blockHeight: Int
    ) {
        val db = LocalStorage.current
        val blockHeight =  getBlockHeight(startOfDay, endOfDay)
        var fromDisplay by remember { mutableStateOf("Von") }
        var toDisplay by remember { mutableStateOf("Nach") }
        val density = LocalDensity.current
        val size = when(blockHeight){
            1 -> ((0.8f) * oneHourDp/4f)
            2 -> ((0.9f) * oneHourDp/4f * 1.25f)
            3 -> ((0.9f) * oneHourDp/4f * 1.5f)
            4 -> ((0.9f) * oneHourDp/4f * 1.333f)
            5,6 -> ((0.9f) * oneHourDp/4f * 1.5f)
            else -> 10.dp
        }
        val fontSize = size.toSp(density)
        LaunchedEffect(Unit) {
            with(Dispatchers.IO){
                db.location.getLocation(from)?.name?.also { fromDisplay = it }
                db.location.getLocation(to)?.name?.also { toDisplay = it }
            }
        }
        when(blockHeight){
            in -Int.MIN_VALUE..2 -> {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 3.dp)
                    ,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(
                        fromDisplay,
                        color = Colors.Calendar.Travel.TEXT,
                        fontSize = fontSize,
                        fontWeight = FontWeight.W900
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        vehicles.forEach {
                            Icon(painterResource(it.type.drawable), it.type.name, Modifier.run{if(it.type == Vehicle.Bus) width(2f*size).height(size) else size(size)}, Colors.Calendar.Travel.SECONDARY)
                        }
                        Spacer(Modifier/*.width(3.dp) */)
                        Box(Modifier.width(1.5*size).height(size).drawArrowBehind(ArrowDirection.Right, size.toPx()/2, Colors.Calendar.Travel.TEXT))
                    }
                    Text(
                        toDisplay,
                        color = Colors.Calendar.Travel.TEXT,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            3 -> {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 5.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(fromDisplay, color = Colors.Calendar.Travel.TEXT, fontSize = fontSize, fontWeight = FontWeight.Bold)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            vehicles.forEach {
                                Icon(painterResource(it.type.drawable), it.type.name, Modifier.run{if(it.type == Vehicle.Bus) width(2f*size).height(size) else size(size)}, Colors.Calendar.Travel.SECONDARY)
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.width(1.5*size).height(size).drawArrowBehind(ArrowDirection.Right, size.toPx()/2, Colors.Calendar.Travel.TEXT))
                        Spacer(Modifier.width(3.dp))
                        Text(toDisplay, color = Colors.Calendar.Travel.TEXT, fontSize = fontSize, fontWeight = FontWeight.Bold)
                    }
                }
            }
            else -> {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 5.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(fromDisplay, color = Colors.Calendar.Travel.TEXT, fontSize = fontSize, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.width(2*size).fillMaxHeight().drawArrowBehind(ArrowDirection.Down, size.toPx()/(12/blockHeight.coerceAtMost(6)), Colors.Calendar.Travel.TEXT))
                        Row(
                            Modifier
                                .run{
                                    if(blockHeight > 6) offset(x = -2*size).background(Colors.Calendar.Travel.BG, CircleShape) else this
                                }
                            ,
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            vehicles.forEach {
                                Icon(painterResource(it.type.drawable), it.type.name, Modifier.run{if(it.type == Vehicle.Bus) width(2f*size).height(size) else size(size)}, Colors.Calendar.Travel.SECONDARY)
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.width(3.dp))
                        Text(toDisplay, color = Colors.Calendar.Travel.TEXT, fontSize = fontSize, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    override suspend fun eraseEventSpecificsFromDB(db: StorageManager, id: Long) {
        db.travel.removeById(id)
        db.vehicle.removeById(id)
    }
    override fun addEventSpecifics(jsonObject: JSONObject): JSONObject = jsonObject
        .put("from", from.toString())
        .put("to", to.toString())
        .put("vehicles", JSONArray().apply { vehicles.forEach { put(it.timedTagLikeToJson()) } })

    override fun copyWithTimes(start: Long, end: Long, uss: Boolean, usl: Boolean) = TravelEvent(start, end, uss, usl, from, to, vehicles)
    override fun getInvalidReason(): String? =
        if(from == 0L)
            "Spezifiziere den Abfahrtsort"
        else if(to == 0L)
            "Spezifiziere den Ankunftsort"
        else if(vehicles.isEmpty())
            "Wähle mindestens ein Verkehrsmittel"
        else null

    companion object {
        fun fromJson(json: JSONObject, start: Long, end: Long, uss: Boolean, usl: Boolean) =
            TravelEvent(
                start, end, uss, usl, json.getString("from").toLong(),
                json.getString("to").toLong(),
                json.getJSONArray("vehicles").jsonObjArray.mapNotNull {
                    TimedTagLikeContainer(
                        Vehicle.getById(it.getInt("type")) ?: return@mapNotNull null,
                        it.getLong("duration_ms")
                    )
                }
            )

        suspend fun from(db: StorageManager, event: EventEntity): TravelEvent? {
            val ev = db.travel.getEvent(event.id)?:return null
            val ve = db.vehicle.getEvent(event.id)
            return TravelEvent(
                event.start,
                event.end,
                event.uss,
                event.usl,
                ev.from,
                ev.to,
                ve.mapNotNull { TimedTagLikeContainer(
                    Vehicle.getById(it.type)?:return@mapNotNull null, it.durationMs.toLong()
                )}
            )
        }
    }
}