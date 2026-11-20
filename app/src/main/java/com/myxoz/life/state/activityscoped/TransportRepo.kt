package com.myxoz.life.state.activityscoped

import androidx.lifecycle.ViewModel
import com.myxoz.life.ui.feed.instantevents.InstantEvent
import kotlin.random.Random

/**
 * This repo is used to transport data into their respective view model, that cannot be parameterized
 */
// TODO Pivot towards using the navpath route as data store
@Deprecated("Pivot towards using the navpath route as data store")
class TransportRepo: ViewModel() {
    val instantEvents = Transporter<List<InstantEvent>>()
    class Transporter<T> {
        private val transports = mutableMapOf<Long, T>()

        fun upload(events: T): Long {
            val id = Random.nextLong()
            transports[id] = events
            return id
        }

        fun consume(id: Long): T {
            return transports.remove(id) ?: error("Transport tried to consume data, not yet uploaded. Id: $id")
        }
    }
}