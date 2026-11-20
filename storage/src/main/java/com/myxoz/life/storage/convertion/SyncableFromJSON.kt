package com.myxoz.life.storage.convertion

import com.myxoz.life.storage.api.LifeJSONArray
import com.myxoz.life.storage.api.LifeJSONObject
import com.myxoz.life.storage.api.helper.CommitModification
import com.myxoz.life.storage.api.syncables.BankingSidecarSyncable
import com.myxoz.life.storage.api.syncables.BankingSyncable
import com.myxoz.life.storage.api.syncables.CommitSyncable
import com.myxoz.life.storage.api.syncables.DeleteEntry
import com.myxoz.life.storage.api.syncables.ExtensionSyncable
import com.myxoz.life.storage.api.syncables.ExtensionSyncable.ExtensionSyncableType
import com.myxoz.life.storage.api.syncables.FullDaySyncable
import com.myxoz.life.storage.api.syncables.LocationSyncable
import com.myxoz.life.storage.api.syncables.ManualTransactionSyncable
import com.myxoz.life.storage.api.syncables.PersonSyncable
import com.myxoz.life.storage.api.syncables.ProfilePictureSyncable
import com.myxoz.life.storage.api.syncables.ServerSyncable
import com.myxoz.life.storage.api.syncables.SyncableIds
import com.myxoz.life.storage.api.syncables.SyncableIds.EVENTRANGE
import com.myxoz.life.storage.api.syncables.TodoSyncable
import com.myxoz.life.storage.api.syncables.TransactionSplitSyncable
import com.myxoz.life.storage.api.syncables.additionals.DayScreenTime
import com.myxoz.life.storage.api.syncables.additionals.PersonSyncablePlatform
import com.myxoz.life.storage.api.syncables.additionals.SocialEventPlatform
import com.myxoz.life.storage.api.syncables.additionals.TransactionSplitPart
import com.myxoz.life.storage.convertion.events.EventsFromJSON.getSyncedEventFromJSON

internal object SyncableFromJSON {
    fun fromJSON(json: LifeJSONObject): ServerSyncable {
        val cal = json.getType()
        val id = json.getId()
        return when (cal) {
            in Int.MIN_VALUE..<0 -> DeleteEntry(-cal, id)
            SyncableIds.DAYS -> fullDaySyncableFromJSON(json)
            SyncableIds.BANKING -> bankingSyncableFromJSON(json)
            SyncableIds.PEOPLE -> personSyncableFromJSON(json)
            SyncableIds.LOCATIONS -> locationSyncableFromJSON(json)
            SyncableIds.BANKINGSIDECAR -> bankingSidecarFromJSON(json)
            SyncableIds.PROFILEPICTURE -> profilePictureFromJSON(json)
            SyncableIds.COMMITS -> commitSyncableFromJSON(json)
            SyncableIds.MANUALTRANSACTION -> manualTransactionSyncableFromJSON(json)
            SyncableIds.TODOS -> todoFromJSON(json)
            SyncableIds.TRANSACTIONSPLIT -> transactionSplitFromJSON(json)
            SyncableIds.EXTENSION -> extensionFromJSON(json)
            in EVENTRANGE -> json.getSyncedEventFromJSON()
            else -> error("Event ")
        }
    }

    fun fullDaySyncableFromJSON(json: LifeJSONObject): FullDaySyncable =
        FullDaySyncable(
            json.getInt("happyness"),
            json.getInt("stress"),
            json.getInt("successfulness"),
            json.getInt("steps"),
            json.getMillis("screen_time_ms"),
            json.getJSONArray("top").mapJSONObjects {
                DayScreenTime(
                    it.getString("packagename"),
                    it.getMillis("duration_ms"),
                )
            },
            json.getLong("date")
        )

