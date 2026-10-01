package com.myxoz.life.storage.api.extensions

import com.myxoz.life.storage.api.syncables.ExtensionSyncable

class FavoriteContactsExtendable(
    val ids: List<Long>
): ExtensionSyncable.Extendable {
    override fun specificsToString() = ids.joinToString(",") { it.toString() }
    companion object: ExtensionSyncable.ExtendableCompanion<FavoriteContactsExtendable> {
        override fun fromString(data: String): FavoriteContactsExtendable =
            FavoriteContactsExtendable(
                data.split(",").mapNotNull { it.toLongOrNull() }
            )
    }
}