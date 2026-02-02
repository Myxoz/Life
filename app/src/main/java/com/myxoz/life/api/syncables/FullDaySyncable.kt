package com.myxoz.life.api.syncables

import com.myxoz.life.api.ServerSyncableCompanion
import com.myxoz.life.api.Syncable
import com.myxoz.life.dbwrapper.DayScreenTimeEntity
import com.myxoz.life.dbwrapper.days.DaysEntity
import com.myxoz.life.api.API
import com.myxoz.life.utils.jsonObjArray
import org.json.JSONArray
import org.json.JSONObject

class FullDaySyncable(
    val happyness: Int,
    val stress: Int,
    val successfulness: Int,
    val steps: Int,
    val screenTimeMs: Int,
    val topApps: List<DayScreenTimeEntity>,
    id: Long,
) : Syncable(SpecialSyncablesIds.DAYS, id) {
    override suspend fun specificsToJson(): JSONObject? {
        return JSONObject()
            .put("type", 1)
            .put("id", id.toString())
            .put("stress", stress)
            .put("successfulness", successfulness)
            .put("happyness", happyness)
            .put("screen_time_ms", screenTimeMs)
            .put("steps", steps)
            .put(
                "top",
                JSONArray().apply {
                    for (item in topApps) {
                        put(
                            JSONObject()
                                .put("duration", item.duration)
                                .put("packagename", item.packagename)
                        )
                    }
                }
            )
    }

    override suspend fun saveToDB(db: API.WriteSyncableDaos) {
        db.daysDao.insertDay(
            DaysEntity(
                id.toInt(),
                screenTimeMs,
                steps,
                happyness,
                stress,
                successfulness,
            )
        )
        db.daysDao.insertAllDayScreenTime(topApps)
    }

    companion object : ServerSyncableCompanion<FullDaySyncable> {
        override fun fromJSON(json: JSONObject): FullDaySyncable {
            val date = json.getString("date").toInt()
            val screenTimeMs = json.getString("screen_time_ms").toInt()
            val steps = json.getString("steps").toInt()
            val happyness = json.getString("happyness").toInt()
            val stress = json.getString("stress").toInt()
            val successfulness = json.getString("successfulness").toInt()
            val topApps = json.getJSONArray("top").jsonObjArray.map {
                DayScreenTimeEntity(
                    it.getString("date").toLong(),
                    it.getString("packagename"),
                    it.getString("duration_ms").toLong(),
                )
            }
            return FullDaySyncable(
                happyness,
                stress,
                successfulness,
                steps,
                screenTimeMs,
                topApps,
                date.toLong()
            )
        }

        override suspend fun fromDB(db: API.ReadSyncableDaos, id: Long): FullDaySyncable? {
            val day = db.daysDao.getDay(id)?:return null
            val topApps = db.daysDao.getScreenTimesByDay(id)
            return FullDaySyncable(
                day.happyness,
                day.stress,
                day.successfulness,
                day.steps,
                day.screenTimeMs,
                topApps,
                id
            )
        }
    }
}