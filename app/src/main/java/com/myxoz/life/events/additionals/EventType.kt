package com.myxoz.life.events.additionals

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.myxoz.life.ui.theme.OldColors

enum class EventType(val id: Int, val color: Color, val selectedColor: Color, val calendarName: String){
    Empty(1, OldColors.SELECTED, Color.White, "DEBUG"),
    Sleep(2, OldColors.Calendar.Sleep.BG, Color.White, "Schlaf"),
    Spont(3, OldColors.Calendar.Spont.BG, Color.Black, "Spontan"),
    Hobby(4, OldColors.Calendar.Hobby.BG, Color.White, "Hobby"),
    Learn(5, OldColors.Calendar.Learn.BG, Color.White, "Lernen"),
    Social(6, OldColors.Calendar.Social.BG, Color.White, "Sozial"),
    Travel(7, OldColors.Calendar.Travel.BG, Color.Black, "Travel"),
    DigSoc(8, OldColors.Calendar.DigSoc.BG, Color.Black, "DigSoc"),
    Work(9, OldColors.Calendar.Work.BG, Color.White, "Arbeit"),
    // TODO INCREMENT ID! AND ADD TO BELOW
    ;
    fun isTagEvent() = this in arrayOf(Hobby, Learn, Spont, Social, Work)
    fun isTitleEvent() = this in arrayOf(Hobby, Learn, Spont, Social, DigSoc, Work)
    fun isDetailsEvent() = this in arrayOf(Hobby, Learn, Work)
    fun isPeopleEvent() = this in arrayOf(DigSoc, Social)
    companion object {
        val order = arrayOf(Sleep, Spont, Hobby, Learn, Travel, Social, DigSoc, Work);
        fun getById(id: Int): EventType? = EventType.entries.firstOrNull { it.id == id }.apply { if(this==null) Log.w("Calendar","Couldnt find calendar with id $id in EventType.getById") }
    }
}