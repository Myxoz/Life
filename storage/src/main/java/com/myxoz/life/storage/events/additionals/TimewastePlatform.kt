package com.myxoz.life.storage.events.additionals

import android.util.Log

enum class TimewastePlatform(internal val id: Int): TagLike {
    Youtube(1),
    Instagram(2),
    ;
    companion object {
        fun getById(id: Int): TimewastePlatform? = entries.firstOrNull { it.id == id }.apply { if(this==null) Log.e("TimewastePlatform", "Couldnt find app with id $id in TimewastePlatform.getById") }
    }
}