package com.myxoz.life.lifecore.instantevents

import android.content.Intent

abstract class InstantEventOpener{
    abstract fun openRoute(any: Any)
    abstract fun openIntent(intent: Intent)
}
