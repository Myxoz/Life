package com.myxoz.life.viewmodels

import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.repositories.utils.subscribeToColdFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.launch

class ContactsViewModel(private val repos: AppRepositories): ViewModel() {
    val favoriteIds = MutableStateFlow(
        (repos.prefs.getStringSet("favorite_people", setOf())  ?: setOf()).mapNotNull { it.toLongOrNull() }
    )
    fun getAllDeviceContacts() = repos.contactRepo.allContacts
    /** Call only from Dispatcher.IO else massive lag spikes, fetches all device contacts and udpates old ones, if size changes */
    fun requestRefetchDeviceContacts() = repos.contactRepo.refetchDeviceContacts()
    val getAllLifeContacts = repos.peopleRepo.getAllPeople().subscribeToColdFlow(viewModelScope, listOf())
    var showIcons = MutableStateFlow(false)
    var scrollDistance = 0
    suspend fun createNewContact(syncable: PersonSyncable) = repos.peopleRepo.updateAndStageSync(syncable)
    init {
        viewModelScope.launch {
            favoriteIds.collect {
                repos.prefs.edit{
                    putStringSet("favorite_people", favoriteIds.map { it.toString() }.toSet())
                }
            }
        }
    }
}