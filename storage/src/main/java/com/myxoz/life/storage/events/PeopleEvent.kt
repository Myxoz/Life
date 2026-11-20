package com.myxoz.life.storage.events

sealed interface PeopleEvent {
    val people: Set<Long>
}
