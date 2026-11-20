package com.myxoz.life.presentation

import androidx.annotation.DrawableRes
import com.myxoz.life.storage.events.additionals.EventTag

enum class EventTagFormatting(
    val tag: EventTag,
    override val displayName: String,
    val searchStrings: Array<String>,
    @DrawableRes override val drawable: Int
): DisplayFormattable {
    Party(EventTag.Party, "Feier", arrayOf("Party", "Feiern", "Geburtstagsfeier"), R.drawable.party),
    Eat(EventTag.Eat,
        "Essen",
        arrayOf("Futtern", "Snack", "Snacken", "Frühstück", "Mittagessen", "Abendessen", "Fooden"),
        R.drawable.food
    ),
    Cards(
        EventTag.Cards,
        "Gesellschafsspiele",
        arrayOf("Karten", "Brettspiele", "Poker", "Uno", "Spieleabend"),
        R.drawable.poker_card
    ),
    Shopping(
        EventTag.Shopping,
        "Shopping",
        arrayOf("Einkaufen", "Shoppen", "Mall", "Einkäufe"),
        R.drawable.shopping
    ),
    Design(
        EventTag.Design,
        "Design",
        arrayOf("Concept Art", "Skizzieren", "Zeichnen", "Artwork", "Entwurf"),
        R.drawable.concept
    ),
    Code(EventTag.Code, "Code", arrayOf("Programmieren", "Script", "Dev", "Software"), R.drawable.code),
    Watch(
        EventTag.Watch,
        "Streaming",
        arrayOf("Netflix", "Serien", "Filme", "Film", "Twitch", "Binge", "Kino", "Stream"),
        R.drawable.watch
    ),
    Alcohol(
        EventTag.Alcohol,
        "Saufen",
        arrayOf("Alkohol", "Trinken", "Bier", "Wein", "Shots", "Betrinken", "Besaufen", "Pegel"),
        R.drawable.alc
    ),
    Shower(
        EventTag.Shower,
        "Hygiene",
        arrayOf("Duschen", "Baden", "Pflege", "Rasieren"),
        R.drawable.shower
    ),
    // Skipped id 10
    S(EventTag.S, "S", arrayOf(), R.drawable.s),
    Lecture(EventTag.Lecture, "Vorlesung", arrayOf("Präsentation", "Vorstellung"), R.drawable.lecture),
    Studium(EventTag.Studium, "Uni", arrayOf("Universität", "Studium", "Studieren"), R.drawable.study),
    Android(EventTag.Android, "Android", arrayOf(), R.drawable.android),
    Clean(
        EventTag.Clean,
        "Putzen",
        arrayOf("Saugen", "Schrubben", "Fegen", "Feudeln", "Wischen"),
        R.drawable.clean
    ),
    Cook(EventTag.Cook, "Kochen", arrayOf("Schneiden", "Mixen", "Zubereiten"), R.drawable.cook),
    Sport(EventTag.Sport, "Sport", arrayOf("Gym", "Kraft", "Workout"), R.drawable.sport),
    Talking(
        EventTag.Talking,
        "Reden",
        arrayOf("Labern", "Yappen", "Tea", "Quatschen", "Konversation", "Convo"),
        R.drawable.talk
    ),
    Stroll(EventTag.Stroll, "Spazieren", arrayOf("Laufen", "Gehen"), R.drawable.walk),
    Read(EventTag.Read, "Lesen", arrayOf("Buch"), R.drawable.read);
    fun matches(query: String) = displayName.contains(query, true) ||
            searchStrings.any { it.contains(query, true) }
    companion object {
        fun EventTag.formattable() =
            when(this) {
                EventTag.Party -> Party
                EventTag.Eat -> Eat
                EventTag.Cards -> Cards
                EventTag.Shopping -> Shopping
                EventTag.Design -> Design
                EventTag.Code -> Code
                EventTag.Watch -> Watch
                EventTag.Alcohol -> Alcohol
                EventTag.Shower -> Shower
                EventTag.S -> S
                EventTag.Lecture -> Lecture
                EventTag.Studium -> Studium
                EventTag.Android -> Android
                EventTag.Clean -> Clean
                EventTag.Cook -> Cook
                EventTag.Sport -> Sport
                EventTag.Talking -> Talking
                EventTag.Stroll -> Stroll
                EventTag.Read -> Read
            }
    }
}