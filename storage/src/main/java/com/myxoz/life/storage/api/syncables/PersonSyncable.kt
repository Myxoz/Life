package com.myxoz.life.storage.api.syncables

import com.myxoz.life.storage.api.syncables.additionals.PersonSyncablePlatform

class PersonSyncable(
    override val id: Long,
    val name: String,
    val fullName: String?,
    val phoneNumber: String?,
    val iban: String?,
    val home: Long?,
    val birthday: Long?,
    val socials: Set<PersonSyncablePlatform>
) : Syncable