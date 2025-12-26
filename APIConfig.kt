package com.example.organicstate.utils

object APIConfig {
    // OpenRouter API Configuration
    const val OPENROUTER_API_KEY = "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"
    const val OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1"

    // Models (all free tier)
    const val MODEL_CLAUDE_HAIKU = "anthropic/claude-3-haiku"
    const val MODEL_LLAMA = "meta-llama/llama-3.1-8b-instruct:free"
    const val MODEL_GEMINI = "google/gemini-flash-1.5"

    // Default model
    const val DEFAULT_MODEL = MODEL_CLAUDE_HAIKU

    // Fallback model if primary fails
    const val FALLBACK_MODEL = MODEL_LLAMA

    // Request configuration
    const val MAX_TOKENS = 1000
    const val TEMPERATURE = 0.7f

    // Timeouts
    const val CONNECT_TIMEOUT = 30L // seconds
    const val READ_TIMEOUT = 60L // seconds
    const val WRITE_TIMEOUT = 30L // seconds
}