package com.myxoz.life.android.contacts

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.api.syncables.PersonSyncable.Companion as Person


object AndroidContacts {
    fun getAllContacts(context: Context): List<PersonSyncable> {
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
            val contactIdIndex =
                it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)

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
                id = -1,
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
    fun contactExists(context: Context, phoneNumber: String): Boolean {
        if (phoneNumber.isEmpty()) { return false }

        val resolver = context.contentResolver

        val uri = Uri.withAppendedPath (
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
        Uri.encode(phoneNumber)
        )

        val projection = arrayOf(ContactsContract.PhoneLookup._ID)

        val cursor = resolver.query(uri, projection, null, null, null)
        return cursor != null && cursor.moveToFirst().also { cursor.close() }
    }
    fun openSaveContactIntent(
        context: Context,
        phoneNumber: String?,
        givenName: String?,
    ) {
        val intent = Intent(ContactsContract.Intents.Insert.ACTION)
        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE)

        intent.putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber)

        if (givenName != null) {
            intent.putExtra(ContactsContract.Intents.Insert.NAME, givenName)
        }

        context.startActivity(intent)
    }
}