    fun manualTransactionSyncableFromJSON(json: LifeJSONObject) =
        ManualTransactionSyncable(
            json.getId(),
            json.getBoolean("digital"),
            json.getBoolean("cashless"),
            json.getInt("amount"),
            json.getString("name"),
            json.getInstant("timestamp"),
            json.getStringOrNull("purpose"),
        )
    fun bankingSyncableFromJSON(json: LifeJSONObject) =
        BankingSyncable(
            json.getString("id"),
            json.getString("card") == "1",
            json.getString("transfer") == "1",
            json.getString("amount_cents").toInt(),
            json.getString("currency"),
            json.getString("booking_time"),
            json.getInstant("value_date"),
            json.getString("from_name"),
            json.getString("from_iban"),
            json.getString("from_bic"),
            json.getString("purpose"),
            json.getLong("saldo_after_cents"),
            json.getInstantOrNull("purpose_date"),
            json.getInstant("last_update")
        )

    fun commitModificationFromJSON(json: String?): List<CommitModification> =
        LifeJSONArray.safeFrom(json)?.mapJSONObjects {
            CommitModification(
                it.getString("filename"),
                it.getInt("additions"),
                it.getInt("deletions"),
                it.getInt("changes"),
                it.getString("filename"),
            )
        } ?: emptyList()

    fun todoFromJSON(json: LifeJSONObject) = TodoSyncable(
        json.getId(),
        json.getString("short"),
        json.getStringOrNull("details"),
        json.getBoolean("done"),
        json.getInstant("timestamp")
    )

    fun personSyncableFromJSON(json: LifeJSONObject): PersonSyncable {
        val id = json.getId()
        val name = json.getString("name")
        val fullName = json.getStringOrNull("fullname")
        val phone = json.getStringOrNull("phone")
        val iban = json.getStringOrNull("iban")
        val home = json.getLongOrNull("home")
        val birthday = json.getLocalDateOrNull("birthday")
        val socials = json.getJSONArray("socials").mapJSONObjectsNotNull {
            PersonSyncablePlatform(
                SocialEventPlatform.getById(it.getInt("platform")) ?:
                    return@mapJSONObjectsNotNull null,
                it.getStringOrNull("handle")
            )
        }.toSet()
        return PersonSyncable(id, name, fullName, phone, iban, home, birthday, socials)
    }

    fun commitSyncableFromJSON(json: LifeJSONObject): CommitSyncable =
        CommitSyncable(
            json.getString("repo_owner"),
            json.getString("repo_name"),
            json.getString("commit_sha"),
            json.getStringOrNull("commit_message"),
            json.getStringOrNull("commit_author"),
            json.getStringOrNull("commit_email"),
            json.getInstantOrNull("commit_date"),
            json.getIntOrNull("additions"),
            json.getIntOrNull("deletions"),
            json.getIntOrNull("files_changed"),
            commitModificationFromJSON(json.getStringOrNull("files_json")),
            json.getStringOrNull("commit_url"),
            json.getInstant("updated"),
        )

    fun locationSyncableFromJSON(json: LifeJSONObject) =
        LocationSyncable(json.getString("name"),
        json.getDouble("longitude"),
        json.getDouble("lat"),
        json.getInt("radiusm"),
        json.getStringOrNull("ssid"),
        json.getStringOrNull("street"),
        json.getStringOrNull("number"),
        json.getStringOrNull("city"),
        json.getString("country"),
        json.getId()
    )

    fun bankingSidecarFromJSON(json: LifeJSONObject) = BankingSidecarSyncable(
        json.getId(),
        json.getString("transactionId"),
        json.getString("name"),
        json.getInstant("date")
    )

    fun transactionSplitFromJSON(json: LifeJSONObject) = TransactionSplitSyncable(
        json.getId(),
        json.getLongOrNull("syncable_id"),
        json.getStringOrNull("remote_id"),
        json.getJSONArray("parts").mapJSONObjects {
            TransactionSplitPart(it.getLong("person"), it.getInt("amount"))
        }
    )

    fun profilePictureFromJSON(json: LifeJSONObject) = ProfilePictureSyncable(
        json.getId(),
        json.getStringOrNull("content")
    )

    fun extensionFromJSON(json: LifeJSONObject): ExtensionSyncable {
        val type = ExtensionSyncableType.typeFromId(json.getId()) ?: error("Received unknown extension syncable")
        return ExtensionSyncable(
            type,
            type.parser.fromString(json.getString("data"))
        )
    }
}