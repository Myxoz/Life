package com.myxoz.life.utils

import org.json.JSONArray
import org.json.JSONObject

fun JSONObject.getServerBoolean(name: String): Boolean {
    val value = getStringOrNull(name) ?: return false
    if(value == "1") return true
    if(value.lowercase() == "true") return true
    return false
}
inline fun JSONObject.getSafeLong(key: String) = getString(key).toLong()
inline fun JSONArray.getSafeLong(id: Int) = getString(id).toLong()
inline fun JSONObject.getStringOrNull(name: String): String? = if(isNull(name)) null else getString(name)
inline fun JSONObject.getJSONObjectOrNull(name: String): JSONObject? = if(isNull(name)) null else getJSONObject(name)
inline fun JSONObject.getJSONArrayOrNull(name: String): JSONArray? = if(isNull(name)) null else getJSONArray(name)
inline fun JSONObject.getLongOrNull(name: String): Long? = if(isNull(name)) null else getString(name).toLong()
inline fun JSONObject.getIntOrNull(name: String): Int? = if(isNull(name)) null else getString(name).toInt()
val JSONArray.jsonObjArray: List<JSONObject>
    get() {
        val retList = mutableListOf<JSONObject>()
        for (i in 0..<this.length()) {
            retList.add(this.getJSONObject(i))
        }
        return retList
    }

inline fun JSONArray.forEach(function: (Any) -> Unit) {
    for (item in 0..<length()) {
        function(this.get(item))
    }
}
inline fun <T> JSONArray.asList(map: JSONArray.(Int) -> T): List<T> {
    val list = mutableListOf<T>()
    for (item in 0..<length()) {
        list.add(map(item))
    }
    return list
}
inline fun <T> JSONArray.asNotNullList(map: JSONArray.(Int) -> T?): List<T> {
    val list = mutableListOf<T>()
    for (item in 0..<length()) {
        list.add(map(item) ?: continue)
    }
    return list
}
inline fun <T> List<T>.jsonArray(map: (T)->Any): JSONArray {
    return JSONArray()
        .apply {
            this@jsonArray.forEach {
                put(map(it))
            }
        }
}
val List<Long>.jsonArray: JSONArray
    inline get() {
    return JSONArray()
        .apply {
            this@jsonArray.forEach {
                put(it.toString())
            }
        }
}
