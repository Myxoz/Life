package com.myxoz.life.calendar.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myxoz.life.api.SyncedEvent
import com.myxoz.life.dbwrapper.BankingEntity
import com.myxoz.life.events.additionals.EventType


data class SegmentedEvent(val event: SyncedEvent, val isFullWidth: Boolean, val isLeft: Boolean, val hasContent: Boolean, val segmentStart: Long, val segmentEnd: Long): DefinedDurationEvent(segmentStart, segmentEnd) {
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Render(oneHour: Dp, bankingSizeDp: Dp, startOfDay: Long, endOfDay: Long, width: Dp, isClickEnabled: Boolean, editEvent: ()->Unit, openEventDetails: ()->Unit){
        Box(
            Modifier
                .padding(
                    top = getTopPadding(oneHour, startOfDay),
                    start = if(!isLeft) width-bankingSizeDp else 0.dp
                )
                .height(getHeightDp(oneHour, startOfDay, endOfDay))
                .width(if(isFullWidth) width else if(isLeft) width-bankingSizeDp else bankingSizeDp)
                .clip(RoundedCornerShape(10.dp))
                .combinedClickable(remember { MutableInteractionSource() }, ripple(), isClickEnabled, onClick = openEventDetails, onLongClick = editEvent)
                .background(event.proposed.getBackgroundBrush(), RoundedCornerShape(10.dp))
        ) {
            if(hasContent)
                with(event.proposed) {
                    RenderContent(oneHour, startOfDay, endOfDay, !isLeft, getBlockHeight(startOfDay, endOfDay))
                }
        }
    }
    companion object {
        fun getSegmentedEvents(events: List<SyncedEvent>, bankingEntries: List<BankingEntity>, bankingEntrySize: Long): List<SegmentedEvent> {
            val order = arrayOf(
                EventType.Travel,
                EventType.Learn,
                EventType.Hobby,
                EventType.Spont,
                EventType.Sleep,
                EventType.Social,
                EventType.DigSoc
            )
            val modifyable = mutableListOf<SegmentedEvent>()
            for (type in order) {
                for(event in events.filter { it.proposed.type == type }){
                    var canBeFullWidth = true
                    modifyable.forEachIndexed { i, it ->
                        if(event.proposed.overlaps(it.event.proposed)) {
                            canBeFullWidth = false
                            modifyable[i] = it.copy(isFullWidth = false, isLeft = true)
                        }
                    }
                    modifyable.add(SegmentedEvent(event, canBeFullWidth, canBeFullWidth, hasContent = true, event.proposed.start, event.proposed.end))
                }
            }
            for(event in bankingEntries) {
                modifyable.forEachIndexed { i, it ->
                    if(it.event.proposed.overlaps(
                            DefinedDurationEvent(
                                event.purposeDate!! - bankingEntrySize / 2,
                                event.purposeDate + bankingEntrySize / 2
                            )
                        )) {
                        modifyable[i] = it.copy(isFullWidth = false)  // Does not set left or right align
                    }
                }
            }
            // Now iterate the list and try to fill spots
            for(seg in modifyable.filter { !it.isLeft }){
                val overlappingEvents = modifyable.filter { seg != it && it.event.proposed.overlaps(seg.event.proposed) }.sortedBy { it.event.proposed.end }.toMutableList()
                var parts = mutableListOf<Pair<Long, Long>>()
                var lastEnd = if(overlappingEvents[0].event.proposed.start > seg.event.proposed.start) seg.event.proposed.start else overlappingEvents[0].event.proposed.end
                for(overlap in overlappingEvents){
                    if(overlap.event.proposed.start <= seg.event.proposed.start) continue
                    parts.add(lastEnd to overlap.event.proposed.start)
                    lastEnd = overlap.event.proposed.end
                }
                if(lastEnd < seg.event.proposed.end) {
                    parts.add(
                        lastEnd to seg.event.proposed.end
                    )
                }
                parts = parts.filter { it.second - it.first != 0L }.toMutableList()
                if(parts.isNotEmpty()){
                    modifyable.remove(seg)
                    modifyable.add(seg.copy(
                        hasContent = false,
                        isLeft = false,
                        isFullWidth = false
                    )) // Right no content full time event
                    val longest = parts.sortedByDescending { it.second - it.first }[0]  // Cant be null
                    parts.remove(longest)
                    modifyable.add(
                        seg.copy(isFullWidth = true,  isLeft = true, hasContent = true, segmentStart = longest.first, segmentEnd = longest.second)
                    )
                    for(part in parts){
                        modifyable.add(
                            seg.copy(isFullWidth = true,  isLeft = true, hasContent = false, segmentStart = part.first, segmentEnd = part.second)
                        )
                    }
                }
            }
            return modifyable
        }
    }
}