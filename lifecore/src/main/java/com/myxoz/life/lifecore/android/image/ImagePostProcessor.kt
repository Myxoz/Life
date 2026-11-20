package com.myxoz.life.lifecore.android.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream

object ImagePostProcessor {
    sealed interface PostProcessingOption {
        fun applyToBitmap(bitmap: Bitmap): Bitmap
        object CropToSquare: PostProcessingOption {
            override fun applyToBitmap(bitmap: Bitmap): Bitmap {
                val width = bitmap.width
                val height = bitmap.height
                val size = minOf(width, height)
                return Bitmap.createBitmap(bitmap, (width - size) / 2, (height - size) / 2, size, size)
            }
        }
        data class Scale(val width: Int, val height: Int = width): PostProcessingOption {
            override fun applyToBitmap(bitmap: Bitmap): Bitmap =
                bitmap.scale(width, height)
        }
        data class Compress(val format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG, val quality: Int = 80): PostProcessingOption {
            override fun applyToBitmap(bitmap: Bitmap): Bitmap {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(format, quality, outputStream)
                val byteArray = outputStream.toByteArray()
                return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            }
        }
    }
    fun List<PostProcessingOption>.process(bitmap: Bitmap): Bitmap =
        fold(bitmap) {acc, option -> option.applyToBitmap(acc) }
}