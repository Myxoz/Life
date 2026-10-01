package com.myxoz.life.storage.api.helper

import android.content.Context
import com.myxoz.life.storage.api.API
import com.myxoz.life.storage.api.syncables.Syncable
import com.myxoz.life.storage.convertion.RemoveSyncableFromDatabase.deleteAndDoNotSync
import com.myxoz.life.storage.convertion.RemoveSyncableFromDatabase.deleteAndSync
import com.myxoz.life.storage.convertion.SavePersistent.saveAndRequestSync
import com.myxoz.life.storage.dbwrapper.WaitingSyncDao

internal class SyncHelper(
    private val waitingSyncDao: WaitingSyncDao,
    private val context: Context,
    private val writeSyncableDaos: API.WriteSyncableDaos,
    private val readSyncableDaos: API.ReadSyncableDaos
) {
    companion object {
        suspend fun Syncable.deleteAndSync(syncHelper: SyncHelper) {
            // Cancel syncing attempt
            deleteAndSync(
                syncHelper.waitingSyncDao,
                syncHelper.writeSyncableDaos,
                syncHelper.context
            )
        }

        suspend fun Syncable.deleteAndDoNotSync(syncHelper: SyncHelper, thoroughDeletion: Boolean) {
            deleteAndDoNotSync(
                syncHelper.writeSyncableDaos,
                syncHelper.context,
                thoroughDeletion
            )
        }
        suspend fun Syncable.saveAndRequestSync(syncHelper: SyncHelper) {
             saveAndRequestSync(
                syncHelper.waitingSyncDao,
                syncHelper.writeSyncableDaos,
                syncHelper.readSyncableDaos,
                syncHelper.context
            )
        }
    }
}