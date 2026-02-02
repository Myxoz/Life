package com.myxoz.life.utils

import org.json.JSONArray
import org.json.JSONObject

fun JSONObject.getStringOrNull(name: String): String? = if(isNull(name)) null else getString(name)
fun JSONObject.getJSONObjectOrNull(name: String): JSONObject? = if(isNull(name)) null else getJSONObject(name)
fun JSONObject.getLongOrNull(name: String): Long? = if(isNull(name)) null else getString(name).toLong()
fun JSONObject.getIntOrNull(name: String): Int? = if(isNull(name)) null else getString(name).toInt()
val JSONArray.jsonObjArray: List<JSONObject>
    get() = this.let {
        val retList = mutableListOf<JSONObject>()
        for (i in 0..<it.length()) {
            retList.add(it.getJSONObject(i))
        }
        retList
    }

inline fun JSONArray.forEach(function: (Any) -> Unit) {
    for (item in 0..<length()) {
        function(this.get(item))
    }
}
