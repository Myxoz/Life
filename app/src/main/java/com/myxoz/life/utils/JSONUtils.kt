package com.myxoz.life.utils

import org.json.JSONObject

fun JSONObject.getStringOrNull(name: String): String? = if(isNull(name)) null else getString(name)
fun JSONObject.getLongOrNull(name: String): Long? = if(isNull(name)) null else getString(name).toLong()
