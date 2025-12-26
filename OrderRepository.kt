package com.example.organicstate.data.repository

import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.model.Order
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class OrderRepository {

    private val db = FirebaseManager.db

    suspend fun createOrder(order: Order): Result<String> {
        return try {
            val docRef = db.collection("orders").add(order.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // FIXED: Removed orderBy to avoid index requirement
    fun getOrdersByCustomer(customerId: String): Flow<List<Order>> = callbackFlow {
        val listener = db.collection("orders")
            .whereEqualTo("customerId", customerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.createdAt?.toDate()?.time ?: 0 }
                    ?: emptyList()

                trySend(orders)
            }

        awaitClose { listener.remove() }
    }

    // FIXED: Removed orderBy to avoid index requirement
    fun getOrdersByFarmer(farmerId: String): Flow<List<Order>> = callbackFlow {
        val listener = db.collection("orders")
            .whereEqualTo("farmerId", farmerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.createdAt?.toDate()?.time ?: 0 }
                    ?: emptyList()

                trySend(orders)
            }

        awaitClose { listener.remove() }
    }

    suspend fun updateOrderStatus(orderId: String, status: String): Result<Unit> {
        return try {
            db.collection("orders")
                .document(orderId)
                .update(mapOf(
                    "status" to status,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                ))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
