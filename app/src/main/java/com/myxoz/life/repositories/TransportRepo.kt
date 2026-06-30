package com.myxoz.life.repositories

import com.myxoz.life.api.API
import com.myxoz.life.ui.feed.instantevents.InstantEvent

/**
 * This repo is used to transport data into their respective view model, that cannot be parameterized
 */
class TransportRepo {
    val instantEvents = Transporter<List<InstantEvent>>()
    class Transporter<T> {
        private val transports = mutableMapOf<Long, T>()

        fun upload(events: T): Long {
            val id = API.generateId()
            transports[id] = events
            return id
        }

        fun consume(id: Long): T {
            return transports.remove(id) ?: error("Transport tried to consume data, not yet uploaded. Id: $id")
        }
    }
}