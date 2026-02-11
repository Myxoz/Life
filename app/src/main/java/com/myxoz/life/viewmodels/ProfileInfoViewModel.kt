package com.myxoz.life.viewmodels

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.myxoz.life.api.syncables.LocationSyncable
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.repositories.utils.StateFlowCache
import com.myxoz.life.repositories.utils.VersionedCache
import com.myxoz.life.repositories.utils.subscribeToColdFlow
import com.myxoz.life.utils.diagrams.PieChart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ProfileInfoModel(val repos: AppRepositories): ViewModel(){
    private val lastInteractionFlowCache = StateFlowCache<Long, SyncedEvent?> {
        repos.aggregators.peopleAggregator.getLastInteraction(it).subscribeToColdFlow(viewModelScope, null)
    }
    fun lastInteractionFlow(personId: Long) = lastInteractionFlowCache.get(personId)

    private val nextInteractionFlowCache = StateFlowCache<Long, SyncedEvent?> {
        repos.aggregators.peopleAggregator.getNextInteraction(it).subscribeToColdFlow(viewModelScope, null)
    }
    fun nextInteractionFlow(personId: Long) = nextInteractionFlowCache.get(personId)

    private val getPersonFlowCache = StateFlowCache<Long, PersonSyncable?>{
        repos.peopleRepo.getPerson(it).map { it?.data }.subscribeToColdFlow(viewModelScope, null)
    }
    fun getPerson(personId: Long) = getPersonFlowCache.get(personId)

    private val getPeopleFlowCache = StateFlowCache<List<Long>, List<PersonSyncable>>{
        repos.peopleRepo.getPeople(it).map { l -> l.mapNotNull { it?.data } }.subscribeToColdFlow(viewModelScope, listOf())
    }
    fun getPeople(personIds: List<Long>) = getPeopleFlowCache.get(personIds)

    val getAlPeopleFlow = repos.peopleRepo.getAllPeople().subscribeToColdFlow(viewModelScope, listOf())

    private val getLocationByIdFLowCache = StateFlowCache<Long?, LocationSyncable?>{
        if(it == null || it == 0L) return@StateFlowCache MutableStateFlow(null)
        repos.locationRepo.getLocationById(it).map { it?.data }.subscribeToColdFlow(viewModelScope, null)
    }
    fun getLocationById(locationId: Long?) = getLocationByIdFLowCache.get(locationId)

    fun getCachedLocation(locationId: Long?) = (if(locationId != null) repos.locationRepo.getCachedLocation(locationId) else null)
    private val _editingPerson = VersionedCache<Long, PersonSyncable>(
        {
            repos.peopleRepo.getCurrentPersonNotAsFlow(it).data
        }
    )
    private val editingPersonFlowCache = StateFlowCache<Long, PersonSyncable?>{
        _editingPerson.flowByKey(viewModelScope, it).map{ it?.data }.subscribeToColdFlow(viewModelScope, null)
    }
    fun getEditingPerson(personId: Long) = editingPersonFlowCache.get(personId)
    fun edit(personId: Long, editWith: (PersonSyncable)->PersonSyncable) {
        _isEditing.value = true

        viewModelScope.launch {
            val cached = _editingPerson.get(personId).data
            _editingPerson.overwrite(personId, editWith(cached))
        }
    }
    suspend fun discardChanges(personId: Long){
        _isEditing.value = false
        _editingPerson.overwrite(personId, repos.peopleRepo.getCurrentPersonNotAsFlow(personId).data)
    }
    suspend fun saveAndStageChanges(personId: Long) {
        val asEdited = _editingPerson.get(personId).data
        val editedPerson = asEdited.copy(
            iban = asEdited.iban?.takeIf { it.length > 4 }?.replace(" ", ""),
            socials = PersonSyncable.getOrderedSocials(platformInputs.value.mapNotNull {
                PersonSyncable.Companion.Socials.from(it)
            }),
            fullName = asEdited.fullName?.takeIf { it.isNotBlank() },
            phoneNumber = asEdited.phoneNumber?.replace(" ", "")?.takeIf { it.isNotBlank() }
        )
        repos.peopleRepo.updateAndStageSync(editedPerson)
        _isEditing.value = false
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    private val inspectedPersonCache = StateFlowCache<Long, PersonSyncable?>{
        isEditing.flatMapLatest { editing ->
            if(editing) getEditingPerson(it) else getPerson(it)
        }.subscribeToColdFlow(viewModelScope, null)
    }
    fun getInspectedPerson(personId: Long) = inspectedPersonCache.get(personId)
    private val savedInContactsCache = StateFlowCache<String, Boolean?>{
        repos.contactRepo.isSavedInContacts(it).map { it?.data }.subscribeToColdFlow(viewModelScope, null)
    }
    fun getSavedInContacts(phoneNumber: String) = savedInContactsCache.get(phoneNumber)
    suspend fun getSavedInContactsNOW(phoneNumber: String) = repos.contactRepo.isSavedInContactsNOW(phoneNumber)
    suspend fun updateProfilePicture(personId: Long, base64: String?) = repos.peopleRepo.updatePP(personId, base64)
    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing
    val isExtended = MutableStateFlow(false)
    val chartScale = MutableStateFlow(2)
    val chartUnit = MutableStateFlow(1)
    val platforms = MutableStateFlow(listOf<PersonSyncable.Companion.Socials>())
    val platformInputs = MutableStateFlow(listOf<String>())
    val isProfilePictureFullScreen = MutableStateFlow(false) /* This doesnt belong here, but this is my app so I dont care */
    private val piechartFlowCache = StateFlowCache<Long, Map<String, PieChart.Companion.PieChartPart>?>{ person ->
        combine(
            repos.calendarRepo.interactedWithPerson(person),
            chartScale
        ) { _, scale ->
            repos.aggregators.peopleAggregator.getPieChartFor(person, scale)
        }.subscribeToColdFlow(viewModelScope, null)
    }
    fun getPieChartForPerson(personId: Long) = piechartFlowCache.get(personId)
    fun openPersonDetails(personId: Long, nav: NavController, context: Context){
        isExtended.value = false
        isProfilePictureFullScreen.value = false
        nav.navigate("display_person/${personId}")
    }

    private val profilePictureCache = StateFlowCache<Long, Bitmap?>{ personId ->
        repos.aggregators.peopleAggregator.getProfilePicture(personId).subscribeToColdFlow(viewModelScope, null)
    }
    fun getProfilePicture(personId: Long) = profilePictureCache.get(personId)

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
