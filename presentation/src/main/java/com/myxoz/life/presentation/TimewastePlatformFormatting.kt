package com.myxoz.life.presentation

import androidx.annotation.DrawableRes
import com.myxoz.life.storage.events.additionals.TimewastePlatform

enum class TimewastePlatformFormatting(
    val platform: TimewastePlatform,
    override val displayName: String,
    @DrawableRes override val drawable: Int
): DisplayFormattable {
    Youtube(TimewastePlatform.Youtube, "Youtube", R.drawable.yt),
    Instagram(TimewastePlatform.Instagram, "Instagram", R.drawable.insta);
    companion object {
        fun TimewastePlatform.formattable() =
            when(this) {
                TimewastePlatform.Youtube -> Youtube
                TimewastePlatform.Instagram -> Instagram
            }
    }
}