package com.myxoz.life.storage.api

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class LifeJSONObject(
    private val jsonObject: JSONObject = JSONObject()
) {
    fun put(key: String, value: String?): LifeJSONObject {
        jsonObject.put(key, value ?: JSONObject.NULL)
        return this
    }
    fun put(key: String, value: Instant?): LifeJSONObject = put(key, value?.toEpochMilli())
    fun put(key: String, value: LocalDate?): LifeJSONObject = put(key, value?.toEpochDay())
    fun put(key: String, value: Long?): LifeJSONObject {
        jsonObject.put(key, value?.toString() ?: JSONObject.NULL)
        return this
    }
    fun put(key: String, value: Int?): LifeJSONObject {
        jsonObject.put(key, value ?: JSONObject.NULL)
        return this
    }
    fun put(key: String, value: Boolean?): LifeJSONObject {
        jsonObject.put(key, value ?: JSONObject.NULL)
        return this
    }
    fun put(key: String, value: Double?): LifeJSONObject {
        jsonObject.put(key, value ?: JSONObject.NULL)
        return this
    }
    fun put(key: String, value: LifeJSONArray?): LifeJSONObject {
        jsonObject.put(key, value?.toJSONArray() ?: JSONObject.NULL)
        return this
    }
    fun put(key: String, value: LifeJSONObject?): LifeJSONObject {
        jsonObject.put(key, value?.toJSONObject() ?: JSONObject.NULL)
        return this
    }
    fun put(key: String, value: Duration?): LifeJSONObject {
        put(key, value?.inWholeMilliseconds)
        return this
    }
    fun getLong(key: String) = jsonObject.getString(key).toLong()
    fun getInt(key: String) = jsonObject.getInt(key)
    fun getDouble(key: String) = jsonObject.getDouble(key)
    fun getString(key: String): String = jsonObject.getString(key)
    fun getInstant(key: String): Instant = Instant.ofEpochMilli(getLong(key))
    fun getInstantOrNull(key: String): Instant? = Instant.ofEpochMilli(getLongOrNull(key) ?: return null)
    fun getMillis(key: String): Duration = getLong(key).milliseconds
    // fun getLocalDate(key: String): LocalDate = LocalDate.ofEpochDay(getLong(key))
    fun getBoolean(key: String): Boolean {
        val value = getStringOrNull(key) ?: return false
        if(value == "1") return true
        if(value.lowercase() == "true") return true
        return false
    }
    fun getId() = getLong("id")
    fun getType() = getInt("type")
    fun getJSONArray(key: String): LifeJSONArray {
        return LifeJSONArray(jsonObject.getJSONArray(key))
    }
    fun getJSONObject(key: String): LifeJSONObject =
        LifeJSONObject(jsonObject.getJSONObject(key))

    fun getJSONObjectOrNull(key: String): LifeJSONObject? =
        LifeJSONObject(
            if(jsonObject.isNull(key))
                return null
            else
                jsonObject.getJSONObject(key)
        )


    fun getIntOrNull(key: String) = if(jsonObject.isNull(key)) null else getInt(key)
    fun getLongOrNull(key: String) = if(jsonObject.isNull(key)) null else getLong(key)
    fun getLocalDateOrNull(key: String) = if(jsonObject.isNull(key)) null else LocalDate.ofEpochDay(getLong(key))
    fun getStringOrNull(key: String) = if(jsonObject.isNull(key)) null else getString(key)

    fun toJSONObject() = jsonObject
    fun formatted(): String = jsonObject.toString(2)
    fun asString(): String = jsonObject.toString()
    @Deprecated("Never use this, use asString", level = DeprecationLevel.ERROR)
    override fun toString() = error("Never use this, use asString")

    companion object {
        fun safeFrom(json: String?): LifeJSONObject? =
            runCatching { LifeJSONObject(JSONObject(json ?: return null)) }.getOrNull()
    }
}