package com.myxoz.life.events.additionals

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.myxoz.life.ui.theme.EventColors
import com.myxoz.life.ui.theme.OldColors

enum class EventType(val id: Int, val colors: EventColors, val selectedColor: Color, val calendarName: String){
    Empty(1, OldColors.Calendar.Empty, Color.White, "DEBUG"),
    Sleep(2, OldColors.Calendar.Sleep, Color.White, "Schlaf"),
    Spont(3, OldColors.Calendar.Spont, Color.Black, "Spontan"),
    Hobby(4, OldColors.Calendar.Hobby, Color.White, "Hobby"),
    Learn(5, OldColors.Calendar.Learn, Color.White, "Lernen"),
    Social(6, OldColors.Calendar.Social, Color.White, "Sozial"),
    Travel(7, OldColors.Calendar.Travel, Color.Black, "Travel"),
    DigSoc(8, OldColors.Calendar.DigSoc, Color.Black, "DigSoc"),
    Work(9, OldColors.Calendar.Work, Color.White, "Arbeit"),
    Timewaste(10, OldColors.Calendar.Timewaste, Color.White, "Timewaste"),
    // ALWAYS INCREMENT ID! AND ADD TO BELOW
    ;
    fun isTagEvent() = this in arrayOf(Hobby, Learn, Spont, Social, Work)
    fun isTitleEvent() = this in arrayOf(Hobby, Learn, Spont, Social, DigSoc, Work, Timewaste)
    fun isDetailsEvent() = this in arrayOf(Hobby, Learn, Work)
    fun isPeopleEvent() = this in arrayOf(DigSoc, Social)
    companion object {
        val order = arrayOf(Sleep, Spont, Hobby, Learn, Travel, Social, DigSoc, Work, Timewaste);
        fun getById(id: Int): EventType? = EventType.entries.firstOrNull { it.id == id }.apply { if(this==null) Log.w("Calendar","Couldnt find calendar with id $id in EventType.getById") }
    }
}