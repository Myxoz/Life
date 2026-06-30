package com.myxoz.life.repositories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.api.extensions.FavoriteContactsExtendable
import com.myxoz.life.api.syncables.ExtensionSyncable
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.utils.setter
import com.myxoz.life.storage.interfaces.utils.subscribeToColdFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class ContactsRepo(private val repos: DatabaseInterface): ViewModel() {
    val lifeContacts = repos.peopleInterface.getAllPeople().subscribeToColdFlow(viewModelScope, listOf())
    val selectMode = MutableStateFlow(false)
    val allSortedDeviceContacts = repos.deviceContactRepo.allSortedContacts
    /** Call only from Dispatcher.IO else massive lag spikes, fetches all device contacts and udpates old ones */
    fun requestRefetchDeviceContacts() = repos.deviceContactRepo.refetchDeviceContacts()
    var showIcons = MutableStateFlow(false)
    suspend fun createNewContact(syncable: PersonSyncable) = repos.peopleInterface.updateAndStageSync(syncable)

    val favoritePeople = repos.extensionInterface
        .flowFor(ExtensionSyncable.ExtensionSyncableType.FavoriteContacts)
        .map { it?.ids ?: listOf() }
        .subscribeToColdFlow(viewModelScope, listOf())
        .setter(
            viewModelScope
        ) {
            repos.extensionInterface.saveAndSync(
                ExtensionSyncable.ExtensionSyncableType.FavoriteContacts,
                FavoriteContactsExtendable(it)
            )
        }
}