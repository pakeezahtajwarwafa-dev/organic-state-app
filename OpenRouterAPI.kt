package com.example.organicstate.data.remote

import android.util.Log
import com.example.organicstate.utils.APIConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenRouterAPI {

    private val client = OkHttpClient.Builder()
        .connectTimeout(APIConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(APIConfig.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(APIConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "OpenRouterAPI"
    }

    /**
     * Send a text message to AI
     */
    suspend fun sendMessage(
        message: String,
        model: String = APIConfig.DEFAULT_MODEL
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = createTextRequest(message, model)
            val response = executeRequest(requestBody)

            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            Result.failure(e)
        }
    }

    /**
     * Send an image with text to AI for analysis
     */
    suspend fun analyzeImage(
        message: String,
        base64Image: String,
        model: String = APIConfig.DEFAULT_MODEL
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = createImageRequest(message, base64Image, model)
            val response = executeRequest(requestBody)

            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing image", e)
            Result.failure(e)
        }
    }

    /**
     * Create request body for text-only message
     */
    private fun createTextRequest(message: String, model: String): String {
        val systemPrompt = """
            You are an expert agricultural AI assistant helping farmers with crop diseases, pests, and farming advice.
            
            Guidelines:
            - Be concise but detailed
            - Use simple, practical language
            - Provide actionable advice
            - Include confidence levels when diagnosing
            - Suggest preventive measures
            - Consider organic farming practices
            
            Response format:
            - Start with the main answer
            - List symptoms if diagnosing
            - Provide treatment steps (numbered)
            - Add prevention tips
            - Keep total response under 500 words
        """.trimIndent()

        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", message)
            })
        }

        return JSONObject().apply {
            put("model", model)
            put("messages", messagesArray)
            put("max_tokens", APIConfig.MAX_TOKENS)
            put("temperature", APIConfig.TEMPERATURE)
        }.toString()
    }

    /**
     * Create request body for image + text
     */
    private fun createImageRequest(
        message: String,
        base64Image: String,
        model: String
    ): String {
        val systemPrompt = """
            You are an expert agricultural AI assistant specializing in crop disease and pest identification.
            
            When analyzing images:
            1. Identify the crop/plant
            2. Detect any visible diseases or pests
            3. Assess severity (mild/moderate/severe)
            4. Provide confidence level (%)
            5. List visible symptoms
            6. Recommend treatment (organic preferred)
            7. Suggest prevention measures
            
            Format your response clearly with sections:
            - **Crop Identified:** [name]
            - **Issue Detected:** [disease/pest name]
            - **Confidence:** [percentage]
            - **Severity:** [level]
            - **Symptoms:**
            - **Treatment:**
            - **Prevention:**
        """.trimIndent()

        val contentArray = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "text")
                put("text", message)
            })
            put(JSONObject().apply {
                put("type", "image_url")
                put("image_url", JSONObject().apply {
                    put("url", "data:image/jpeg;base64,$base64Image")
                })
            })
        }

        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", contentArray)
            })
        }

        return JSONObject().apply {
            put("model", model)
            put("messages", messagesArray)
            put("max_tokens", APIConfig.MAX_TOKENS)
            put("temperature", APIConfig.TEMPERATURE)
        }.toString()
    }

    /**
     * Execute HTTP request
     */
    private fun executeRequest(requestBody: String): String {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("${APIConfig.OPENROUTER_BASE_URL}/chat/completions")
            .addHeader("Authorization", "Bearer ${APIConfig.OPENROUTER_API_KEY}")
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://organicstate.app")
            .addHeader("X-Title", "Organic State Farming Assistant")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                throw Exception("API Error (${response.code}): $errorBody")
            }

            val responseBody = response.body?.string()
                ?: throw Exception("Empty response body")

            return parseResponse(responseBody)
        }
    }

    /**
     * Parse API response to extract message content
     */
    private fun parseResponse(responseBody: String): String {
        try {
            val json = JSONObject(responseBody)
            val choices = json.getJSONArray("choices")

            if (choices.length() == 0) {
                throw Exception("No response from AI")
            }

            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")
            val content = message.getString("content")

            return content.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response: $responseBody", e)
            throw Exception("Failed to parse AI response: ${e.message}")
        }
    }
}