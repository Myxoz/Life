package com.myxoz.life.lifecore.android.image

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.flow.MutableStateFlow

class ImagePicker(componentActivity: ComponentActivity) {
    val bitmap = MutableStateFlow<Bitmap?>(null)
    private var pickerLauncher: ActivityResultLauncher<Intent>? =
        componentActivity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedImageUri: Uri? = result.data?.data
                Log.d("ImagePicker", "Selected uri is $selectedImageUri")
                this.setURI(selectedImageUri?:return@registerForActivityResult, componentActivity.applicationContext)
            }
        }

    fun pickPhoto(){
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickerLauncher?.launch(galleryIntent)
    }

    fun setURI(uri: Uri, context: Context){
        val image = parseImage(uri, context) ?: return
        bitmap.value = image
    }

    private fun parseImage(uri: Uri, context: Context): Bitmap? {
        try {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}