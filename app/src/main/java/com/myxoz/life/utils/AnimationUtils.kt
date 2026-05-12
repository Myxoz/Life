package com.myxoz.life.utils

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times

inline fun between(progress: Float, from: Dp, to: Dp) = progress * (to - from) + from