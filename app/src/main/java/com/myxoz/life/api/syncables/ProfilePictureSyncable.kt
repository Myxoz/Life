package com.myxoz.life.api.syncables

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.myxoz.life.MainActivity
import com.myxoz.life.api.API
import com.myxoz.life.api.ServerSyncableCompanion
import com.myxoz.life.api.Syncable
import com.myxoz.life.dbwrapper.people.ProfilePictureStored
import com.myxoz.life.dbwrapper.people.ReadPeopleDao
import com.myxoz.life.screens.feed.fullscreenevent.getId
import com.myxoz.life.utils.getStringOrNull
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class ProfilePictureSyncable(
    val personId: Long,
    val bitmapBase64: String?,
): Syncable(SpecialSyncablesIds.PROFILEPICTURE, personId) {
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun specificsToJson(): JSONObject? {
        if (bitmapBase64 == null) return JSONObject().put("has_pp", false)

        return JSONObject()
            .put("has_pp", true)
            .put("content", bitmapBase64)
    }

    override suspend fun saveToDB(db: API.WriteSyncableDaos) {
        try {
            if (bitmapBase64 != null) {
                val context = MainActivity.getAppContext()
                val savedFile = saveBase64ToFile(context, bitmapBase64, personId)

                if (savedFile != null) {
                    Log.w("Calendar","Saved ProfilePic Bitmap to ${savedFile.path}")
                }
                db.peopleDao.insertProfilePicture(
                    ProfilePictureStored(
                        personId,
                        true
                    )
                )
            } else {
                val context = MainActivity.getAppContext()
                deleteBitmapFile(context, personId)

                db.peopleDao.insertProfilePicture(
                    ProfilePictureStored(
                        personId = personId,
                        hasPP = false
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("ImagePicker","Failed to overwrite Bitmap from ServerResponse", e)
        }
    }
    @OptIn(ExperimentalEncodingApi::class)
    companion object: ServerSyncableCompanion<ProfilePictureSyncable> {
        fun base64ToBitmap(base64String: String): Bitmap? {
            return try {
                val decodedBytes = Base64.decode(base64String)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                Log.e("ImagePicker","Failed to decode bitmap from Base64", e)
                null
            }
        }
        fun saveBase64ToFile(context: Context, base64Str: String, personId: Long): File? {
            try {
                val file = getProfilePicPath(context, personId)
                val fos = FileOutputStream(file)
                fos.write(Base64.decode(base64Str))
                fos.flush(); fos.close()
                Log.i("ImagePicker","Successfully saved base64 bitmap to ${file.path}")
                return file
            } catch (e: Exception) {
                Log.e("ImagePicker","Failed to save base64 bitmap for ID $personId", e)
                return null
            }
        }
        fun deleteBitmapFile(context: Context, personId: Long): Boolean {
            return try {
                getProfilePicPath(context, personId).delete()
            } catch (e: Exception) {
                Log.e("ImagePicker","Failed to delete Bitmap for $personId", e)
                false
            }
        }
        fun getProfilePicPath(context: Context, personId: Long): File {
            val directory = File(context.filesDir, SpecialSyncablesIds.PROFILEPICTURE.toString())
            if (!directory.exists()) { directory.mkdirs() }
            return File(directory, "$personId.jpg")
        }

        fun loadBase64ByPerson(context: Context, personId: Long): String? {
            return try {
                val file = getProfilePicPath(context, personId)
                if (file.exists()) Base64.encode(file.readBytes()) else null
            } catch (e: Exception) {
                Log.e("ImagePicker","Failed to decode existing Bitmap", e)
                null
            }
        }

        override fun fromJSON(json: JSONObject): ProfilePictureSyncable =
            ProfilePictureSyncable(
                json.getId(),
                json.getStringOrNull("content")
            )

        suspend fun getSyncable(id: Long, context: Context, db: ReadPeopleDao): ProfilePictureSyncable {
            val pp = db.getPPById(id)
            if(pp == null || !pp.hasPP) {
                return ProfilePictureSyncable(id, null)
            }
            return ProfilePictureSyncable(id, loadBase64ByPerson(context, id))
        }

        fun bitmapToBase64(bitmap: Bitmap): String? {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()
            return Base64.encode(bytes)
        }
    }
}