package com.myxoz.life.android.livenotification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.myxoz.life.android.MainApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LiveEventRefreshReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                (context.applicationContext as MainApplication)
                    .liveRescheduling
                    .launchRefresh()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
