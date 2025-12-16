package com.myxoz.life.events.additionals

import com.myxoz.life.R

enum class DigSocPlatform(override val id: Int, override val drawable: Int): TagLike {
    Call(1, R.drawable.phone),
    Instagram(2, R.drawable.insta),
    WhatsApp(3, R.drawable.whatsapp),
    Snapchat(4, R.drawable.snap),
    Message(5, R.drawable.message),
    ;
    companion object {
        fun getById(id: Int): DigSocPlatform? = DigSocPlatform.entries.firstOrNull { it.id == id }.apply { if(this==null) println("Couldnt find app with id $id in DigSocEntry.getById") }
    }
}
