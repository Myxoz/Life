package com.myxoz.life.storage.stored

import com.myxoz.life.storage.api.LifeJSONArray
import com.myxoz.life.storage.api.LifeJSONObject
import com.myxoz.life.storage.dbwrapper.state.KeyValueStoreKey
import com.myxoz.life.storage.dbwrapper.state.KeyValueStorePrefix
import com.myxoz.life.storage.dbwrapper.state.KeyValueType
import java.time.Instant

sealed class AutoDetectStored(
    val key: KeyValueStoreKey<Instant>
) {
    object LastAutoDetectPhoneCalls: AutoDetectStored(AUTODETECTPREFIX.keyOfType("lastautodetectphonecalls", KeyValueType.InstantType))
    object LastAutoSleep: AutoDetectStored(AUTODETECTPREFIX.keyOfType("lastautodetectsleep", KeyValueType.InstantType))
    data class LastWhatsAppCall(
        val start: Instant,
        val notificationKey: String,
        val people: Set<Long>
    )
    companion object {
        private val AUTODETECTPREFIX = KeyValueStorePrefix("autodetect")
        val LastWhatsAppCallStored = AUTODETECTPREFIX.keyOfType(
            "lastWhatsAppCall",
            object: KeyValueType.MappedValueType<LastWhatsAppCall>() {
                override fun decodeToType(json: LifeJSONObject): LastWhatsAppCall =
                    LastWhatsAppCall(
                        json.getInstant("start"),
                        json.getString("notificationKey"),
                        json.getJSONArray("people").mapIndexed { getLong(it) }.toSet(),
                    )

                override fun encodeToJSON(value: LastWhatsAppCall): LifeJSONObject = LifeJSONObject()
                    .put("start", value.start)
                    .put("notificationKey", value.notificationKey)
                    .put("people", LifeJSONArray.fromLongs(value.people))
            }
        )
    }
}