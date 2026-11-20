package com.myxoz.life.presentation.notifications

import androidx.annotation.DrawableRes
import com.myxoz.life.presentation.R

object StepsServiceRepresentation {
    @DrawableRes
    val NOTIFICATIONICON: Int = R.drawable.walk
    const val NOTIFICATIONTITLE = "Step tracking active"
    const val CHANNELNAME = "Step tracking"
    const val NOTIFICATIONCONTENT = "Monitoring your steps in background"
}