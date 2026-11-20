package com.myxoz.life.background

import kotlinx.coroutines.flow.MutableSharedFlow

interface PushApplicationState {
    val channel: MutableSharedFlow<Any?>
}