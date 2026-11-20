package com.myxoz.life.storage.events.additionals

import kotlin.time.Duration

data class TimedTagLikeContainer<T: TagLike>(val type: T, val duration: Duration)
