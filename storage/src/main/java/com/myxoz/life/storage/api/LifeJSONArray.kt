package com.myxoz.life.storage.api

import org.json.JSONArray

class LifeJSONArray(
    val jsonArray: JSONArray = JSONArray()
) {
    fun isEmpty() = jsonArray.length() > 0
    fun length() = jsonArray.length()

    inline fun forEach(function: (Any) -> Unit) {
        for (item in 0..<jsonArray.length()) {
            function(jsonArray.get(item))
        }
    }

    inline fun forEachJSONObjects(function: (LifeJSONObject) -> Unit) {
        for (item in 0..<jsonArray.length()) {
            function(LifeJSONObject(jsonArray.getJSONObject(item)))
        }
    }

    inline fun <T> filterNonNull(function: (Any) -> T?): List<T> =
        buildList {
            for (item in 0..<jsonArray.length()) {
                function(jsonArray.get(item))?.let { add(it) }
            }
        }

    inline fun <T> mapIndexed(map: LifeJSONArray.(Int) -> T): List<T> =
        buildList {
            for (item in 0..<jsonArray.length()) {
                add(map(item))
            }
        }

    inline fun jsonObjectArray(): List<LifeJSONObject> =
        buildList {
            for (item in 0..<jsonArray.length()) {
                add(LifeJSONObject(jsonArray.getJSONObject(item)))
            }
        }

    inline fun <T> mapJSONObjects(map: (LifeJSONObject) -> T): List<T> =
        buildList {
            for (item in 0..<jsonArray.length()) {
                add(map(LifeJSONObject(jsonArray.getJSONObject(item))))
            }
        }

    inline fun <T> mapJSONObjectsNotNull(map: (LifeJSONObject) -> T?): List<T> =
        buildList {
            for (item in 0..<jsonArray.length()) {
                add(map(LifeJSONObject(jsonArray.getJSONObject(item))) ?: continue)
            }
        }

    inline fun <T> mapNotNullIndexed(map: LifeJSONArray.(Int) -> T?): List<T> =
        buildList {
            for (item in 0..<jsonArray.length()) {
                add(map(item) ?: continue)
            }
        }

    fun getInt(index: Int) = jsonArray.getInt(index)
    fun getLong(index: Int) = jsonArray.getString(index).toLong()
    fun getString(index: Int) = jsonArray.getString(index)
    fun toJSONArray() = jsonArray
    fun formatted(): String = jsonArray.toString(2)
    @Deprecated("Never use this, use asString", level = DeprecationLevel.ERROR)
    override fun toString() = error("Never use this, use asString")
    fun asString(): String = jsonArray.toString()
    companion object {
        fun from(lifeJSONObject: List<LifeJSONObject>): LifeJSONArray {
            val jsonArray = JSONArray()
            lifeJSONObject.forEach {
                jsonArray.put(it.toJSONObject())
            }
            return LifeJSONArray(jsonArray)
        }
        fun <T> from(items: Collection<T>, putContent: LifeJSONObject.(T) -> Unit): LifeJSONArray {
            val jsonArray = JSONArray()
            items.forEach {
                val json = LifeJSONObject()
                json.putContent(it)
                jsonArray.put(json.toJSONObject())
            }
            return LifeJSONArray(jsonArray)
        }
        fun fromInts(intList: List<Int>): LifeJSONArray {
            val jsonArray = JSONArray()
            intList.forEach {
                jsonArray.put(it)
            }
            return LifeJSONArray(jsonArray)
        }
        fun fromLongs(intList: List<Long>): LifeJSONArray {
            val jsonArray = JSONArray()
            intList.forEach {
                jsonArray.put(it.toString())
            }
            return LifeJSONArray(jsonArray)
        }
        fun fromStrings(intList: List<String>): LifeJSONArray {
            val jsonArray = JSONArray()
            intList.forEach {
                jsonArray.put(it)
            }
            return LifeJSONArray(jsonArray)
        }
        fun safeFrom(json: String?): LifeJSONArray? =
            runCatching { LifeJSONArray(JSONArray(json ?: return null)) }.getOrNull()
    }
}