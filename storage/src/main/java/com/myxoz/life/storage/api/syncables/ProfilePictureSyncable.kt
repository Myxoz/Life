package com.myxoz.life.storage.api.syncables

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class ProfilePictureSyncable(
    override val id: Long,
    val bitmapBase64: String?,
): Syncable {
    @OptIn(ExperimentalEncodingApi::class)
    companion object {
        fun base64ToBitmap(base64String: String): Bitmap? {
            return try {
                val decodedBytes = Base64.decode(base64String)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                Log.e("ImagePicker","Failed to decode bitmap from Base64", e)
                null
            }
        }
        internal fun saveBase64ToFile(context: Context, base64Str: String, personId: Long): File? {
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
        internal fun deleteBitmapFile(context: Context, personId: Long): Boolean {
            return try {
                getProfilePicPath(context, personId).delete()
            } catch (e: Exception) {
                Log.e("ImagePicker","Failed to delete Bitmap for $personId", e)
                false
            }
        }
        private fun getProfilePicPath(context: Context, personId: Long): File {
            val directory = File(context.filesDir, SyncableIds.PROFILEPICTURE.toString())
            if (!directory.exists()) { directory.mkdirs() }
            return File(directory, "$personId.jpg")
        }

        internal fun loadBase64ByPerson(context: Context, personId: Long): String? {
            return try {
                val file = getProfilePicPath(context, personId)
                if (file.exists()) Base64.encode(file.readBytes()) else null
            } catch (e: Exception) {
                Log.e("ImagePicker","Failed to decode existing Bitmap", e)
                null
            }
        }

        internal fun bitmapToBase64(bitmap: Bitmap): String {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()
            return Base64.encode(bytes)
        }
    }
}