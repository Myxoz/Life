package com.myxoz.life.background.sensors

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.myxoz.life.storage.DatabaseInterfacable

class NextAlarmChanged: BroadcastReceiver() {
    // It's not that deep
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context?, intent: Intent?) {
        val repo = (context as? DatabaseInterfacable ?: return).dbInterface
        repo.calendarInterface.refetchAlarmClockTs()
    }
}