package com.myxoz.life.android.livenotification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.myxoz.life.android.MainApplication
import com.myxoz.life.android.livenotification.LiveRescheduling.Companion.LOGTAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LiveEventRefreshReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(LOGTAG, "Received LiveRescheduling")
                (context.applicationContext as? MainApplication)
                    ?.liveRescheduling
                    ?.launchRefresh()
                    ?: Log.d(LOGTAG, "Cannot acess LiveRescheduling")

            } finally {
                pendingResult.finish()
            }
        }
    }
}
