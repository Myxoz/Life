package com.myxoz.life.api.syncables

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.myxoz.life.R
import com.myxoz.life.api.ServerSyncableCompanion
import com.myxoz.life.api.Syncable
import com.myxoz.life.dbwrapper.people.PersonEntity
import com.myxoz.life.dbwrapper.people.ReadPeopleDao
import com.myxoz.life.dbwrapper.people.SocialsEntity
import com.myxoz.life.api.API
import com.myxoz.life.screens.feed.fullscreenevent.getId
import com.myxoz.life.utils.AndroidUtils
import com.myxoz.life.utils.getLongOrNull
import com.myxoz.life.utils.getStringOrNull
import com.myxoz.life.utils.jsonObjArray
import org.json.JSONArray
import org.json.JSONObject

class PersonSyncable(
    id: Long,
    val name: String,
    val fullName: String?,
    val phoneNumber: String?,
    val iban: String?,
    val home: Long?,
    val birthday: Long?,
    val socials: List<Socials>
) : Syncable(SpecialSyncablesIds.PEOPLE, id) {
    override suspend fun saveToDB(db: API.WriteSyncableDaos) {
        db.peopleDao.insertPerson(
            PersonEntity(
                id,
                name,
                fullName,
                phoneNumber,
                iban,
                home,
                birthday,
            )
        )
        db.peopleDao.removeAllSocialsFor(id)
        db.peopleDao.insertAllSocials(
            socials.map {
                SocialsEntity(id, it.platform.id, it.handle)
            }
        )
    }

    fun copy(
        name: String = this.name,
        fullName: String? = this.fullName,
        phoneNumber: String? = this.phoneNumber,
        iban: String? = this.iban,
        home: Long? = this.home,
        birthday: Long? = this.birthday,
        socials: List<Socials> = this.socials
    ) = PersonSyncable(id, name, fullName, phoneNumber, iban, home, birthday, socials)

    override suspend fun specificsToJson(): JSONObject? = JSONObject()
        .put("name", name)
        .put("fullname", fullName ?: JSONObject.NULL)
        .put("phone", phoneNumber ?: JSONObject.NULL)
        .put("iban", iban ?: JSONObject.NULL)
        .put("home", home?.toString() ?: JSONObject.NULL)
        .put("birthday", birthday?.toString() ?: JSONObject.NULL)
        .put(
            "socials",
            JSONArray().apply {
                socials.forEach {
                    put(
                        JSONObject()
                            .put("platform", it.platform.id)
                            .put("handle", it.handle)
                    )
                }
            }
        )

    companion object : ServerSyncableCompanion<PersonSyncable> {
        override fun fromJSON(json: JSONObject): PersonSyncable {
            val id = json.getId()
            val name = json.getString("name")
            val fullName = json.getStringOrNull("fullname")
            val phone = json.getStringOrNull("phone")
            val iban = json.getStringOrNull("iban")
            val home = json.getLongOrNull("home")
            val birthday = json.getLongOrNull("birthday")
            val platforms = json.getJSONArray("socials").jsonObjArray.map {
                SocialsEntity(
                    id,
                    it.getInt("platform"),
                    it.getString("handle")
                )
            }
            return PersonSyncable(
                id, name, fullName, phone, iban, home, birthday, platforms.mapNotNull {
                    Socials.from(it)
                }
            )
        }

        enum class Platform(
            val id: Int,
            val fullName: String,
            val short: String,
            val icon: Int,
            val color: Color,
            val priority: Int,
            private val openApp: PlatformOpener.() -> Unit
        ) {
            Instagram(
                1,
                "Instagram",
                "insta",
                R.drawable.insta,
                Color(0xFFE1306C),
                0,
                { web("https://www.instagram.com/$handle") }
            ),
            Snapchat(
                2,
                "Snapchat",
                "snap",
                R.drawable.snap,
                Color(0xFFFFFC00),
                1,
                { web("https://www.snapchat.com/add/$handle") }
            ),
            WhatsApp(
                3,
                "WhatsApp",
                "wa",
                R.drawable.whatsapp,
                Color(0xFF25D366),
                9,
                {
                    intent(
                        ComponentName("com.whatsapp", "com.whatsapp.Conversation"),
                        listOf(
                            "jid" to (phoneNumber ?: "").replace(
                                "\\D".toRegex(),
                                ""
                            ) + "@s.whatsapp.net"
                        )
                    )
                }
            ),
            Youtube(
                4,
                "YouTube",
                "youtube",
                R.drawable.yt,
                Color(0xFFFF0000),
                -1,
                { web("https://youtube.com/@$handle") }
            ),
            Signal(
                5,
                "Signal",
                "signal",
                R.drawable.signal,
                Color(0xFF3b45fd),
                10,
                { web("https://signal.me/#p/$phoneNumber") }
            ),
            Telegram(
                6,
                "Telegram",
                "telegram",
                R.drawable.telegram,
                Color(0xFF2AABEE),
                2,
                { web(if (handle != Telegram.fullName) "https://t.me/$handle" else "https://t.me/$phoneNumber") }
            )
            ;

            fun openPlatform(context: Context, handle: String, phoneNumber: String?) = PlatformOpener(context, handle, phoneNumber).openApp()

            private class PlatformOpener(
                val context: Context,
                val handle: String,
                val phoneNumber: String?
            ) {
                fun web(url: String) {
                    AndroidUtils.openLink(context, url)
                }

                fun intent(componentName: ComponentName, extras: List<Pair<String, String>>) {
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.component = componentName
                    extras.forEach { intent.putExtra(it.first, it.second) }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }

            companion object {
                fun getById(id: Int): Platform? = Platform.entries.firstOrNull { it.id == id }
                    .apply { if (this == null) Log.w("Person","Couldnt find social with id $id in Platforms.getById") }

                fun getByName(name: String): Platform? =
                    Platform.entries.firstOrNull { it.short == name }
                        .apply { if (this == null) Log.w("Person","Couldnt find social with name $name in Platforms.getById") }
            }
        }

        data class Socials(val platform: Platform, val handle: String) {
            fun asString() = "${platform.short}@$handle"

            companion object {
                fun from(it: SocialsEntity): Socials? {
                    return Socials(Platform.getById(it.platform) ?: return null, it.handle)
                }

                fun from(it: String): Socials? {
                    if (!it.contains("@")) return null
                    return Socials(
                        Platform.getByName(it.substringBefore("@")) ?: return null,
                        it.substringAfter("@")
                    )
                }
            }
        }

        suspend fun from(db: ReadPeopleDao, dbEntry: PersonEntity): PersonSyncable {
            val socials = db.getSocialsByPerson(dbEntry.id)
            return PersonSyncable(
                dbEntry.id,
                dbEntry.name,
                dbEntry.fullname,
                dbEntry.phoneNumber,
                dbEntry.iban,
                dbEntry.home,
                dbEntry.birthday,
                getOrderedSocials(socials.mapNotNull {
                    Socials.from(it)
                })
            )
        }

        fun getOrderedSocials(socials: List<Socials>) =
            socials.sortedByDescending { it.platform.priority }
    }
}

