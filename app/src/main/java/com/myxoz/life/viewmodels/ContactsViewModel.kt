package com.myxoz.life.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.api.extensions.FavoriteContactsExtendable
import com.myxoz.life.api.syncables.ExtensionSyncable
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.repositories.utils.setter
import com.myxoz.life.repositories.utils.subscribeToColdFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class ContactsViewModel(private val repos: AppRepositories): ViewModel() {
    val selectMode = MutableStateFlow(false)
    val allSortedDeviceContacts = repos.contactRepo.allSortedContacts
    /** Call only from Dispatcher.IO else massive lag spikes, fetches all device contacts and udpates old ones */
    fun requestRefetchDeviceContacts() = repos.contactRepo.refetchDeviceContacts()
    val lifeContacts = repos.peopleRepo.getAllPeople().map { people -> people.sortedBy { it.name } }.subscribeToColdFlow(viewModelScope, listOf())
    var showIcons = MutableStateFlow(false)
    suspend fun createNewContact(syncable: PersonSyncable) = repos.peopleRepo.updateAndStageSync(syncable)

    val favoritePeople = repos.extensionRepo
        .flowFor(ExtensionSyncable.ExtensionSyncableType.FavoriteContacts)
        .map { it?.ids ?: listOf() }
        .subscribeToColdFlow(viewModelScope, listOf())
        .setter(
            viewModelScope
        ) {
            repos.extensionRepo.saveAndSync(
                ExtensionSyncable.ExtensionSyncableType.FavoriteContacts,
                FavoriteContactsExtendable(it)
            )
        }
}