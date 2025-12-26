package com.example.organicstate.data.model

import com.google.firebase.Timestamp

data class Chat(
    val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val farmerId: String = "",
    val farmerName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Timestamp? = null,
    val unreadCount: Map<String, Int> = emptyMap()
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", "", "", "", "", null, emptyMap())

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "customerId" to customerId,
            "customerName" to customerName,
            "farmerId" to farmerId,
            "farmerName" to farmerName,
            "lastMessage" to lastMessage,
            "lastMessageTime" to (lastMessageTime ?: Timestamp.now()),
            "unreadCount" to unreadCount
        )
    }
}