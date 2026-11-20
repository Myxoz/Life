package com.myxoz.life.storage.interfaces

import android.content.Context
import com.myxoz.life.storage.api.API
import com.myxoz.life.storage.api.syncables.SyncableIds
import com.myxoz.life.storage.dbwrapper.Daos
import com.myxoz.life.storage.dbwrapper.state.KeyValueStore
import com.myxoz.life.storage.dbwrapper.state.KeyValueStorePrefix
import com.myxoz.life.storage.dbwrapper.state.KeyValueType
import java.io.File

class DebugInterface internal constructor(
    private val api: API,
    private val keyValueStore: KeyValueStore,
    private val stepInterface: StepInterface,
    private val db: Daos,
    private val context: Context,
) {
    suspend fun resetAPI(){
        api.resetLastUpdateDebug()
        db.cleanupDb.clearAllExceptPersistent()
        File(context.filesDir, SyncableIds.PROFILEPICTURE.toString()).deleteRecursively() // Delete Profile Pics
        api.resetLastUpdateDebug()
    }
    fun lastStepAmountSaved() = stepInterface.debugGetSteps()
    fun stepsAtMidnight() = stepInterface.debugGetMidnightSteps()
    fun stepsLastSaved() = stepInterface.debugLastSteps()
    var stepResetLog = keyValueStore.getAsState(stepLogs)
    companion object {
        private val debugKeyPrefix = KeyValueStorePrefix("debug")
        private val stepLogs = debugKeyPrefix.keyOfType("stepLog", KeyValueType.StringListType)
    }
}