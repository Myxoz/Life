package com.myxoz.life.lifecore.settings

import android.content.Context

interface ServiceController {
    fun enable(context: Context, service: Service)
    fun disable(context: Context, service: Service)
}