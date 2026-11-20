package com.myxoz.life.storage.convertion

import android.content.Context
import com.myxoz.life.core.nullIfEmpty
import com.myxoz.life.core.toInstant
import com.myxoz.life.storage.AutoDetectLocalEvent
import com.myxoz.life.storage.api.LifeJSONObject
import com.myxoz.life.storage.api.syncables.BankingSidecarSyncable
import com.myxoz.life.storage.api.syncables.BankingSyncable
import com.myxoz.life.storage.api.syncables.CommitSyncable
import com.myxoz.life.storage.api.syncables.FullDaySyncable
import com.myxoz.life.storage.api.syncables.LocationSyncable
import com.myxoz.life.storage.api.syncables.ManualTransactionSyncable
import com.myxoz.life.storage.api.syncables.PersonSyncable
import com.myxoz.life.storage.api.syncables.ProfilePictureSyncable
import com.myxoz.life.storage.api.syncables.ProfilePictureSyncable.Companion.loadBase64ByPerson
import com.myxoz.life.storage.api.syncables.TodoSyncable
import com.myxoz.life.storage.api.syncables.TransactionSplitSyncable
import com.myxoz.life.storage.api.syncables.additionals.DayScreenTime
import com.myxoz.life.storage.api.syncables.additionals.PersonSyncablePlatform
import com.myxoz.life.storage.api.syncables.additionals.SocialEventPlatform
import com.myxoz.life.storage.api.syncables.additionals.TransactionSplitPart
import com.myxoz.life.storage.convertion.SyncableFromJSON.commitModificationFromJSON
import com.myxoz.life.storage.convertion.events.EventsFromJSON.getRawEventFromJSON
import com.myxoz.life.storage.dbwrapper.banking.BankingEntity
import com.myxoz.life.storage.dbwrapper.banking.BankingSidecarEntity
import com.myxoz.life.storage.dbwrapper.banking.ManualTransactionEntity
import com.myxoz.life.storage.dbwrapper.banking.ReadBankingDao
import com.myxoz.life.storage.dbwrapper.banking.TransactionSplitEntity
import com.myxoz.life.storage.dbwrapper.banking.TransactionSplitPartEntity
import com.myxoz.life.storage.dbwrapper.commits.CommitEntity
import com.myxoz.life.storage.dbwrapper.days.DayScreenTimeEntity
import com.myxoz.life.storage.dbwrapper.days.DaysEntity
import com.myxoz.life.storage.dbwrapper.days.ReadDaysDao
import com.myxoz.life.storage.dbwrapper.localevent.AutoDetectLocalEventEntity
import com.myxoz.life.storage.dbwrapper.locations.LocationEntity
import com.myxoz.life.storage.dbwrapper.people.PersonEntity
import com.myxoz.life.storage.dbwrapper.people.ProfilePictureStored
import com.myxoz.life.storage.dbwrapper.people.ReadPeopleDao
import com.myxoz.life.storage.dbwrapper.people.SocialsEntity
import com.myxoz.life.storage.dbwrapper.todos.TodoEntity
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

internal object SyncableFromEntity {
    fun ManualTransactionEntity.fromEntity() =
        ManualTransactionSyncable(id, digital, cashless, amountCents, name, timestamp.toInstant(), purpose)

    fun BankingEntity.fromEntity() =
        BankingSyncable(id, card, transfer, amountCents, currency, bookingTime, valueDate.toInstant(),
            fromName, fromIban, fromBic, purpose, saldoAfterCents, purposeDate?.toInstant(), lastUpdate.toInstant())

    fun CommitEntity.fromEntity() =
        CommitSyncable(repoOwner, repoName, commitSha, commitMessage, commitAuthor, commitEmail, commitDate?.toInstant(), additions, deletions, filesChanged,
            commitModificationFromJSON(filesJson), commitUrl, updated.toInstant())

    fun TodoEntity.fromEntity() =
        TodoSyncable(id, short, details, done, timestamp.toInstant())

    suspend fun PersonEntity.fromEntity(db: ReadPeopleDao) =
        PersonSyncable(id, name, fullname, phoneNumber, iban, home, birthday?.let { LocalDate.ofEpochDay(it) },
            db.getSocialsByPerson(id).mapNotNullTo(mutableSetOf()) { it.fromEntity() }
        )

    private fun SocialsEntity.fromEntity(): PersonSyncablePlatform? =
        PersonSyncablePlatform(SocialEventPlatform.getById(platform) ?: return null, handle.nullIfEmpty())

    fun LocationEntity.fromEntity() =
        LocationSyncable(name, longitude, lat, radiusM, ssid, street, number, city, country, id)

    fun BankingSidecarEntity.fromEntity() =
        BankingSidecarSyncable(id, transactionId, name, date.toInstant())

    suspend fun TransactionSplitEntity.fromEntity(db: ReadBankingDao) =
        TransactionSplitSyncable(id, syncableId, remoteId,
            db.getTransactionSplitParts(id).map{
                it.fromEntity()
            }
        )

    private fun TransactionSplitPartEntity.fromEntity() =
        TransactionSplitPart(person, amount)

    fun ProfilePictureStored.fromEntity(context: Context) =
        ProfilePictureSyncable(personId, if(this.hasPP) loadBase64ByPerson(context, personId) else null)

    suspend fun DaysEntity.fromEntity(readDaysDao: ReadDaysDao) =
        FullDaySyncable(
            happyness,
            stress,
            successfulness,
            steps,
            screenTimeMs.milliseconds,
            readDaysDao.getScreenTimesByDay(id).map { it.fromEntity() },
            id,
        )
    fun DayScreenTimeEntity.fromEntity() = DayScreenTime(packagename, duration.milliseconds)
    fun AutoDetectLocalEventEntity.fromEntity(): AutoDetectLocalEvent? = AutoDetectLocalEvent(
        id,
        LifeJSONObject.safeFrom(content)?.getRawEventFromJSON() ?: return null
    )
}