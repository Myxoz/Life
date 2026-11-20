package com.myxoz.life.presentation.events

import androidx.compose.ui.graphics.Color
import com.myxoz.life.storage.events.DigSocEvent
import com.myxoz.life.storage.events.EmptyEvent
import com.myxoz.life.storage.events.HobbyEvent
import com.myxoz.life.storage.events.LearnEvent
import com.myxoz.life.storage.events.PeopleEvent
import com.myxoz.life.storage.events.RawEvent
import com.myxoz.life.storage.events.SleepEvent
import com.myxoz.life.storage.events.SocialEvent
import com.myxoz.life.storage.events.SpontEvent
import com.myxoz.life.storage.events.TimewasteEvent
import com.myxoz.life.storage.events.TravelEvent
import com.myxoz.life.storage.events.WorkEvent

private val TIMEWASTETEXTCOLOR = Color(0xFFFFFFFF)
private val WORKTEXTCOLOR = Color.White
private val SPONTTEXTCOLOR = Color(0xFF444444)
enum class FormattableEvent(
    val displayName: String,
    val textColors: Color,
    val bg: Color,
    val secondary: Color?,
    val tagColors: TagColors?
) {
    Empty(
        "DEBUG",
        Color(0xFFFFFFFF),
        Color(0xFFFF7777),
        null,
        null
    ),
    Timewaste(
        "Timewaste",
        TIMEWASTETEXTCOLOR,
        Color(0xFF4A412A),
        null,
        TagColors(TIMEWASTETEXTCOLOR, Color(0xFF342D1E)),
    ),
    Sleep(
        "Schlaf",
        Color(0xFFbfbfbf),
        Color(0xFF666666),
        null,
        null
    ),
    Work(
        "Arbeit",
        WORKTEXTCOLOR,
        Color(0xFF7A2E3A),
        Color(0xFFAAAAAA),
        TagColors(Color.Black, WORKTEXTCOLOR),
    ),
    Spont(
        "Spontan",
        SPONTTEXTCOLOR,
        Color(0xFFFFEE6A),
        null,
        TagColors(Color.White, SPONTTEXTCOLOR)
    ),
    Hobby(
        "Hobby",
        Color(0xFFFFFFFF),
        Color(0xFF5C4899),
        Color(0xFFAAAAAA),
        TagColors(Color.White, Color(0xFF3f258c)),
    ),
    Learn(
        "Lernen",
        Color(0xFFFFFFFF),
        Color(0xFF368BB3),
        Color(0xFFDEDEDE),
        TagColors(Color.White, Color(0xFF266380))
    ),
    Social(
        "Sozial",
        Color(0xFFFFFFFF),
        Color(0xFF41a68d),
        Color(0xFFE3E3E3),
        TagColors(Color.White, Color(0xFF317e6b))
    ),
    Travel(
        "Travel",
        Color(0xFF363636),
        Color(0xFFafa084),
        Color(0xFF585858),
        null
    ),
    DigSoc(
        "DigSoc",
        Color(0xFF363636),
        Color(0xFF94D16E),
        Color(0xFF585858),
        TagColors(Color(0xFF363636), Color(0xFFE3E3E3))
    );
    companion object {
        val TagEvents = setOf(Spont, Hobby, Learn, Social, Work)
        val TitleEvents = setOf(Spont, Hobby, Learn, Social, DigSoc, Work, Timewaste)
        val DetailsEvents = setOf(Hobby, Learn, Work)
        val PeopleEvent = setOf(Social, DigSoc)
        fun RawEvent.formattable() =
            when(this) {
                is DigSocEvent -> DigSoc
                is EmptyEvent -> Empty
                is HobbyEvent -> Hobby
                is LearnEvent -> Learn
                is SleepEvent -> Sleep
                is SocialEvent -> Social
                is SpontEvent -> Spont
                is TimewasteEvent -> Timewaste
                is TravelEvent -> Travel
                is WorkEvent -> Work
            }

        fun PeopleEvent.formattable() =
            when(this) {
                is DigSocEvent -> DigSoc
                is SocialEvent -> Social
            }
    }
    data class TagColors(val ICON: Color, val CONTAINER: Color)
}