package com.myxoz.life.storage.convertion

import com.myxoz.life.storage.api.LifeJSONArray
import com.myxoz.life.storage.api.LifeJSONObject
import com.myxoz.life.storage.api.helper.CommitModification
import com.myxoz.life.storage.api.syncables.BankingSidecarSyncable
import com.myxoz.life.storage.api.syncables.DeleteEntry
import com.myxoz.life.storage.api.syncables.ExtensionSyncable
import com.myxoz.life.storage.api.syncables.FullDaySyncable
import com.myxoz.life.storage.api.syncables.LocationSyncable
import com.myxoz.life.storage.api.syncables.ManualTransactionSyncable
import com.myxoz.life.storage.api.syncables.PersonSyncable
import com.myxoz.life.storage.api.syncables.ProfilePictureSyncable
import com.myxoz.life.storage.api.syncables.Syncable
import com.myxoz.life.storage.api.syncables.SyncableIds.getCalendarId
import com.myxoz.life.storage.api.syncables.SyncedEvent
import com.myxoz.life.storage.api.syncables.TodoSyncable
import com.myxoz.life.storage.api.syncables.TransactionSplitSyncable
import com.myxoz.life.storage.api.syncables.additionals.TransactionSplitPart
import com.myxoz.life.storage.convertion.events.EventsToJSON.eventToJSON


internal object SyncableToJSON{
    fun toJSON(syncable: Syncable): LifeJSONObject{
        val json: LifeJSONObject = LifeJSONObject()
            .put("type", syncable.getCalendarId())
            .put("id", syncable.id)

        return with(syncable) {
            when(this) {
                is BankingSidecarSyncable -> toJSON(json)
                is DeleteEntry -> toJSON(json)
                is ExtensionSyncable -> toJSON(json)
                is FullDaySyncable -> toJSON(json)
                is LocationSyncable -> toJSON(json)
                is ManualTransactionSyncable -> toJSON(json)
                is PersonSyncable -> toJSON(json)
                is ProfilePictureSyncable -> toJSON(json)
                is SyncedEvent -> raw.eventToJSON(json)
                is TodoSyncable -> toJSON(json)
                is TransactionSplitSyncable -> toJSON(json)
            }
        }
    }
    fun CommitModification.toJSON(): LifeJSONObject = LifeJSONObject()
        .put("filename", filename)
        .put("additions", additions)
        .put("deletions", deletions)
        .put("changes", changes)
        .put("status", status)

    private fun ManualTransactionSyncable.toJSON(json: LifeJSONObject): LifeJSONObject = json
        .put("digital", digital)
        .put("cashless", cashless)
        .put("amount", amountCents)
        .put("name", name)
        .put("timestamp", timestamp.toString())
        .put("purpose", purpose)

    private fun FullDaySyncable.toJSON(json: LifeJSONObject): LifeJSONObject = json
            .put("stress", stress)
            .put("successfulness", successfulness)
            .put("happyness", happyness)
            .put("screen_time_ms", screenTimeMs)
            .put("steps", steps)
            .put("top", LifeJSONArray.from(topApps) {
                put("duration", it.duration)
                put("packagename", it.packagename)
            })

    private fun TransactionSplitSyncable.toJSON(json: LifeJSONObject): LifeJSONObject = json
        .put("syncable_id", syncableId)
        .put("remote_id", remoteId)
        .put("parts", LifeJSONArray.from(parts.map{ it.toJSON(json) }))

    private fun DeleteEntry.toJSON(json: LifeJSONObject): LifeJSONObject = json
        .put("tags", 1)

    private fun PersonSyncable.toJSON(json: LifeJSONObject) = json
        .put("name", name)
        .put("fullname", fullName)
        .put("phone", phoneNumber)
        .put("iban", iban)
        .put("home", home)
        .put("birthday", birthday)
        .put("socials", LifeJSONArray.from(socials) {
            put("platform", it.platform.id)
            put("handle", it.handle)
        })

    private fun LocationSyncable.toJSON(json: LifeJSONObject) = json
        .put("name", name)
        .put("longitude", longitude)
        .put("lat", lat)
        .put("radiusm", radiusM)
        .put("ssid", ssid)
        .put("street", street)
        .put("number", number)
        .put("city", city)
        .put("country", country)

    private fun ProfilePictureSyncable.toJSON(json: LifeJSONObject): LifeJSONObject {
        if (bitmapBase64 == null) return json.put("has_pp", false)

        return json
            .put("has_pp", true)
            .put("content", bitmapBase64)
    }

    private fun ExtensionSyncable.toJSON(json: LifeJSONObject) = json
        // Yes this doesn't put JSON but a JSON string, as it's supposed to
        .put("data", content.specificsToString())

    private fun BankingSidecarSyncable.toJSON(json: LifeJSONObject) = json
        .put("transactionId", transactionId)
        .put("name", name)
        .put("date", date)

    private fun TransactionSplitPart.toJSON(json: LifeJSONObject) = json
        .put("person", person)
        .put("amount", amount)

    private fun TodoSyncable.toJSON(json: LifeJSONObject) = json
        .put("short", short)
        .put("details", details)
        .put("done", done)
        .put("timestamp", timestamp)
}