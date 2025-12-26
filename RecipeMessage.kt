package com.example.organicstate.data.model

data class RecipeMessage(
    val id: String = System.currentTimeMillis().toString(),
    val text: String = "",
    val isUser: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val isTyping: Boolean = false,
    val isError: Boolean = false,
    val isSystem: Boolean = false
) {
    companion object {
        fun createUserMessage(text: String): RecipeMessage {
            return RecipeMessage(
                text = text,
                isUser = true
            )
        }

        fun createAIMessage(text: String): RecipeMessage {
            return RecipeMessage(
                text = text,
                isUser = false
            )
        }

        fun createSystemMessage(text: String): RecipeMessage {
            return RecipeMessage(
                text = text,
                isUser = false,
                isSystem = true
            )
        }

        fun createTypingIndicator(): RecipeMessage {
            return RecipeMessage(
                text = "AI is thinking...",
                isUser = false,
                isTyping = true
            )
        }

        fun createErrorMessage(error: String): RecipeMessage {
            return RecipeMessage(
                text = error,
                isUser = false,
                isError = true
            )
        }
    }

    fun getFormattedTime(): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }
}