package com.myxoz.life.events.additionals

import androidx.compose.ui.graphics.Color
import com.myxoz.life.ui.theme.Colors

enum class EventType(val id: Int, val color: Color, val selectedColor: Color, val calendarName: String){
    Empty(1, Colors.SELECTED, Color.White, "DEBUG"),
    Sleep(2, Colors.Calendar.Sleep.BG, Color.White, "Sleep"),
    Spont(3, Colors.Calendar.Spont.BG, Color.Black, "Spontan"),
    Hobby(4, Colors.Calendar.Hobby.BG, Color.White, "Hobby"),
    Learn(5, Colors.Calendar.Learn.BG, Color.White, "Lernen"),
    Social(6, Colors.Calendar.Social.BG, Color.White, "Sozial"),
    Travel(7, Colors.Calendar.Travel.BG, Color.Black, "Travel"),
    DigSoc(8, Colors.Calendar.DigSoc.BG, Color.Black, "DigSoc"),
    // TODO INCREMENT ID!
    ;
    companion object {
        fun getById(id: Int): EventType? = EventType.entries.firstOrNull { it.id == id }.apply { if(this==null) println("Couldnt find calendar with id $id in EventType.getById") }
    }
}