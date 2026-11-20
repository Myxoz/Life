package com.myxoz.life.background.sensors

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.myxoz.life.background.PushApplicationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.EmptyCoroutineContext

class ShareReceiverActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoroutineScope(EmptyCoroutineContext).launch {
            if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrEmpty()) {
                    // Launch MainActivity with navigation intent
                    withContext(Dispatchers.Main) {
                        (applicationContext as PushApplicationState).channel.tryEmit(
                            SharedRouteParser.from(sharedText, applicationContext) ?: return@withContext
                        )
                    }
                }
            }
            finish()
        }
    }
}