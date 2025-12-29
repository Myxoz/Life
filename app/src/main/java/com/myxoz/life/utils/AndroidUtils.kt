package com.myxoz.life.utils

import android.content.Context
import android.content.Intent
import androidx.annotation.MainThread
import androidx.core.net.toUri

object AndroidUtils {
    @MainThread
    fun openLink(context: Context, link: String) {
        println("Opening link: $link")
        val uri = link.toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}
