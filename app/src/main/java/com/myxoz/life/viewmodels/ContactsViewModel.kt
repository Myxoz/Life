package com.myxoz.life.viewmodels

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.dbwrapper.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import com.myxoz.life.api.syncables.PersonSyncable.Companion as Person

class ContactsViewModel(val db: StorageManager): ViewModel() {
    val deviceContacts = MutableStateFlow(listOf<PersonSyncable>())
    var lifeContacts = MutableStateFlow(listOf<PersonSyncable>())
    var showIcons = MutableStateFlow(false)
    var scrollDistance = 0
    /**
    * Call only from Dispatcher.IO else massive lag spikes, fetches all device contacts and udpates old ones, if size changes
     */
    suspend fun fetchDeviceContacts(context: Context){
        val newContacts = getAllContacts(context)
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
    private fun getAllContacts(context: Context): List<PersonSyncable> {
        val contacts = mutableListOf<Pair<String, String>>()
        val resolver = context.contentResolver
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
            ),
            null, null, null
        )

        val contactIdMap = mutableMapOf<String, Long>()
        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val contactIdIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex)
                val number = it.getString(numberIndex)
                val contactId = it.getLong(contactIdIndex)
                contacts.add(name to number)
                contactIdMap[number.replace(" ", "")] = contactId
            }
        }

        val knownNumbers = HashSet<String>()
        val uniqueContacts = contacts.filter {
            val normalizedNumber = it.second.replace(" ", "")
            if (knownNumbers.contains(normalizedNumber)) {
                false
            } else {
                knownNumbers.add(normalizedNumber)
                true
            }
        }

        return uniqueContacts.map { contact ->
            val normalizedNumber = contact.second.replace(" ", "")
            val contactId = contactIdMap[normalizedNumber] ?: 0L
            val hasWhatsApp = checkIfContactHasWhatsApp(resolver, contactId)

            val socials = if (hasWhatsApp) {
                listOf(Person.Socials(Person.Platform.WhatsApp, "WhatsApp"))
            } else {
                emptyList()
            }

            PersonSyncable(
                id = 0,
                name = contact.first,
                fullName = null,
                phoneNumber = contact.second,
                iban = null,
                home = null,
                birthday = null,
                socials = socials
            )
        }
    }

    private fun checkIfContactHasWhatsApp(resolver: ContentResolver, contactId: Long): Boolean {
        val cursor = resolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            ContactsContract.Data.CONTACT_ID + " = ? AND " +
                    ContactsContract.Data.MIMETYPE + " = ?",
            arrayOf(contactId.toString(), "vnd.android.cursor.item/vnd.com.whatsapp.profile"),
            null
        )

        val hasWhatsApp = cursor?.use { it.count > 0 } ?: false
        return hasWhatsApp
    }
}