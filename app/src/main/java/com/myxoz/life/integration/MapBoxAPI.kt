package com.myxoz.life.integration

import android.content.Context
import androidx.annotation.StringRes
import com.myxoz.life.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException

object MapBoxAPI {
    suspend fun getWithMapboxToken(
        context: Context,
        baseUrl: String,
        @StringRes tokenResId: Int = R.string.mapbox_access_token
    ): String? = withContext(Dispatchers.IO) {
        try {
            val token = context.getString(tokenResId)
            val fullUrl = "$baseUrl&access_token=$token"

            val url = URL(fullUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("accept", "*/*")
            }

            connection.inputStream.bufferedReader().use { it.readText() }.apply {
                connection.disconnect()
            }
        } catch (_: UnknownHostException) {
            println("Offline")
            null
        } catch (e: Exception) {
            println("Exception in try: $e")
            e.printStackTrace()
            null
        }
    }

    suspend fun forwardGeocode(
        context: Context,
        query: String
    ): String? {
        val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8.name())
        val baseUrl = "https://api.mapbox.com/search/geocode/v6/forward?q=$encodedQuery"
        return getWithMapboxToken(context, baseUrl)
    }

    suspend fun reverseGeocode(
        context: Context,
        lat: Double,
        longitude: Double
    ) = getWithMapboxToken(context, "https://api.mapbox.com/search/geocode/v6/reverse?latitude=$lat&longitude=$longitude").also { println("Mapbox reverse geocode: $it") }
}