package com.myxoz.life.events.additionals

import org.json.JSONObject

interface DetailsEvent {
    val details: String?
    fun JSONObject.addDetails(): JSONObject = this.put("details", details?: JSONObject.NULL)
}
