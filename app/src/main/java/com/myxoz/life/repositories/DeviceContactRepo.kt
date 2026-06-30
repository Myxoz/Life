package com.myxoz.life.repositories

import android.content.Context
import com.myxoz.life.android.contacts.AndroidContacts
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.storage.interfaces.utils.PerformantCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeviceContactRepo(
    private val context: Context,
    private val appScope: CoroutineScope
) {
    private val _savedInContacts = PerformantCache<String, Boolean>(appScope) { false }
    private val _allSortedContacts = MutableStateFlow<List<PersonSyncable>>(listOf())
    val allSortedContacts: StateFlow<List<PersonSyncable>> = _allSortedContacts

    suspend fun updateIsSavedInContacts(phone: String): Boolean {
        val isInContacts = withContext(Dispatchers.IO) {
            AndroidContacts.contactExists(context, phone)
        }
        if(isInContacts != _savedInContacts.getCached(phone)) {
            _savedInContacts.overwrite(phone, isInContacts)
        }
        return isInContacts
    }
    fun isSavedInContacts(phoneNumber: String): Flow<Boolean?> {
        appScope.launch {
            withContext(Dispatchers.IO) {
                updateIsSavedInContacts(phoneNumber)
            }
        }
        return _savedInContacts.flowByKey(phoneNumber)
    }
    suspend fun isSavedInContactsNOW(phone: String) = updateIsSavedInContacts(phone)
    fun refetchDeviceContacts(){
        appScope.launch {
            withContext(Dispatchers.IO) {
                val savedContacts = AndroidContacts.getAllContacts(context)
                _allSortedContacts.value = savedContacts.sortedBy { it.name }
                _savedInContacts.overwriteAll(
                    savedContacts.mapNotNull { (it.phoneNumber ?: return@mapNotNull null) to true }
                )
            }
        }
    }
}