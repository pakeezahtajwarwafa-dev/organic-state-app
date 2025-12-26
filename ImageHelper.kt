package com.example.organicstate.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object ImageHelper {

    /**
     * Convert image URI to Base64 string with compression
     */
    suspend fun convertToBase64(context: Context, imageUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap == null) {
                    return@withContext null
                }

                // Compress and resize
                val compressedBitmap = compressImage(bitmap)

                // Convert to base64
                val outputStream = ByteArrayOutputStream()
                compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val imageBytes = outputStream.toByteArray()

                // Clean up
                if (compressedBitmap != bitmap) {
                    compressedBitmap.recycle()
                }
                bitmap.recycle()

                Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Compress and resize image to reduce size
     */
    private fun compressImage(bitmap: Bitmap): Bitmap {
        val maxWidth = 1024
        val maxHeight = 1024

        return if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
            val ratio = Math.min(
                maxWidth.toFloat() / bitmap.width,
                maxHeight.toFloat() / bitmap.height
            )
            val newWidth = (bitmap.width * ratio).toInt()
            val newHeight = (bitmap.height * ratio).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }

    /**
     * Get file size in KB
     */
    fun getImageSize(context: Context, uri: Uri): Long {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val size = inputStream?.available()?.toLong() ?: 0L
            inputStream?.close()
            size / 1024 // Convert to KB
        } catch (e: Exception) {
            0L
        }
    }
}