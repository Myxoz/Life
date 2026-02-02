package com.myxoz.life.android.integration

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import com.myxoz.life.R
import com.myxoz.life.api.syncables.LocationSyncable
import com.myxoz.life.utils.getJSONObjectOrNull
import com.myxoz.life.utils.getStringOrNull
import com.myxoz.life.utils.jsonObjArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
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

            val url = URL(fullUrl.also { Log.d("MapBoxAPI","Debug: fetches mapbox at $it") })
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("accept", "*/*")
            }

            connection.inputStream.bufferedReader().use { it.readText() }.apply {
                connection.disconnect()
            }
        } catch (_: UnknownHostException) {
            Log.d("MapBoxAPI","Offline")
            null
        } catch (e: Exception) {
            Log.d("MapBoxAPI","Exception in try: $e")
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
    ) = getWithMapboxToken(context, "https://api.mapbox.com/search/geocode/v6/reverse?latitude=$lat&longitude=$longitude").also { Log.w("MapBoxAPI","Mapbox reverse geocode: $it") }

    fun getLocationListFromAPIResponse(response: String): List<LocationSyncable>{
        val features = JSONObject(response)
            .getJSONArray("features")
            .jsonObjArray
        if(features.isEmpty()) return listOf()
        return features.mapNotNull {
            val searchedItem = it.getJSONObjectOrNull("properties")?:return@mapNotNull null
            val details = searchedItem.getJSONObjectOrNull("context")?:return@mapNotNull null
            LocationSyncable(
                searchedItem.getStringOrNull("name_preferred") ?: "Name",
                searchedItem.getJSONObjectOrNull("coordinates")?.getDouble("longitude")?:return@mapNotNull null,
                searchedItem.getJSONObjectOrNull("coordinates")?.getDouble("latitude")?:return@mapNotNull null,
                0,
                null,
                details.getJSONObjectOrNull("street")?.getStringOrNull("name"),
                details.getJSONObjectOrNull("address")
                    ?.getStringOrNull("address_number"),
                details.getJSONObjectOrNull("place")?.getStringOrNull("name"),
                details.getJSONObjectOrNull("country")?.getStringOrNull("name")
                    ?: "Germany",
                -1L,
            )
        }
    }
}