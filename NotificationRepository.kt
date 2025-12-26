package com.example.organicstate.data.repository

import android.util.Log
import com.example.organicstate.data.model.Notification
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository {

    private val db = FirebaseFirestore.getInstance()
    private val notificationsCollection = db.collection("notifications")

    companion object {
        private const val TAG = "NotificationRepository"
    }

    suspend fun sendNotification(notification: Notification): Result<String> {
        return try {
            val data = hashMapOf(
                "userId" to notification.userId,
                "title" to notification.title,
                "message" to notification.message,
                "type" to notification.type,
                "isRead" to notification.isRead,
                "createdAt" to Timestamp.now(),
                "data" to (notification.data ?: emptyMap())
            )

            val docRef = notificationsCollection.add(data).await()
            Log.d(TAG, "✅ Notification sent: ${docRef.id} to user ${notification.userId}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending notification", e)
            Result.failure(e)
        }
    }

    fun getNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        Log.d(TAG, "📡 Setting up notification listener for: $userId")

        val listener = notificationsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error listening to notifications", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.w(TAG, "⚠️ Snapshot is null")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                Log.d(TAG, "📥 Received ${snapshot.size()} notifications")

                val notifications = snapshot.documents.mapNotNull { doc ->
                    try {
                        Notification(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            title = doc.getString("title") ?: "",
                            message = doc.getString("message") ?: "",
                            type = doc.getString("type") ?: "general",
                            isRead = doc.getBoolean("isRead") ?: false,
                            createdAt = doc.getTimestamp("createdAt"),
                            data = doc.get("data") as? Map<String, String>
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing notification: ${doc.id}", e)
                        null
                    }
                }

                Log.d(TAG, "✅ Sending ${notifications.size} notifications to UI")
                trySend(notifications)
            }

        awaitClose {
            Log.d(TAG, "🔚 Closing notification listener")
            listener.remove()
        }
    }

    fun getUnreadCount(userId: String): Flow<Int> = callbackFlow {
        Log.d(TAG, "📊 Setting up unread count listener for: $userId")

        val listener = notificationsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to unread count", error)
                    trySend(0)
                    return@addSnapshotListener
                }

                val count = snapshot?.size() ?: 0
                Log.d(TAG, "🔔 Unread count: $count")
                trySend(count)
            }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection
                .document(notificationId)
                .update("isRead", true)
                .await()
            Log.d(TAG, "✓ Marked as read: $notificationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking as read", e)
            Result.failure(e)
        }
    }

    suspend fun markAllAsRead(userId: String): Result<Unit> {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val batch = db.batch()
            snapshot.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()

            Log.d(TAG, "✓ Marked all as read for: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking all as read", e)
            Result.failure(e)
        }
    }
}