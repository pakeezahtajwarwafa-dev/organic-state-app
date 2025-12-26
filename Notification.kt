package com.example.organicstate.data.model

import com.google.firebase.Timestamp

data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "general", // order, promotion, system, general
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null,
    val data: Map<String, String>? = null
) {
    constructor() : this("", "", "", "", "system", false, null, emptyMap())

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "title" to title,
            "message" to message,
            "type" to type,
            "isRead" to isRead,
            "createdAt" to (createdAt ?: Timestamp.now()),
            "data" to (data ?: emptyMap<String, String>())
        )
    }
}