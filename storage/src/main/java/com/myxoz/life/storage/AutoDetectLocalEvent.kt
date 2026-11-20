package com.myxoz.life.storage

import com.myxoz.life.storage.api.syncables.SyncableIds.getEventCalendarId
import com.myxoz.life.storage.events.RawEvent

data class AutoDetectLocalEvent(
    val localId: Long,
    val raw: RawEvent
) {
    companion object {
        fun deterministicFrom(raw: RawEvent) = AutoDetectLocalEvent(
            (31L * raw.eventTime.start.toEpochMilli() + raw.eventTime.end.toEpochMilli()) * 31 + raw.getEventCalendarId(),
            raw
        )
    }
}