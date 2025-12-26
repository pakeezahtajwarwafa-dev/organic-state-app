package com.example.organicstate.data.model

import com.google.firebase.Timestamp

data class CustomerMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Timestamp? = null,
    val isRead: Boolean = false
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", "", "", null, false)

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "senderId" to senderId,
            "senderName" to senderName,
            "message" to message,
            "timestamp" to (timestamp ?: Timestamp.now()),
            "isRead" to isRead
        )
    }
}