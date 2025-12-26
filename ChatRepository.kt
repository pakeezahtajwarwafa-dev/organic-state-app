package com.example.organicstate.data.repository

import android.util.Log
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.model.Chat
import com.example.organicstate.data.model.CustomerMessage
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val db = FirebaseManager.db
    private val TAG = "ChatRepository"

    /**
     * Get total unread message count for a user
     */
    fun getUnreadChatCount(userId: String): Flow<Int> = callbackFlow {
        Log.d(TAG, "📊 Setting up unread chat count listener for: $userId")

        val listener = db.collection("chats")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error listening to unread count", error)
                    trySend(0)
                    return@addSnapshotListener
                }

                val totalUnread = snapshot?.documents?.sumOf { doc ->
                    try {
                        val chat = doc.toObject(Chat::class.java)

                        // Only count chats where this user is a participant
                        if (chat?.customerId == userId || chat?.farmerId == userId) {
                            val unreadCount = chat?.unreadCount?.get(userId) ?: 0
                            Log.d(TAG, "   Chat ${doc.id}: $unreadCount unread")
                            unreadCount
                        } else {
                            0
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parsing chat: ${doc.id}", e)
                        0
                    }
                } ?: 0

                Log.d(TAG, "💬 Total unread count for $userId: $totalUnread")
                trySend(totalUnread)
            }

        awaitClose {
            Log.d(TAG, "📚 Closing unread count listener")
            listener.remove()
        }
    }

    /**
     * Get or create a chat between customer and farmer
     */
    suspend fun getOrCreateChat(
        customerId: String,
        customerName: String,
        farmerId: String,
        farmerName: String
    ): Result<String> {
        return try {
            // Check if chat already exists
            val chatId = generateChatId(customerId, farmerId)
            val chatDoc = db.collection("chats").document(chatId).get().await()

            if (!chatDoc.exists()) {
                // Create new chat
                val chat = Chat(
                    id = chatId,
                    customerId = customerId,
                    customerName = customerName,
                    farmerId = farmerId,
                    farmerName = farmerName,
                    lastMessage = "Chat started",
                    lastMessageTime = Timestamp.now(),
                    unreadCount = mapOf(customerId to 0, farmerId to 0)
                )
                db.collection("chats").document(chatId).set(chat.toMap()).await()
                Log.d(TAG, "✅ New chat created: $chatId")
            }

            Result.success(chatId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating chat", e)
            Result.failure(e)
        }
    }

    /**
     * Generate consistent chat ID from user IDs
     */
    private fun generateChatId(customerId: String, farmerId: String): String {
        return "chat_${customerId}_${farmerId}"
    }

    /**
     * Send a message in a chat
     */
    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        messageText: String
    ): Result<Unit> {
        return try {
            val message = CustomerMessage(
                senderId = senderId,
                senderName = senderName,
                message = messageText,
                timestamp = Timestamp.now(),
                isRead = false
            )

            // Add message to subcollection
            val messageRef = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(message.toMap())
                .await()

            // Update chat's last message and increment unread count
            val chatRef = db.collection("chats").document(chatId)
            val chatDoc = chatRef.get().await()
            val chat = chatDoc.toObject(Chat::class.java)

            if (chat != null) {
                val receiverId = if (senderId == chat.customerId) chat.farmerId else chat.customerId
                val newUnreadCount = chat.unreadCount.toMutableMap()
                newUnreadCount[receiverId] = (newUnreadCount[receiverId] ?: 0) + 1

                chatRef.update(
                    mapOf(
                        "lastMessage" to messageText,
                        "lastMessageTime" to Timestamp.now(),
                        "unreadCount" to newUnreadCount
                    )
                ).await()
            }

            Log.d(TAG, "✅ Message sent: ${messageRef.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending message", e)
            Result.failure(e)
        }
    }

    /**
     * Get real-time messages for a chat
     */
    fun getMessages(chatId: String): Flow<List<CustomerMessage>> = callbackFlow {
        val listener = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error listening to messages", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CustomerMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                Log.d(TAG, "💬 Received ${messages.size} messages")
                trySend(messages)
            }

        awaitClose {
            Log.d(TAG, "📚 Closing messages listener")
            listener.remove()
        }
    }

    /**
     * Get all chats for a user
     */
    fun getUserChats(userId: String): Flow<List<Chat>> = callbackFlow {
        Log.d(TAG, "📱 Loading chats for user: $userId")

        val listener = db.collection("chats")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error listening to chats", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val chats = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val chat = doc.toObject(Chat::class.java)?.copy(id = doc.id)

                        // Debug logging
                        Log.d(TAG, "📄 Chat doc: ${doc.id}")
                        Log.d(TAG, "   customerId: ${chat?.customerId}")
                        Log.d(TAG, "   farmerId: ${chat?.farmerId}")
                        Log.d(TAG, "   Match? ${chat?.customerId == userId || chat?.farmerId == userId}")

                        // Only return chats where user is participant
                        if (chat?.customerId == userId || chat?.farmerId == userId) {
                            chat
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parsing chat: ${doc.id}", e)
                        null
                    }
                }?.sortedByDescending { it.lastMessageTime?.toDate()?.time ?: 0 } ?: emptyList()

                Log.d(TAG, "💬 User has ${chats.size} chats")
                chats.forEach {
                    Log.d(TAG, "   - ${it.id}: ${it.customerName} ↔️ ${it.farmerName}")
                }

                trySend(chats)
            }

        awaitClose {
            Log.d(TAG, "📚 Closing chats listener")
            listener.remove()
        }
    }

    /**
     * Mark messages as read
     */
    suspend fun markMessagesAsRead(chatId: String, userId: String): Result<Unit> {
        return try {
            val chatRef = db.collection("chats").document(chatId)
            val chatDoc = chatRef.get().await()
            val chat = chatDoc.toObject(Chat::class.java)

            if (chat != null) {
                val newUnreadCount = chat.unreadCount.toMutableMap()
                newUnreadCount[userId] = 0

                chatRef.update("unreadCount", newUnreadCount).await()
                Log.d(TAG, "✅ Messages marked as read")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error marking messages as read", e)
            Result.failure(e)
        }
    }

    /**
     * Get unique farmers from products
     */
    suspend fun getUniqueFarmers(): Result<List<Pair<String, String>>> {
        return try {
            val snapshot = db.collection("products")
                .whereEqualTo("isAvailable", true)
                .get()
                .await()

            val farmers = snapshot.documents
                .mapNotNull { doc ->
                    val farmerId = doc.getString("farmerId")
                    val farmerName = doc.getString("farmerName")
                    if (!farmerId.isNullOrEmpty() && !farmerName.isNullOrEmpty()) {
                        farmerId to farmerName
                    } else null
                }
                .distinctBy { it.first }
                .sortedBy { it.second }

            Log.d(TAG, "👨‍🌾 Found ${farmers.size} unique farmers")
            Result.success(farmers)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting farmers", e)
            Result.failure(e)
        }
    }
}