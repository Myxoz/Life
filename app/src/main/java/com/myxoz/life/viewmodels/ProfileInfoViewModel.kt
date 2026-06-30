package com.myxoz.life.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.repositories.supermodule.CrossRepoSuper
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.utils.StateFlowCache
import com.myxoz.life.storage.interfaces.utils.subscribeToColdFlow
import com.myxoz.life.ui.NavPath
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileInfoModel(
    val handle: SavedStateHandle,
    val dbInterface: DatabaseInterface,
    val crossRepoSuper: CrossRepoSuper
): ViewModel(){
    val peopleAggregator = dbInterface.aggregators.peopleAggregator
    val inspectedPersonId = handle.get<Long>(NavPath.Menu.Contacts.DISPLAY_PERSON.parameterName) ?: error("Cannot parse person id in route")
    val inspectedPerson = crossRepoSuper.getPerson(inspectedPersonId)
        .subscribeToColdFlow(viewModelScope, null)

    suspend fun getPersonOrAwait(): PersonSyncable{
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

    val debt = dbInterface.bankingRepo
        .getDebtFor(inspectedPersonId)
        .subscribeToColdFlow(viewModelScope, null)
    val editingPerson = MutableStateFlow<PersonSyncable?>(null)
    fun edit(editWith: (PersonSyncable)->PersonSyncable) {
        _isEditing.value = true
        viewModelScope.launch {
            val cached = editingPerson.value ?: getPersonOrAwait()
            val new = editWith(cached)
            platformInputs.value = new.socials.map { it.asString() }
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
            socials = PersonSyncable.getOrderedSocials(platformInputs.value.mapNotNull {
                PersonSyncable.Companion.Socials.from(it)
            }),
            fullName = asEdited.fullName?.takeIf { it.isNotBlank() },
            phoneNumber = asEdited.phoneNumber?.replace(" ", "")?.takeIf { it.isNotBlank() }
        )
        dbInterface.peopleInterface.updateAndStageSync(editedPerson)
        editingPerson.value = null
        _isEditing.value = false
    }
    private val savedInContactsCache = StateFlowCache<String, Boolean?>{
        dbInterface.deviceContactRepo.isSavedInContacts(it).subscribeToColdFlow(viewModelScope, null)
    }
    fun getSavedInContacts(phoneNumber: String) = savedInContactsCache.get(phoneNumber)
    suspend fun getSavedInContactsNOW(phoneNumber: String) = dbInterface.deviceContactRepo.isSavedInContactsNOW(phoneNumber)
    suspend fun updateProfilePicture(base64: String?) = dbInterface.peopleInterface.updatePP(inspectedPersonId, base64)
    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing
    val isExtended = MutableStateFlow(false)
    val chartScale = MutableStateFlow(2)
    val chartUnit = MutableStateFlow(1)
    val platformInputs = MutableStateFlow(listOf<String>())
    val isProfilePictureFullScreen = MutableStateFlow(false) /* This doesnt belong here, but this is my app so I dont care */

    val pieChart = combine(
            dbInterface.calendarInterface.interactedWithPerson(inspectedPersonId),
            chartScale
        ) { _, scale ->
            dbInterface.aggregators.peopleAggregator.getPieChartFor(inspectedPersonId, scale)
        }.subscribeToColdFlow(viewModelScope, null)

    val profilePic = dbInterface.aggregators.peopleAggregator.getProfilePicture(inspectedPersonId).subscribeToColdFlow(viewModelScope, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val inspectedPersonIncludingEdits = isEditing.flatMapMerge { if(it) editingPerson else inspectedPerson }.subscribeToColdFlow(viewModelScope, null)

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
