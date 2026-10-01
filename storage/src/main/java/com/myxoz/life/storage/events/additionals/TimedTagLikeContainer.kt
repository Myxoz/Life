package com.myxoz.life.storage.events.additionals

data class TimedTagLikeContainer<T: TagLike>(val type: T, val durationMs: Long)
