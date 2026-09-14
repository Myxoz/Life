package com.myxoz.life.api

import com.myxoz.life.viewmodels.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SyncEngine(
    val api: API,
    val permissionChecker: Settings.Permission.PermissionChecker
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var syncWorker: Job? = null
    private val resyncMutex = Mutex(false)
    private val _lastApiResponse = MutableStateFlow<API.SyncingResponse?>(null)
    private val resyncRequests = Channel<Unit>(Channel.CONFLATED)
    val lastApiResponse = _lastApiResponse.asStateFlow()
    fun suggestResync() {
        if(!Settings.Feature.SyncWithServer.isEnabled(permissionChecker)) return
        resyncRequests.trySend(Unit)
        if (syncWorker?.isActive == true) return

        syncWorker = scope.launch {
            resyncLoop()
        }
    }
    fun stopResync(){
        syncWorker?.cancel()
    }
    private var retryDelay = 5.seconds
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun resyncLoop() {
        while (currentCoroutineContext().isActive) {
            synchronize()
            if (resyncRequests.tryReceive().isSuccess) {
                continue
            }

            // Wait 10 seconds, unless suggestResync() interrupts it.
            select {
                this.onTimeout(retryDelay) {
                    // Select this after the retry delay has passed
                }
                resyncRequests.onReceive {
                    // Select after the update channel emits
                }
            }
        }
    }
    private suspend fun synchronize() {
        val lastReponse = resyncMutex.withLock {
            val sendDataResponse = api.sendToServer()
            if(sendDataResponse !is API.SyncingResponse.SUCCESS) {
                return@withLock sendDataResponse
            }
            val receivedData = api.receiveFromServer()
            return@withLock receivedData
        }
        _lastApiResponse.update { lastReponse }
        retryDelay = when(lastReponse) {
            is API.SyncingResponse.FAILED, API.SyncingResponse.OFFLINE ->
                (retryDelay*2).coerceAtMost(5.minutes)
            is API.SyncingResponse.SUCCESS -> 10.seconds
        }
        return
    }
}