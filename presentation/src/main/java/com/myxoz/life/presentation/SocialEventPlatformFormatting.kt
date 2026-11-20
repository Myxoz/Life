package com.myxoz.life.presentation

import android.content.ComponentName
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.myxoz.life.core.nullIfEmpty
import com.myxoz.life.storage.api.syncables.PersonSyncable
import com.myxoz.life.storage.api.syncables.additionals.PersonSyncablePlatform
import com.myxoz.life.storage.api.syncables.additionals.SocialEventPlatform

enum class SocialEventPlatformFormatting(
    val socialEventPlatform: SocialEventPlatform,
    val fullName: String,
    val short: String,
    @DrawableRes val icon: Int,
    val color: Color,
    val priority: Int,
    private val openApp: PersonPlatformDetails.(opener: PlatformOpener) -> Unit
) {
    Instagram(
        SocialEventPlatform.Instagram,
        "Instagram",
        "insta",
        R.drawable.insta,
        Color(0xFFE1306C),
        0,
        { opener -> opener.web("https://www.instagram.com/$handle") }
    ),
    Snapchat(
        SocialEventPlatform.Snapchat,
        "Snapchat",
        "snap",
        R.drawable.snap,
        Color(0xFFFFFC00),
        1,
        { opener -> opener.web("https://www.snapchat.com/add/$handle") }
    ),
    WhatsApp(
        SocialEventPlatform.WhatsApp,
        "WhatsApp",
        "wa",
        R.drawable.whatsapp,
        Color(0xFF25D366),
        9,
        { operer ->
            operer.intent(
                ComponentName("com.whatsapp", "com.whatsapp.Conversation"),
                listOf(
                    "jid" to (handle.takeIf { it != WhatsApp.fullName } ?: phoneNumber ?: "").replace(
                        "\\D".toRegex(),
                        ""
                    ) + "@s.whatsapp.net"
                )
            )
        }
    ),
    Youtube(
        SocialEventPlatform.Youtube,
        "YouTube",
        "youtube",
        R.drawable.yt,
        Color(0xFFFF0000),
        -1,
        {operer -> operer.web("https://youtube.com/@$handle") }
    ),
    Signal(
        SocialEventPlatform.Signal,
        "Signal",
        "signal",
        R.drawable.signal,
        Color(0xFF3b45fd),
        10,
        {operer -> operer.web("https://signal.me/#p/$phoneNumber") }
    ),
    Telegram(
        SocialEventPlatform.Telegram,
        "Telegram",
        "telegram",
        R.drawable.telegram,
        Color(0xFF2AABEE),
        2,
        {operer -> operer.web(if (handle != Telegram.fullName) "https://t.me/$handle" else "https://t.me/$phoneNumber") }
    )
    ;
    fun PersonSyncable.orderedSocialEventPlatforms(): List<PersonSyncablePlatform> {
        return socials.sortedBy { it.platform.formattable() }
    }

    private data class PersonPlatformDetails(
        val handle: String?,
        val phoneNumber: String?,
    ) {
        companion object {
            fun of(personSyncable: PersonSyncable, platform: PersonSyncablePlatform) = PersonPlatformDetails(
                platform.handle,
                personSyncable.phoneNumber,
            )
        }
    }
    interface PlatformOpener {
        fun web(url: String)

        fun intent(componentName: ComponentName, extras: List<Pair<String, String>>)
    }

    companion object {
        fun parsePersonSyncablePlatformByDisplay(display: String): PersonSyncablePlatform? {
            if (!display.contains("@")) return null
            return PersonSyncablePlatform(
                getPlatformByShort(display.substringBefore("@"))?.socialEventPlatform ?: return null,
                display.substringAfter("@").nullIfEmpty()
            )
        }
        fun SocialEventPlatform.formattable() =
            when(this) {
                SocialEventPlatform.Instagram -> Instagram
                SocialEventPlatform.Snapchat -> Snapchat
                SocialEventPlatform.WhatsApp -> WhatsApp
                SocialEventPlatform.Youtube -> Youtube
                SocialEventPlatform.Signal -> Signal
                SocialEventPlatform.Telegram -> Signal
            }

        private val nameMap = SocialEventPlatformFormatting.entries.associateBy { it.short }
        fun getPlatformByShort(name: String) = nameMap[name]
    }
}
