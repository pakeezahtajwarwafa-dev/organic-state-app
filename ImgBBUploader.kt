package com.example.organicstate.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object ImgBBUploader {

    private const val API_KEY = "5a03fd48d30fceb1b981158251bbaed7"

    private const val UPLOAD_URL = "https://api.imgbb.com/1/upload"

    suspend fun uploadImage(context: Context, imageUri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val base64Image = convertImageToBase64(context, imageUri)

                if (base64Image.isEmpty()) {
                    return@withContext Result.failure(Exception("Failed to convert image"))
                }

                val url = URL(UPLOAD_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.doInput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                val postData = "key=$API_KEY&image=${URLEncoder.encode(base64Image, "UTF-8")}"

                connection.outputStream.use { outputStream ->
                    outputStream.write(postData.toByteArray())
                    outputStream.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(response)

                    if (jsonResponse.getBoolean("success")) {
                        val imageUrl = jsonResponse.getJSONObject("data").getString("url")
                        Result.success(imageUrl)
                    } else {
                        Result.failure(Exception("Upload failed: ${jsonResponse.optString("error")}"))
                    }
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Result.failure(Exception("Upload failed with code: $responseCode. Error: $errorResponse"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Upload error: ${e.message}", e))
            }
        }
    }

    private fun convertImageToBase64(context: Context, imageUri: Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                return ""
            }

            val maxWidth = 1920
            val maxHeight = 1920
            val resizedBitmap = if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
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

            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val imageBytes = outputStream.toByteArray()

            if (resizedBitmap != bitmap) {
                resizedBitmap.recycle()
            }
            bitmap.recycle()

            Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}