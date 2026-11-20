package com.myxoz.life.storage.interfaces.aggregator

import android.annotation.SuppressLint
import android.net.Uri
import com.myxoz.life.storage.api.LifeJSONObject
import com.myxoz.life.storage.dbwrapper.state.KeyValueStore
import com.myxoz.life.storage.dbwrapper.state.KeyValueStorePrefix
import com.myxoz.life.storage.dbwrapper.state.KeyValueType
import com.myxoz.life.storage.stored.AlarmSound
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AlarmAggregator internal constructor(
    private val keyValueStore: KeyValueStore,
) {
    private val _currentSond = MutableStateFlow(getSound())
    val currentSound: StateFlow<AlarmSound?> = _currentSond
    fun setSound(alarmSound: AlarmSound) {
        _currentSond.value = alarmSound
        keyValueStore.put(sounds, alarmSound.toJson())
    }
    private fun getSound() = keyValueStore.get(sounds)?.fromJSON()
    var nextAlarmTs by keyValueStore.getAsState(Companion.nextAlarmTs)
    companion object {
        fun AlarmSound.toJson() = LifeJSONObject().put("name", name).put("uri", uri.toString())
        val prefix = KeyValueStorePrefix("alarm")
        val nextAlarmTs = prefix.keyOfType("nextAlarmTs", KeyValueType.InstantType)
        val sounds = prefix.keyOfType("ringtone", KeyValueType.JSONObjectType)
        @SuppressLint("UseKtx")
        fun LifeJSONObject.fromJSON() =
            AlarmSound(
                getString("name"),
                Uri.parse(getString("uri"))
            )
    }
}