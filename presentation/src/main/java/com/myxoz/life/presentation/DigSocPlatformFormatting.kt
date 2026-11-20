package com.myxoz.life.presentation

import androidx.annotation.DrawableRes
import com.myxoz.life.storage.events.additionals.DigSocPlatform

enum class DigSocPlatformFormatting(
    val platform: DigSocPlatform,
    override val displayName: String,
    @DrawableRes override val drawable: Int
): DisplayFormattable {
    Call(DigSocPlatform.Call, "Anruf", R.drawable.phone),
    Instagram(DigSocPlatform.Instagram, "Instagram", R.drawable.insta),
    WhatsApp(DigSocPlatform.WhatsApp, "WhatsApp", R.drawable.whatsapp),
    Snapchat(DigSocPlatform.Snapchat, "Snapchat", R.drawable.snap),
    Messages(DigSocPlatform.Messages, "Nachrichten", R.drawable.message);
    companion object {
        fun DigSocPlatform.formattable() =
            when(this) {
                DigSocPlatform.Call -> Call
                DigSocPlatform.Instagram -> Instagram
                DigSocPlatform.WhatsApp -> WhatsApp
                DigSocPlatform.Snapchat -> Snapchat
                DigSocPlatform.Messages -> Messages
            }
    }
}