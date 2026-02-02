package com.myxoz.life.repositories

import android.content.Context
import com.myxoz.life.android.contacts.AndroidContacts
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.repositories.utils.Versioned
import com.myxoz.life.repositories.utils.VersionedCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactRepo(
    private val context: Context,
    private val appScope: CoroutineScope
) {
    private val _savedInContacts = VersionedCache<String, Boolean>(
        {
            false
        }
    )
    private val _allContacts = MutableStateFlow<List<PersonSyncable>>(listOf())
    val allContacts: StateFlow<List<PersonSyncable>> = _allContacts

    suspend fun updateIsSavedInContacts(phone: String): Boolean {
        val isInContacts = withContext(Dispatchers.IO) {
            AndroidContacts.contactExists(context, phone)
        }
        if(isInContacts != _savedInContacts.getCached(phone)?.data) {
            _savedInContacts.overwrite(phone, isInContacts)
        }
        return isInContacts
    }
    fun isSavedInContacts(phoneNumber: String): Flow<Versioned<Boolean>?> {
        appScope.launch {
            withContext(Dispatchers.IO) {
                updateIsSavedInContacts(phoneNumber)
            }
        }
        return _savedInContacts.flowByKey(appScope, phoneNumber)
    }
    suspend fun isSavedInContactsNOW(phone: String) = updateIsSavedInContacts(phone)
    init {
        refetchDeviceContacts()
    }
    fun refetchDeviceContacts(){
        appScope.launch {
            withContext(Dispatchers.IO){
                val savedContacts = AndroidContacts.getAllContacts(context)
                _allContacts.value = savedContacts
                _savedInContacts.overwriteAll(
                    savedContacts.mapNotNull { (it.phoneNumber ?: return@mapNotNull null) to true }
                )
            }
        }
    }
}