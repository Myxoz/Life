package com.myxoz.life.events.additionals

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.api.forEach
import com.myxoz.life.dbwrapper.PeopleMappingEntity
import com.myxoz.life.dbwrapper.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface PeopleEvent {
    val people: List<Long>
    suspend fun savePeopleMapping(db: StorageManager, eventId: Long, people: List<Long>) {
        db.peopleMapping.insertAll(
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
        @Composable
        fun GetFullNames(db: StorageManager, list: List<Long>, onDone: (List<PersonSyncable>)->Unit){
            LaunchedEffect(list) {
                withContext(Dispatchers.IO){
                    onDone(list
                        .mapNotNull { db.people.getPersonById(it) }
                        .map { PersonSyncable.from(db, it) }
                    )
                }
            }
        }
        fun getPeopleFromJson(jsonObject: JSONObject) = jsonObject.getJSONArray("people").run {
            val list = mutableListOf<Long>()
            forEach {
                list.add((it as String).toLong())
            }
            list
        }
    }
}
