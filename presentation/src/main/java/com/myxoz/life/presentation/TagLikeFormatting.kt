package com.myxoz.life.presentation

import com.myxoz.life.presentation.DigSocPlatformFormatting.Companion.formattable
import com.myxoz.life.presentation.EventTagFormatting.Companion.formattable
import com.myxoz.life.presentation.TimewastePlatformFormatting.Companion.formattable
import com.myxoz.life.presentation.VehicleFormatting.Companion.formattable
import com.myxoz.life.storage.events.additionals.DigSocPlatform
import com.myxoz.life.storage.events.additionals.EventTag
import com.myxoz.life.storage.events.additionals.TagLike
import com.myxoz.life.storage.events.additionals.TimewastePlatform
import com.myxoz.life.storage.events.additionals.Vehicle

fun TagLike.asFormattable(): DisplayFormattable =
    when(this) {
        is DigSocPlatform -> formattable()
        is EventTag -> formattable()
        is TimewastePlatform -> formattable()
        is Vehicle -> formattable()
    }