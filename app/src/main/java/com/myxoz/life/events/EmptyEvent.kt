package com.myxoz.life.events

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.events.additionals.EventType
import org.json.JSONObject

class EmptyEvent(start: Long, end: Long, uss: Boolean, usl: Boolean): ProposedEvent(start, end, EventType.Empty, uss, usl) {
    override suspend fun saveEventSpecifics(db: StorageManager, id: Long) = false

    @Composable
    override fun BoxScope.RenderContent(
        oneHourDp: Dp,
        startOfDay: Long,
        endOfDay: Long,
        isSmall: Boolean,
        blockHeight: Int
    ) { /* no-op */ }

    override suspend fun eraseEventSpecificsFromDB(db: StorageManager, id: Long) = Unit

    override fun copyWithTimes(start: Long, end: Long, uss: Boolean, usl: Boolean) = EmptyEvent(start, end, uss, usl)
    override fun getInvalidReason(): String = "Wähle einen anderen Eventtyp"

    override fun addEventSpecifics(jsonObject: JSONObject) = jsonObject
}
