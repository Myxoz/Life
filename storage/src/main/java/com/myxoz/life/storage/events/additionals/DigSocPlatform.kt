package com.myxoz.life.storage.events.additionals

import android.util.Log

enum class DigSocPlatform(internal val id: Int): TagLike {
    Call(1),
    Instagram(2),
    WhatsApp(3),
    Snapchat(4),
    Messages(5),
    ;
    companion object {
        fun getById(id: Int): DigSocPlatform? = DigSocPlatform.entries.firstOrNull { it.id == id }.apply { if(this==null) Log.e("DigSocPlatform", "Couldnt find app with id $id in DigSocEntry.getById") }
    }
}
