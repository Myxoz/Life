package com.myxoz.life.storage.events.additionals

enum class EventTag(internal val id: Int): TagLike {
    Party(1),
    Eat(2),
    Cards(3),
    Shopping(4),
    Design(5),
    Code(6),
    Watch(7),
    Alcohol(8),
    Shower(9),
    // Skipped id 10
    S(11),
    Lecture(12),
    Studium(13),
    Android(14),
    Clean(15),
    Cook(16),
    Sport(17),
    Talking(18),
    Stroll(19),
    Read(20),
    ;
    companion object {
        fun getTagById(id: Int) = EventTag.entries.firstOrNull { it.id == id }
    }
}