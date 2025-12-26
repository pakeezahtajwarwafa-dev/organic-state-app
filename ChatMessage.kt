package com.example.organicstate.data.model

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val text: String = "",
    val imageUrl: String = "",
    val isUser: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val isTyping: Boolean = false,
    val isError: Boolean = false
) {
    companion object {
        fun createUserMessage(text: String, imageUrl: String = ""): ChatMessage {
            return ChatMessage(
                text = text,
                imageUrl = imageUrl,
                isUser = true
            )
        }

        fun createAIMessage(text: String): ChatMessage {
            return ChatMessage(
                text = text,
                isUser = false
            )
        }

        fun createTypingIndicator(): ChatMessage {
            return ChatMessage(
                text = "AI is thinking...",
                isUser = false,
                isTyping = true
            )
        }

        fun createErrorMessage(error: String): ChatMessage {
            return ChatMessage(
                text = "Error: $error",
                isUser = false,
                isError = true
            )
        }
    }

    fun getFormattedTime(): String {
        val hours = (timestamp / (1000 * 60 * 60)) % 24
        val minutes = (timestamp / (1000 * 60)) % 60
        return String.format("%02d:%02d", hours, minutes)
    }
}