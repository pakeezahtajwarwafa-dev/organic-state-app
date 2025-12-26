package com.example.organicstate.data.model

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "", // "customer", "farmer", "admin"
    val phone: String = "",
    val address: String = "",
    val profileImage: String = "",
    val createdAt: Timestamp? = null
) {
    // Empty constructor for Firestore
    constructor() : this("", "", "", "", "", "", "", null)

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "email" to email,
            "role" to role,
            "phone" to phone,
            "address" to address,
            "profileImage" to profileImage,
            "createdAt" to (createdAt ?: Timestamp.now())
        )
    }
}