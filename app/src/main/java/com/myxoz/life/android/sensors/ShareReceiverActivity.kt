package com.myxoz.life.android.sensors

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.myxoz.life.MainActivity
import com.myxoz.life.repositories.MainApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

class ShareReceiverActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoroutineScope(EmptyCoroutineContext).launch {
            if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrEmpty()) {
                    val travelEvent = SharedRouteParser.from(sharedText, applicationContext, (applicationContext as MainApplication).repositories.locationRepo) ?: return@launch

                    // Launch MainActivity with navigation intent
                    with(Dispatchers.Main){
                        val mainIntent = Intent(applicationContext, MainActivity::class.java).apply {
                            putExtra("targetRoute", "fullscreen_event")
                            putExtra("shared_travel_event", travelEvent.toJson().toString())
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(mainIntent)
                    }
                }
            }
            finish()
        }
    }
}