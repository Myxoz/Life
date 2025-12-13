package com.myxoz.life.events.additionals

import org.json.JSONObject

interface TitleEvent {
    val title: String
    fun JSONObject.addTitle(): JSONObject = put("title", title)
}
