package com.myxoz.life.api

import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.dbwrapper.DayScreenTimeEntity
import com.myxoz.life.dbwrapper.DaysEntity
import org.json.JSONArray
import org.json.JSONObject

class FullDaySyncable(
    val happyness: Int,
    val stress: Int,
    val successfulness: Int,
    val steps: Int,
    val screenTimeMs: Int,
    id: Long,
) : Syncable(SpecialSyncablesIds.DAYS, id) {
    override suspend fun specificsToJson(db: StorageManager): JSONObject? {
        val topApps = db.dayScreenTime.getScreenTimesByDay(id)
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

    override suspend fun saveToDB(db: StorageManager) {
        db.days.insertDay(
            DaysEntity(
                id.toInt(),
                screenTimeMs,
                steps,
                happyness,
                stress,
                successfulness,
            )
        )
    }

    companion object : ServerSyncableCompanion {
        override suspend fun overwriteByJson(db: StorageManager, it: JSONObject) {
            db.days.insertDay(
                DaysEntity(
                    it.getString("date").toInt(),
                    it.getString("screen_time_ms").toInt(),
                    it.getString("steps").toInt(),
                    it.getString("happyness").toInt(),
                    it.getString("stress").toInt(),
                    it.getString("successfulness").toInt(),
                )
            )
            it.getJSONArray("top").jsonObjArray.forEach {
                db.dayScreenTime.insertDayScreenTime(
                    DayScreenTimeEntity(
                        it.getString("date").toLong(),
                        it.getString("packagename"),
                        it.getString("duration_ms").toLong(),
                    )
                )
            }
        }
    }
}