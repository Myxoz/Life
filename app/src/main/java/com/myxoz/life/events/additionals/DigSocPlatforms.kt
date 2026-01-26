package com.myxoz.life.events.additionals

import android.util.Log
import com.myxoz.life.R

enum class DigSocPlatform(override val id: Int, override val drawable: Int): TagLike {
    Anruf(1, R.drawable.phone),
    Instagram(2, R.drawable.insta),
    WhatsApp(3, R.drawable.whatsapp),
    Snapchat(4, R.drawable.snap),
    Nachrichten(5, R.drawable.message),
    ;
    companion object {
        fun getById(id: Int): DigSocPlatform? = DigSocPlatform.entries.firstOrNull { it.id == id }.apply { if(this==null) Log.e("DigSocPlatform", "Couldnt find app with id $id in DigSocEntry.getById") }
    }
}
