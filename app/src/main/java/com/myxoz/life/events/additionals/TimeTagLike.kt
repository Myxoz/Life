package com.myxoz.life.events.additionals

import org.json.JSONObject

data class TimedTagLikeContainer<T: TagLike>(val type: T, val durationMs: Long) {
    fun timedTagLikeToJson(): JSONObject = JSONObject().put("type", type.id).put("duration_ms", durationMs)
}
