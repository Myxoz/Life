package com.myxoz.life.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import com.myxoz.life.android.contacts.AndroidContacts
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.dbwrapper.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow

class ContactsViewModel(val db: StorageManager): ViewModel() {
    val deviceContacts = MutableStateFlow(listOf<PersonSyncable>())
    var lifeContacts = MutableStateFlow(listOf<PersonSyncable>())
    var showIcons = MutableStateFlow(false)
    var scrollDistance = 0
    /**
    * Call only from Dispatcher.IO else massive lag spikes, fetches all device contacts and udpates old ones, if size changes
     */
    suspend fun fetchDeviceContacts(context: Context){
        val newContacts = AndroidContacts.getAllContacts(context)
        refetchLifeContacts()
        if(newContacts.size != deviceContacts.value.size) deviceContacts.value = newContacts.filter {
            lifeContacts.value.none { lc ->
                it.phoneNumber?.trim()?.replace("\\D".toRegex(), "") == lc.phoneNumber?.trim()?.replace("\\D".toRegex(), "")
            }
        }
    /* Update if size changes, update if problematic */
    }
    suspend fun refetchLifeContacts(){
        val newContacts = db.people.getAllPeople().map { PersonSyncable.from(db, it) }
        if(newContacts.asComparableString(db).hashCode() != lifeContacts.value.asComparableString(db).hashCode()) lifeContacts.value = newContacts
    }
    private suspend fun List<PersonSyncable>.asComparableString(db: StorageManager) = this.map { it.toJson(db).toString() }.joinToString(";")
}