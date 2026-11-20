package com.myxoz.life.background.livenotification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.myxoz.life.background.ContainsLifeRescheduling
import com.myxoz.life.background.livenotification.LiveRescheduling.Companion.LOGTAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LiveEventRefreshReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(LOGTAG, "Received LiveRescheduling")
                (context.applicationContext as? ContainsLifeRescheduling)
                    ?.liveRescheduling
                    ?.launchRefresh()
                    ?: Log.d(LOGTAG, "Cannot acess LiveRescheduling")

            } finally {
                pendingResult.finish()
            }
        }
    }
}
