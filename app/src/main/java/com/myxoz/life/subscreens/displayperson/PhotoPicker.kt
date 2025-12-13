package com.myxoz.life.subscreens.displayperson

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.core.graphics.scale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream

class PhotoPicker {
    private val _photoURI = MutableStateFlow<Uri?>(null)
    val photoURI  = _photoURI.asStateFlow()
    private val _processedBitmap = MutableStateFlow<Bitmap?>(null)
    val processedBitmap = _processedBitmap.asStateFlow()
    var pickerLauncher: ActivityResultLauncher<Intent>? = null

    fun pickPhoto(){
        val galleryIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickerLauncher?.launch(galleryIntent)
    }

    fun setURI(uri: Uri, context: Context){
        _photoURI.value = uri
        processImage(uri, context)
    }

    private fun processImage(uri: Uri, context: Context) {
        try {
            val originalBitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            val croppedBitmap = cropToSquare(originalBitmap)
            val resizedBitmap = croppedBitmap.scale(480, 480)
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            val finalBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            _processedBitmap.value = finalBitmap
            if (croppedBitmap != originalBitmap) croppedBitmap.recycle()
            if (resizedBitmap != finalBitmap) resizedBitmap.recycle()
            if (originalBitmap != finalBitmap) originalBitmap.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cropToSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val size = minOf(width, height)
        return Bitmap.createBitmap(bitmap, (width - size) / 2, (height - size) / 2, size, size)
    }
}