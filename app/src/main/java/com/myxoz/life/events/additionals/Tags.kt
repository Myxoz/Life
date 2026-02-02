package com.myxoz.life.events.additionals

import com.myxoz.life.R
import com.myxoz.life.dbwrapper.events.TagsEntity
import com.myxoz.life.dbwrapper.events.WriteEventDetailsDao
import com.myxoz.life.utils.forEach
import org.json.JSONArray
import org.json.JSONObject

enum class EventTag(override val id: Int, val displayName: String, val queryString: List<String>, override val drawable: Int): TagLike {
    Party(1, "Feier", listOf("Party", "Feiern", "Geburtstagsfeier"), R.drawable.party),
    Eat(2, "Essen", listOf("Futtern", "Snack", "Snacken", "Frühstück", "Mittagessen", "Abendessen", "Fooden"), R.drawable.food),
    Cards(3, "Gesellschafsspiele", listOf("Karten", "Brettspiele", "Poker", "Uno", "Spieleabend"), R.drawable.poker_card),
    Shopping(4, "Shopping", listOf("Einkaufen", "Shoppen", "Mall", "Einkäufe"), R.drawable.shopping),
    Design(5, "Design", listOf("Concept Art", "Skizzieren", "Zeichnen", "Artwork", "Entwurf"), R.drawable.concept),
    Code(6, "Code", listOf("Programmieren", "Script", "Dev", "Software"), R.drawable.code),
    Watch(7,"Streaming", listOf("Netflix", "Serien", "Filme", "Film", "Twitch", "Binge", "Kino", "Stream"), R.drawable.watch),
    Alcohol(8, "Saufen", listOf("Alkohol", "Trinken", "Bier", "Wein", "Shots", "Betrinken", "Besaufen", "Pegel"), R.drawable.alc),
    Shower(9, "Hygiene", listOf("Duschen", "Baden", "Pflege", "Rasieren"), R.drawable.shower),
    // Skipped id 10
    S(11, "S", listOf(), R.drawable.s),
    Lecture(12, "Vorlesung", listOf("Präsentation", "Vorstellung"), R.drawable.lecture),
    Studium(13, "Uni", listOf("Universität", "Studium", "Studieren"), R.drawable.study),
    Android(14, "Android", listOf(), R.drawable.android),
    Clean(15, "Putzen", listOf("Saugen", "Schrubben", "Fegen", "Feudeln", "Wischen"), R.drawable.clean),
    Cook(16, "Kochen", listOf("Schneiden", "Mixen", "Zubereiten"), R.drawable.cook),
    ;
    fun matches(query: String) = displayName.contains(query, true) || queryString.any { it.contains(query, true) }
    // Always add to getResource and to getTagById!!!
    companion object {
        fun getTagById(id: Int) = EventTag.entries.firstOrNull {
            it.id == id
        }
    }
}
interface TagLike {
    val id: Int
    val drawable: Int
}
interface TagEvent {
    val eventTags: List<EventTag>
    suspend fun storeTags(tagsDao: WriteEventDetailsDao, id: Long){
        eventTags.forEach {
            tagsDao.insertTag(
                TagsEntity(
                    id,
                    it.id
                )
            )
        }
    }
    fun containsTagLike(query: String) = eventTags.any { it.matches(query) }
    fun JSONObject.addTags(): JSONObject = put("tags", JSONArray().apply {eventTags.forEach { put(it.id) }})
    companion object {
        fun JSONObject.getTagsFromJson() = getJSONArray("tags").run {
            val list = mutableListOf<EventTag>()
            forEach {
                val eventTag = EventTag.getTagById(it as Int)?:return@forEach
                list.add(eventTag)
            }
            list
        }
    }
}