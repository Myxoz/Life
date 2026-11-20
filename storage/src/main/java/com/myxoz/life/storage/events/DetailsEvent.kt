package com.myxoz.life.storage.events

sealed interface DetailsEvent {
    val details: String?
}
