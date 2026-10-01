package com.myxoz.life.storage.api.syncables

import com.myxoz.life.storage.api.syncables.additionals.TransactionSplitPart

data class TransactionSplitSyncable(
    override val id: Long,
    val syncableId: Long?,
    val remoteId: String?,
    val parts: List<TransactionSplitPart>
): Syncable {
    internal val key = syncableId to remoteId
}