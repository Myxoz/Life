package com.myxoz.life.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.core.content.edit
import com.myxoz.life.screens.feed.fullscreenevent.getEventId
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.viewmodels.Settings
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

@Stable
class API(
    val context: Context,
    private val db: StorageManager,
    private val prefs: SharedPreferences,
    private val internet: Settings.Features.Feature
) {
    var lastUpdate = prefs.getLong("last_update", 0)
    var isSyncing = false
    val security = Security()
    suspend fun resyncLastDays(): Int {
        if (!internet.has.value || isSyncing) return 0
        isSyncing = true
        val elementsPerRequest = 50
        var offset = 0
        println("Sending entries to server...")
        while (true) {
            val lastNEntries = buildList {
                for (entry in db.waitingSync.getLastNWaitingSyncEntries(
                    elementsPerRequest,
                    offset
                )) {
                    try {
                        val x = Syncable.from(entry, db)
                        x?.let { add(it) }
                    } catch (e: Exception) {
                        Log.e("API", "Failed to create Syncable from $entry", e)
                    }
                }
            }
            if (lastNEntries.isEmpty()) {
                println("None to send")
                break
            }

            val json = JSONArray()
            for (entry in lastNEntries) {
                entry.toJson(db)?.also { json.put(it) }
                    ?: println("Couldnt stringify ${entry.id} of type ${entry.calendarId} ($entry)")
            }

            println("JSON:\n" + json.toString(2))
            val response = send(Method.Send, json.toString(), offset)
            println("Server responded:\n$response\n")
            if (response == null) {
                isSyncing = false
                return 0
            }
            val resJson = JSONObject(response)
            resJson
                .getJSONArray("msg")
                .forEach { println("Server reported: $it") }
            resJson.getJSONArray("entries").jsonObjArray.forEach {
                db.waitingSync.deleteWaitingSync(it.getEventId(), it.getInt("type"))
            }
            if (lastNEntries.size < elementsPerRequest) break
            println("More to go")
        }
        var serverLastUpdate: Long
        var updateAmount = 0
        println("Get new entries...")
        while (true) {
            val responseText = send(Method.Resync, "[]", offset)
            if (responseText == null) {
                isSyncing = false; return 0
            }
            if (!responseText.trim().startsWith("{")) {
                println("API Unexpected response: $responseText")
                println("${System.currentTimeMillis()} is the current time")
                isSyncing = false
                return 0
            }
            val response = JSONObject(responseText)
            println("Server entries:\n" + response.toString(2))
            val jsonArray = response.getJSONArray("e").jsonObjArray
            serverLastUpdate = response.getLong("date")
            if (jsonArray.isEmpty()) break
            updateAmount += jsonArray.size
            Log.i("API", "Batch received ${jsonArray.size} events")
            jsonArray.forEach {
                println(it)
                ServerSyncable.overwriteByJson(db, it)
            }
            if (jsonArray.size < elementsPerRequest) break
            offset += elementsPerRequest
        }
        lastUpdate = serverLastUpdate
        prefs.edit {
            putLong("last_update", lastUpdate)
        }
        isSyncing = false
        return updateAmount
    }

    suspend fun send(method: Method, data: String, offset: Int?): String? = security.send(
        "https://myxoz.de/life/_api.php",
        lastUpdate,
        System.currentTimeMillis(),
        data,
        method,
        offset
    )

    suspend fun testSign() = send(
        Method.Verify,
        "[]",
        0
    ) == "1"

    enum class Method(val method: String) {
        Resync("r"),
        Send("s"),
        Verify("v")
    }

    companion object {
        fun generateId(): Long {
            return Random.nextLong(0, Long.MAX_VALUE)
        }
    }
}

val JSONArray.jsonObjArray: List<JSONObject>
    get() = this.let {
        val retList = mutableListOf<JSONObject>()
        for (i in 0..<it.length()) {
            retList.add(it.getJSONObject(i))
        }
        retList
    }

inline fun JSONArray.forEach(function: (Any) -> Unit) {
    for (item in 0..<length()) {
        function(this.get(item))
    }
}