package com.myxoz.life.lifecore

import kotlin.time.Duration

interface AbstractCrossScreenNavigation {
    fun navigateToProfileInfo(minPastToIncludeInStats: Duration)
}