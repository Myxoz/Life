package com.myxoz.life.background.permissions

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.myxoz.life.background.sensors.StepsService
import com.myxoz.life.lifecore.settings.Service
import com.myxoz.life.lifecore.settings.ServiceController

class ActualServiceController: ServiceController {
    override fun enable(context: Context, service: Service) {
        ContextCompat.startForegroundService(context, Intent(context, StepsService::class.java))
    }

    override fun disable(context: Context, service: Service) {
        ContextCompat.startForegroundService(context, Intent(context, StepsService::class.java).apply {
            putExtra("stop", true)
        })
    }
}