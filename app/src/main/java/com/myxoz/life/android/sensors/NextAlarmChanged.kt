package com.myxoz.life.android.sensors

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.myxoz.life.repositories.MainApplication

class NextAlarmChanged: BroadcastReceiver() {
    // It's not that deep
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context?, intent: Intent?) {
        val repo = (context as? MainApplication  ?: return).repositories
        repo.calendarRepo.refetchAlarmClockTs()
    }
}