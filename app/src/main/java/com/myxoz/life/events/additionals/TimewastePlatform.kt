package com.myxoz.life.events.additionals

import android.util.Log
import com.myxoz.life.R

enum class TimewastePlatform(override val id: Int, override val displayName: String, override val drawable: Int): TagLike {
    Youtube(1, "Youtube", R.drawable.yt),
    Instagram(2, "Instagram", R.drawable.insta)
    ;
    companion object {
        fun getById(id: Int): TimewastePlatform? = entries.firstOrNull { it.id == id }.apply { if(this==null) Log.e("TimewastePlatform", "Couldnt find app with id $id in TimewastePlatform.getById") }
    }
}
