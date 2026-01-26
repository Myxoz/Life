package com.myxoz.life.api.syncables

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.myxoz.life.MainActivity
import com.myxoz.life.api.ServerSyncableCompanion
import com.myxoz.life.api.Syncable
import com.myxoz.life.dbwrapper.ProfilePictureStored
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.screens.feed.fullscreenevent.getEventId
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class ProfilePictureSyncable(val personId: Long): Syncable(SpecialSyncablesIds.PROFILEPICTURE, personId) {
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun specificsToJson(db: StorageManager): JSONObject? {
        val entry = db.profilePictureDao.getPPById(personId) ?: return null
        if (!entry.hasPP) return JSONObject().put("has_pp", false)

        val context = MainActivity.getAppContext()
        val imageFile = getProfilePicPath(context, personId)
        if (!imageFile.exists()) return null
        val imageBytes = imageFile.readBytes()
        val base64 = Base64.encode(imageBytes)

        return JSONObject()
            .put("has_pp", true)
            .put("content", base64)
    }

    override suspend fun saveToDB(db: StorageManager) {
        throw Error("Do not use this Method ProfilePictureSyncable.saveToDB")
    }
    @OptIn(ExperimentalEncodingApi::class)
    companion object: ServerSyncableCompanion {
        override suspend fun overwriteByJson(
            db: StorageManager,
            it: JSONObject
        ) {
            try {
                val personId = it.getEventId()
                val hasPP = it.getString("has_pp") == "1"

                if (hasPP && it.has("content")) {
                    val base64Content = it.getString("content")
                    val bitmap = base64ToBitmap(base64Content)

                    if (bitmap != null) {
                        val context = MainActivity.getAppContext()
                        val savedFile = saveBitmapToFile(context, bitmap, personId)

                        if (savedFile != null) {
                            Log.w("Calendar","Saved ProfilePic Bitmap to ${savedFile.path}")
                        }
                        bitmap.recycle()
                    }
                    db.profilePictureDao.insertProfilePicture(
                        ProfilePictureStored(
                            personId,
                            true
                        )
                    )
                } else {
                    val context = MainActivity.getAppContext()
                    deleteBitmapFile(context, personId)

                    db.profilePictureDao.insertProfilePicture(
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
        fun base64ToBitmap(base64String: String): Bitmap? {
            return try {
                val decodedBytes = Base64.decode(base64String)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                Log.e("ImagePicker","Failed to decode bitmap from Base64", e)
                null
            }
        }
        fun saveBitmapToFile(context: Context, bitmap: Bitmap, personId: Long): File? {
            try {
                val file = getProfilePicPath(context, personId)
                val fos = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
                fos.flush(); fos.close()
                Log.i("ImagePicker","Successfully saved Bitmap to ${file.path}")
                return file

            } catch (e: Exception) {
                Log.e("ImagePicker","Failed to save bitmap for ID $personId", e)
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

        fun loadBitmapByPerson(context: Context, personId: Long): Bitmap? {
            return try {
                val file = getProfilePicPath(context, personId)
                if (file.exists()) { BitmapFactory.decodeFile(file.absolutePath) } else null
            } catch (e: Exception) {
                Log.e("ImagePicker","Failed to decode existing Bitmap", e)
                null
            }
        }
    }
}