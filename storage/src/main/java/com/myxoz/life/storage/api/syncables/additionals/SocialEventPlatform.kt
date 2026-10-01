package com.myxoz.life.storage.api.syncables.additionals

import android.util.Log

enum class SocialEventPlatform(val id: Int) {
    Instagram(1),
    Snapchat(2),
    WhatsApp(3),
    Youtube(4),
    Signal(5),
    Telegram(6);

    companion object {
        fun getById(platform: Int): SocialEventPlatform? = entries.firstOrNull { it.id == platform }
            .apply { if (this == null) Log.w("Person","Couldnt find social with id $platform in Platforms.getById") }
    }
}