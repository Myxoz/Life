package com.myxoz.life.state.screenscoped

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.lifecore.flows.StateFlowCache
import com.myxoz.life.core.minus
import com.myxoz.life.lifecore.NavPath
import com.myxoz.life.lifecore.android.image.ImagePostProcessor.PostProcessingOption.*
import com.myxoz.life.lifecore.android.image.ImagePostProcessor.process
import com.myxoz.life.presentation.SocialEventPlatformFormatting
import com.myxoz.life.presentation.events.FormattableEvent
import com.myxoz.life.presentation.events.FormattableEvent.Companion.formattable
import com.myxoz.life.state.activityscoped.DeviceContactRepo
import com.myxoz.life.state.activityscoped.supermodule.CrossRepoSuper
import com.myxoz.life.storage.api.syncables.PersonSyncable
import com.myxoz.life.storage.interfaces.BankingInterface
import com.myxoz.life.storage.interfaces.PeopleInterface
import com.myxoz.life.storage.interfaces.aggregator.PeopleEventAggregator
import com.myxoz.life.subscribeToColdFlow
import com.myxoz.life.utils.diagrams.PieChart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class ProfileInfoModel(
    val handle: SavedStateHandle,
    val peopleAggregator: PeopleEventAggregator,
    val deviceContactRepo: DeviceContactRepo,
    val peopleInterface: PeopleInterface,
    val bankingInterface: BankingInterface,
    val crossRepoSuper: CrossRepoSuper
): ViewModel() {
    val inspectedPersonId = handle.get<Long>(NavPath.Menu.Contacts.DISPLAY_PERSON.parameterName)
        ?: error("Cannot parse person id in route")
    val inspectedPerson = crossRepoSuper.getPerson(inspectedPersonId)
        .subscribeToColdFlow(viewModelScope, null)

    suspend fun getPersonOrAwait(): PersonSyncable {
        val person = inspectedPerson.value
        if(person != null) return person
        return inspectedPerson.filterNotNull().first()
    }
    val lastInteraction = peopleAggregator
        .getLastInteraction(inspectedPersonId)
        .subscribeToColdFlow(viewModelScope, null)

    val nextInteraction = peopleAggregator
        .getNextInteraction(inspectedPersonId)
        .subscribeToColdFlow(viewModelScope, null)

    val debt = bankingInterface
        .getDebtFor(inspectedPersonId)
        .subscribeToColdFlow(viewModelScope, null)
    val editingPerson = MutableStateFlow<PersonSyncable?>(null)
    fun edit(editWith: (PersonSyncable)->PersonSyncable) {
        _isEditing.value = true
        viewModelScope.launch {
            val cached = editingPerson.value ?: getPersonOrAwait()
            val new = editWith(cached)
            platformInputs.value = new.socials.map { "${it.handle}@${it.platform}" }
            editingPerson.update { new }
        }
    }
    suspend fun discardChanges(){
        _isEditing.value = false
        editingPerson.value = getPersonOrAwait()
    }
    suspend fun saveAndStageChanges() {
        val asEdited = editingPerson.value ?: return
        val editedPerson = asEdited.copy(
            iban = asEdited.iban?.takeIf { it.length > 4 }?.replace(" ", ""),
            socials = platformInputs.value.mapNotNull {
                SocialEventPlatformFormatting.parsePersonSyncablePlatformByDisplay(it)
            }.toSet(),
            fullName = asEdited.fullName?.takeIf { it.isNotBlank() },
            phoneNumber = asEdited.phoneNumber?.replace(" ", "")?.takeIf { it.isNotBlank() }
        )
        peopleInterface.updateAndStageSync(editedPerson)
        editingPerson.value = null
        _isEditing.value = false
    }
    private val savedInContactsCache = StateFlowCache<String, Boolean?>{
        deviceContactRepo.isSavedInContacts(it).subscribeToColdFlow(viewModelScope, null)
    }

    private fun mergeIntervals(intervals: List<Pair<Instant, Instant>>): List<Pair<Instant, Instant>> {
        if (intervals.isEmpty()) return emptyList()
        val sorted = intervals.sortedBy { it.first }
        val out = mutableListOf<Pair<Instant, Instant>>()
        var current = sorted[0]

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.first <= current.second) {
                // overlap, extend
                current = current.first to maxOf(current.second, next.second)
            } else {
                out += current
                current = next
            }
        }
        out += current
        return out
    }
    suspend fun getPieChartFor(id: Long, chartScale: ProfileInfoChartScale): Map<String, PieChart.Companion.PieChartPart> {
        val totals = mutableMapOf<FormattableEvent, Duration>()
        val (socialEvents, overlappingEvents) = peopleAggregator.getEventsWithPerson(id, chartScale.durationConsiderInfinite())
        for (social in socialEvents) {
            val sStart = social.raw.eventTime.start
            val sEnd = social.raw.eventTime.end
            val time = social.raw.eventTime
            val overlapping = overlappingEvents.filter { it.raw.eventTime.overlaps(time) }
            val coverage = mutableListOf<Pair<Instant, Instant>>()
            for (e in overlapping) {
                if (e.id == social.id) continue
                val eTime = e.raw.eventTime
                val oStart = maxOf(sStart, eTime.start)
                val oEnd = minOf(sEnd, eTime.end)
                if (oStart < oEnd) {
                    val duration = oEnd - oStart
                    totals.merge(e.raw.formattable(), duration) { old, new ->
                        old + new
                    }
                    coverage += oStart to oEnd
                }
            }
            val merged = mergeIntervals(coverage)
            // Not already grabed parts
            var cursor = sStart
            for ((cStart, cEnd) in merged) {
                if (cursor < cStart) {
                    val pureSocial = cStart - cursor
                    totals.merge(social.raw.formattable(), pureSocial) { old, new -> old + new}
                }
                cursor = maxOf(cursor, cEnd)
            }
            if (cursor < sEnd) {
                val pureSocial = sEnd - cursor
                totals.merge(social.raw.formattable(), pureSocial) { old, new -> old + new}
            }
        }
        val newChart = PieChart.build {
            totals.forEach {
                add(it.key.bg, it.value.inWholeMilliseconds.toDouble(), it.key.displayName)
            }
        }
        return newChart
    }
    fun getSavedInContacts(phoneNumber: String) = savedInContactsCache.get(phoneNumber)
    suspend fun getSavedInContactsNOW(phoneNumber: String) = deviceContactRepo.isSavedInContactsNOW(phoneNumber)
    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing
    val isExtended = MutableStateFlow(false)
    val chartScale = MutableStateFlow(ProfileInfoChartScale.MONTH)
    val chartUnit = MutableStateFlow(1)
    val platformInputs = MutableStateFlow(listOf<String>())
    val isProfilePictureFullScreen = MutableStateFlow(false) /* This doesnt belong here, but this is my app so I dont care */

    val pieChart = combine(
            peopleAggregator.getLastInteraction(inspectedPersonId),
            chartScale
        ) { _, scale ->
            getPieChartFor(inspectedPersonId, scale)
        }.subscribeToColdFlow(viewModelScope, null)

    val profilePic = peopleInterface.getProfilePicture(inspectedPersonId).subscribeToColdFlow(viewModelScope, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val inspectedPersonIncludingEdits = isEditing.flatMapMerge { if(it) editingPerson else inspectedPerson }.subscribeToColdFlow(viewModelScope, null)

    companion object {
        fun formatTime(duration: Long): String{
            val future = duration > 0
            val prefix = if (future) "in " else "vor "
            return prefix + formatTimeDuration(duration)
        }
        fun formatTimeDuration(duration: Long): String { // AI
            val absMs = abs(duration)
            val totalSeconds = absMs / 1000


            // Helper for singular/plural seconds and minutes
            fun secStr(s: Long): String =
                when (s) {
                    1L -> "einer Sekunde"
                    else -> "$s Sekunden"
                }

            fun minStr(m: Long): String =
                when (m) {
                    1L -> "einer Minute"
                    else -> "$m Minuten"
                }

            fun hourStr(h: Long): String =
                when (h) {
                    1L -> "einer Stunde"
                    else -> "$h Stunden"
                }

            // 1. Seconds
            if (totalSeconds < 60) {
                return secStr(totalSeconds)
            }

            // 2. Minutes
            val minutes = totalSeconds / 60
            if (minutes < 60) {
                return minStr(minutes)
            }

            // 3. Hours
            val hours = minutes / 60
            if (hours < 24) {
                return hourStr(hours)
            }

            // 4. Days
            val days = hours / 24
            if (days <= 7) {
                val remHours = hours % 24
                return if (days == 1L) {
                    if (remHours == 0L) "1d"
                    else "1d ${remHours}h"
                } else {
                    if (remHours == 0L) "${days}d"
                    else "${days}d ${remHours}h"
                }
            }

            // 5. Weeks (up to 4), remainder days
            val weeks = days / 7
            if (weeks <= 4) {
                val remDays = days % 7
                return when {
                    remDays == 0L -> "${weeks}w"
                    else -> "${weeks}w ${remDays}d"
                }
            }

            // 6. Months + weeks (month = 30 days)
            val months = days / 30
            if (months < 12) {
                val remDays = days % 30
                val remWeeks = remDays / 7
                return when {
                    remWeeks == 0L -> "${months}mo"
                    else -> "${months}mo ${remWeeks}w"
                }
            }

            // 7. Years + months (year = 12 months)
            val years = months / 12
            val remMonths = months % 12

            return when {
                remMonths == 0L -> "${years}y"
                else -> "${years}Y ${remMonths}mo"
            }
        }
    }
    enum class ProfileInfoChartScale(val displayName: String, val duration: Duration) {
        ALL("All", Duration.INFINITE),
        YEAR("365d", 365.days),
        MONTH("30d",30.days),
        WEEK("1w",7.days);
        fun durationConsiderInfinite() = if(this.duration != Duration.INFINITE) duration else null
        companion object {
            private val ascendingEntries = entries.sortedBy(ProfileInfoChartScale::duration)
            fun minIncluding(duration: Duration): ProfileInfoChartScale =
                ascendingEntries
                    .firstOrNull { it.duration >= duration }
                    ?: ALL
        }
    }

    val imageCompressionPipeline = listOf(CropToSquare, Scale(480), Compress())
    suspend fun setProfilePicture(profilePictureRaw: Bitmap) {
        val processedBitmap = imageCompressionPipeline.process(profilePictureRaw)
        peopleInterface.updatePP(inspectedPersonId, processedBitmap)
    }
}
