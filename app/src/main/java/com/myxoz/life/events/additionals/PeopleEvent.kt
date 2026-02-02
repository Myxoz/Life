package com.myxoz.life.events.additionals

import com.myxoz.life.dbwrapper.events.PeopleMappingEntity
import com.myxoz.life.dbwrapper.events.WriteEventDetailsDao
import com.myxoz.life.utils.forEach
import org.json.JSONArray
import org.json.JSONObject

interface PeopleEvent {
    val people: List<Long>
    suspend fun savePeopleMapping(db: WriteEventDetailsDao, eventId: Long, people: List<Long>) {
        db.insertAllPeopleMappings(
            people.map {
                PeopleMappingEntity(
                    eventId,
                    it
                )
            }
        )
    }
    fun JSONObject.addPeople(): JSONObject = put("people", JSONArray().apply { people.forEach { put(it.toString()) } })
    companion object {
        fun getPeopleFromJson(jsonObject: JSONObject) = jsonObject.getJSONArray("people").run {
            val list = mutableListOf<Long>()
            forEach {
                list.add((it as String).toLong())
            }
            list
        }
    }
}
