package com.myxoz.life.viewmodels

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.myxoz.life.api.Location
import com.myxoz.life.api.PersonSyncable
import com.myxoz.life.api.ProfilePictureSyncable
import com.myxoz.life.dbwrapper.EventEntity
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.diagrams.PieChart
import com.myxoz.life.events.additionals.EventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileInfoModel(): ViewModel(){
    val id  = MutableStateFlow<Long?>(null)
    val name = MutableStateFlow<String?>(null)
    val fullName = MutableStateFlow<String?>(null)
    val iban = MutableStateFlow<String?>(null)
    val home = MutableStateFlow<Location?>(null)
    val phone = MutableStateFlow<String?>(null)
    val birthday = MutableStateFlow<Long?>(null)
    private val _lastInteraction = MutableStateFlow<EventEntity?>(null)
    val lastInteraction = _lastInteraction.asStateFlow()
    private val _nextInteraction = MutableStateFlow<EventEntity?>(null)
    val nextInteraction = _nextInteraction.asStateFlow()
    val isEditing = MutableStateFlow(false)
    val isExtended = MutableStateFlow(false)
    val picture = MutableStateFlow<Bitmap?>(null)
    val chart = PieChart()
    val chartScale = MutableStateFlow(2)
    val chartUnit = MutableStateFlow(1)
    val platforms = MutableStateFlow(listOf<PersonSyncable.Companion.Socials>())
    val platformInputs = MutableStateFlow(listOf<String>())
    val isProfilePictureFullScreen = MutableStateFlow(false) /* This doesnt  belong here,  but this is my app so I dont care */
    suspend fun saveAndSync(db: StorageManager){
        val id = id.value?:throw Error("Trying to sync person with id null (before id was set)")
        val name = name.value?:throw Error("Trying to sync person without name (before name was set)")
        iban.value = iban.value?.takeIf { it.length > 4 }?.replace(" ", "")
        val newSocials = PersonSyncable.getOrderedSocials(platformInputs.value.mapNotNull {
            PersonSyncable.Companion.Socials.from(it)
        })
        fullName.value = fullName.value?.takeIf { it.isNotBlank() }
        phone.value = phone.value?.replace(" ", "")?.takeIf { it.isNotBlank() }
        PersonSyncable(
            id,
            name,
            fullName.value,
            phone.value,
            iban.value,
            home.value?.id,
            birthday.value,
            newSocials,
        ).saveAndSync(db)
        platforms.value = newSocials
    }
    suspend fun renderPieChart(db: StorageManager){
        val totals = mutableMapOf<Int, Long>()
        val id = id.value?:return
        val timeframe = chartScale.value
        val socialEvents = db.events.getEventsWithPerson(id).filter {
            it.start > (if(timeframe==0) 0L else System.currentTimeMillis() - 24*3600L*1000*(when (timeframe) {
                1 -> 356L
                2 -> 30L
                else -> 7L
            }))
        }
        for (social in socialEvents) {
            if(social.start > System.currentTimeMillis()) continue
            val sStart = social.start
            val sEnd = social.end
            val overlapping = db.events.getEventsOverlapping(sStart, sEnd)
            val coverage = mutableListOf<Pair<Long, Long>>()
            for (e in overlapping) {
                if (e.id == social.id) continue
                val oStart = maxOf(sStart, e.start)
                val oEnd = minOf(sEnd, e.end)
                if (oStart < oEnd) {
                    val duration = oEnd - oStart
                    totals[e.type] = (totals[e.type] ?: 0) + duration
                    coverage += oStart to oEnd
                }
            }
            val merged = mergeIntervals(coverage)
            // Not already grabed parts
            var cursor = sStart
            for ((cStart, cEnd) in merged) {
                if (cursor < cStart) {
                    val pureSocial = cStart - cursor
                    totals[social.type] = (totals[social.type] ?: 0) + pureSocial
                }
                cursor = maxOf(cursor, cEnd)
            }
            if (cursor < sEnd) {
                val pureSocial = sEnd - cursor
                totals[social.type] = (totals[social.type] ?: 0) + pureSocial
            }
        }
        val oldChart = chart
        val newChart = PieChart.build {
            totals.forEach {
                val type = EventType.getById(it.key) ?: EventType.Empty
                add(type.color, it.value.toDouble(), type.id.toString())
            }
        }
        oldChart.update(newChart)
    }

    private fun mergeIntervals(intervals: List<Pair<Long, Long>>): List<Pair<Long, Long>> {
        if (intervals.isEmpty()) return emptyList()
        val sorted = intervals.sortedBy { it.first }
        val out = mutableListOf<Pair<Long, Long>>()
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
    suspend fun setStateToDb(db: StorageManager) {
        val id = id.value ?: return
        val dbEntry = db.people.getPersonById(id) ?: return
        val location = dbEntry.home?.let { db.location.getLocationById(it) }?.let { Location.from(it) }
        val socials = db.socials.getSocialsFromPerson(id)
        platforms.value = socials.mapNotNull { PersonSyncable.Companion.Socials.from(it) }
        platformInputs.value = platforms.value.map { it.asString() }
        name.value = dbEntry.name
        fullName.value = dbEntry.fullname
        phone.value = dbEntry.phoneNumber
        birthday.value = dbEntry.birthday
        home.value = location
        iban.value = dbEntry.iban
    }
    fun updateStateIfOutdated(personId: Long, db: StorageManager, context: Context){
        // Changed due to caching reasons
        viewModelScope.launch {
            id.value = personId
            setStateToDb(db)
            val entry = db.profilePictureDao.getPPById(personId)
            if(entry!=null && entry.hasPP) picture.value = ProfilePictureSyncable.loadBitmapByPerson(context, personId) else picture.value = null
            val now = System.currentTimeMillis()
            _lastInteraction.value = db.people.getLastInteractionByPerson(personId, now)
            _nextInteraction.value = db.people.getNextPlanedEvent(personId, now)
            renderPieChart(db)
        }
    }
    suspend fun openPersonDetails(personId: Long, nav: NavController, db: StorageManager, context: Context){
        isEditing.value = false
        isExtended.value = false
        isProfilePictureFullScreen.value = false
        updateStateIfOutdated(personId, db, context)
        nav.navigate("display_person/${personId}")
    }
    companion object {
        fun formatTime(duration: Long): String{
            val future = duration > 0
            val prefix = if (future) "in " else "vor "
            return prefix + formatTimeDuration(duration)
        }
        fun formatTimeDuration(duration: Long): String { // AI
            val absMs = kotlin.math.abs(duration)
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
}
