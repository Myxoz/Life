package com.myxoz.life.api.extensions

import com.myxoz.life.api.syncables.ExtensionSyncable

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