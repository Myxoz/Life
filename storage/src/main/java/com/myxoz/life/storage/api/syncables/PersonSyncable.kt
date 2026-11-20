package com.myxoz.life.storage.api.syncables

import com.myxoz.life.storage.api.syncables.additionals.PersonSyncablePlatform
import java.time.LocalDate

data class PersonSyncable(
    override val id: Long,
    val name: String,
    val fullName: String?,
    val phoneNumber: String?,
    val iban: String?,
    val home: Long?,
    val birthday: LocalDate?,
    val socials: Set<PersonSyncablePlatform>
) : Syncable