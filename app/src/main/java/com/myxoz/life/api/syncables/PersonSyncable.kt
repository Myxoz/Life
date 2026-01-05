package com.myxoz.life.api.syncables

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import com.myxoz.life.R
import com.myxoz.life.api.ServerSyncableCompanion
import com.myxoz.life.api.Syncable
import com.myxoz.life.api.forEach
import com.myxoz.life.screens.feed.fullscreenevent.getEventId
import com.myxoz.life.dbwrapper.PersonEntity
import com.myxoz.life.dbwrapper.SocialsEntity
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.utils.AndroidUtils
import com.myxoz.life.utils.getLongOrNull
import com.myxoz.life.utils.getStringOrNull
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
    override suspend fun saveToDB(db: StorageManager) {
        db.people.insertPerson(
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
        db.socials.removeAllFromPerson(id)
        socials.forEach {
            db.socials.insertSocial(
                SocialsEntity(id, it.platform.id, it.handle)
            )
        }
    }

    override suspend fun specificsToJson(db: StorageManager): JSONObject = JSONObject()
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

    companion object : ServerSyncableCompanion {
        override suspend fun overwriteByJson(db: StorageManager, it: JSONObject) {
            val id = it.getEventId()
            db.people.insertPerson(
                PersonEntity(
                    id,
                    it.getString("name"),
                    it.getStringOrNull("fullname"),
                    it.getStringOrNull("phone"),
                    it.getStringOrNull("iban"),
                    it.getLongOrNull("home"),
                    it.getLongOrNull("birthday"),
                )
            )
            db.socials.removeAllFromPerson(id)
            it.getJSONArray("socials").forEach {
                db.socials.insertSocial(
                    SocialsEntity(
                        id,
                        (it as JSONObject).getInt("platform"),
                        it.getString("handle")
                    )
                )
            }
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
                    .apply { if (this == null) println("Couldnt find social with id $id in Platforms.getById") }

                fun getByName(name: String): Platform? =
                    Platform.entries.firstOrNull { it.short == name }
                        .apply { if (this == null) println("Couldnt find social with name $name in Platforms.getById") }
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

        suspend fun from(db: StorageManager, dbEntry: PersonEntity): PersonSyncable {
            val socials = db.socials.getSocialsFromPerson(dbEntry.id)
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

