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

class RecipeAIService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(APIConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(APIConfig.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(APIConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "RecipeAIService"
    }

    /**
     * Get recipe suggestions from ingredients
     */
    suspend fun getRecipesFromIngredients(ingredients: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val systemPrompt = """
                    You are an expert chef and nutritionist helping people create delicious recipes.
                    
                    When given ingredients:
                    1. Suggest 2-3 practical recipes using those ingredients
                    2. Prioritize recipes that use MOST of the listed ingredients
                    3. Include preparation time and difficulty level
                    4. Provide brief cooking instructions
                    5. Mention if additional common ingredients are needed
                    6. Consider dietary preferences if mentioned
                    7. Add nutritional highlights
                    
                    Format your response clearly:
                    
                    🍳 **Recipe 1: [Name]**
                    ⏱️ Time: [X] minutes | 📊 Difficulty: [Easy/Medium/Hard]
                    
                    **Ingredients:**
                    - [List all ingredients with quantities]
                    
                    **Instructions:**
                    1. [Step 1]
                    2. [Step 2]
                    ...
                    
                    💡 **Nutritional Highlights:** [Key nutrients]
                    
                    ---
                    
                    [Repeat for other recipes]
                    
                    Keep recipes practical, achievable, and delicious!
                """.trimIndent()

                val userPrompt = """
                    I have these ingredients: $ingredients
                    
                    Please suggest some delicious recipes I can make with them!
                """.trimIndent()

                val requestBody = createRequest(systemPrompt, userPrompt)
                val response = executeRequest(requestBody)

                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting recipes", e)
                Result.failure(e)
            }
        }

    /**
     * Get ingredients list from recipe name
     */
    suspend fun getIngredientsFromRecipe(recipeName: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val systemPrompt = """
                    You are an expert chef providing detailed ingredient lists and cooking instructions.
                    
                    When given a recipe name or description:
                    1. Provide the complete ingredients list with exact quantities
                    2. Organize ingredients by category (produce, proteins, spices, etc.)
                    3. Include common pantry items if needed
                    4. Provide step-by-step cooking instructions
                    5. Add preparation and cooking times
                    6. Include tips for best results
                    7. Suggest possible substitutions
                    
                    Format your response clearly:
                    
                    🍽️ **[Recipe Name]**
                    👥 Serves: [X] people
                    ⏱️ Prep: [X] min | Cook: [X] min | Total: [X] min
                    
                    **Ingredients:**
                    
                    *Produce:*
                    - [Item] - [Quantity]
                    
                    *Proteins:*
                    - [Item] - [Quantity]
                    
                    *Spices & Seasonings:*
                    - [Item] - [Quantity]
                    
                    *Pantry Items:*
                    - [Item] - [Quantity]
                    
                    **Instructions:**
                    1. [Detailed step]
                    2. [Detailed step]
                    ...
                    
                    💡 **Pro Tips:**
                    - [Tip 1]
                    - [Tip 2]
                    
                    🔄 **Substitutions:**
                    - [Ingredient]: Can use [Alternative]
                    
                    Be specific with quantities and measurements!
                """.trimIndent()

                val userPrompt = """
                    I want to cook: $recipeName
                    
                    Please provide the complete ingredients list and cooking instructions!
                """.trimIndent()

                val requestBody = createRequest(systemPrompt, userPrompt)
                val response = executeRequest(requestBody)

                Result.success(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting ingredients", e)
                Result.failure(e)
            }
        }

    /**
     * Create request body for AI API
     */
    private fun createRequest(systemPrompt: String, userPrompt: String): String {
        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            })
        }

        return JSONObject().apply {
            put("model", APIConfig.DEFAULT_MODEL)
            put("messages", messagesArray)
            put("max_tokens", 2000) // More tokens for detailed recipes
            put("temperature", 0.8) // Slightly higher for creativity
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
            .addHeader("X-Title", "Organic State Recipe Assistant")
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
     * Parse API response
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