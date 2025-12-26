package com.example.organicstate.data.model

import com.google.firebase.Timestamp

data class Complaint(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val subject: String = "",
    val description: String = "",
    val status: String = "open", // open, in-progress, resolved
    val createdAt: Timestamp? = null,
    val resolvedAt: Timestamp? = null
) {
    constructor() : this("", "", "", "", "", "open", null, null)

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "userName" to userName,
            "subject" to subject,
            "description" to description,
            "status" to status,
            "createdAt" to (createdAt ?: Timestamp.now()),
            "resolvedAt" to resolvedAt
        )
    }
}