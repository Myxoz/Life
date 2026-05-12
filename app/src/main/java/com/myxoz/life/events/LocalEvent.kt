package com.myxoz.life.events

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.utils.rippleClick
import org.json.JSONObject

data class LocalEvent(
    val localId: Long,
    val raw: RawEvent,
) {
    fun specificsToJson(): JSONObject =
        raw.toJson()
            .put("localId", localId.toString())

    fun asSynced() = SyncedEvent(API.generateId(), System.currentTimeMillis(), null, raw)

    // override fun getInvalidReason(): String? = raw.getInvalidReason()

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Render(oneHour: Dp, startOfDay: Long, endOfDay: Long, removeProposedEvent: ()->Unit, syncEvent: ()->Unit){
        Box(
            Modifier
                .padding(top = raw.getTopPadding(oneHour, startOfDay))
                .height(raw.getHeightDp(oneHour, startOfDay, endOfDay))
                .background(raw.type.colors.bg.copy(.5f), RoundedCornerShape(10.dp))
                .fillMaxWidth()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .alpha(.5f)
            ) {
                with(raw) {
                    RenderContent(oneHour, startOfDay, endOfDay, false,
                        raw.getBlockHeight(startOfDay, endOfDay)
                    )
                }
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .fillMaxSize(),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(OldColors.ACCEPT)
                        .clip(RoundedCornerShape(10.dp))
                        .rippleClick {
                            syncEvent()
                        }
                )
                Box(
                    Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(OldColors.DECLINE)
                        .clip(RoundedCornerShape(10.dp))
                        .combinedClickable(onLongClick = {
                            removeProposedEvent()
                        }) {}
                )
            }
        }
    }
    companion object {
        fun deterministicId(raw: RawEvent) = LocalEvent(
            (31L * raw.start + raw.end) * 31 + raw.type.id,
            raw
        )
        fun fromJSON(json: JSONObject): LocalEvent {
            return LocalEvent(
                json.getString( "localId").toLong(),
                RawEvent.fromJSON(json)
            )
        }
//        fun from(preparedEventContent: RawEvent.PreparedEventContent): SyncedEvent? {
//            return SyncedEvent(
//                preparedEventContent.event.id,
//                preparedEventContent.event.created,
//                preparedEventContent.event.edited,
//                RawEvent.from(preparedEventContent) ?: return null
//            )
//        }
//
//        override fun fromJSON(json: JSONObject): SyncedEvent =
//            SyncedEvent(
//                json.getId(),
//                json.getSafeLong("created"),
//                json.getLongOrNull("edited"),
//                RawEvent.fromJSON(json)
//            )
//        fun getSemanticNullValueEvent() =
//            SyncedEvent(-1L, 0L, null, EmptyEvent(0L, 0L, false, usl = false))
//    }
    }
}
