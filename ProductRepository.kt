package com.example.organicstate.data.repository

import android.util.Log
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.model.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProductRepository {

    private val db = FirebaseManager.db
    private val auth = FirebaseManager.auth
    private val TAG = "ProductRepository"

    fun getAllProducts(): Flow<List<Product>> = callbackFlow {
        Log.d(TAG, "🔄 getAllProducts() called")
        Log.d(TAG, "   Current user: ${auth.currentUser?.uid}")

        val listener = db.collection("products")
            .whereEqualTo("isAvailable", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Listener error: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.w(TAG, "⚠️ Snapshot is null")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                Log.d(TAG, "📦 Snapshot received")
                Log.d(TAG, "   Documents: ${snapshot.documents.size}")
                Log.d(TAG, "   From cache: ${snapshot.metadata.isFromCache}")

                val products = snapshot.documents.mapNotNull { doc ->
                    try {
                        val product = doc.toObject(Product::class.java)?.copy(id = doc.id)
                        if (product != null) {
                            Log.d(TAG, "   ✅ ${product.name} - ${product.category}")
                        }
                        product
                    } catch (e: Exception) {
                        Log.e(TAG, "   ❌ Error parsing doc ${doc.id}", e)
                        null
                    }
                }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0 }

                Log.d(TAG, "📤 Sending ${products.size} products")
                trySend(products)
            }

        awaitClose {
            Log.d(TAG, "🔚 Closing listener")
            listener.remove()
        }
    }

    fun getPromotedProducts(): Flow<List<Product>> = callbackFlow {
        val listener = db.collection("products")
            .whereEqualTo("isPromoted", true)
            .whereEqualTo("isAvailable", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error in getPromotedProducts", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val products = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.createdAt?.toDate()?.time ?: 0 } ?: emptyList()
                trySend(products)
            }
        awaitClose { listener.remove() }
    }

    fun getProductsByFarmer(farmerId: String): Flow<List<Product>> = callbackFlow {
        val listener = db.collection("products")
            .whereEqualTo("farmerId", farmerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error in getProductsByFarmer", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val products = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.createdAt?.toDate()?.time ?: 0 } ?: emptyList()
                trySend(products)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getProductById(productId: String): Result<Product?> {
        return try {
            val doc = db.collection("products").document(productId).get().await()
            val product = doc.toObject(Product::class.java)?.copy(id = doc.id)
            Result.success(product)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting product by ID", e)
            Result.failure(e)
        }
    }

    suspend fun searchProducts(query: String): Result<List<Product>> {
        return try {
            val snapshot = db.collection("products")
                .whereEqualTo("isAvailable", true)
                .get()
                .await()

            val products = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Product::class.java)?.copy(id = doc.id)
            }.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true) ||
                        it.category.contains(query, ignoreCase = true)
            }

            Result.success(products)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error searching products", e)
            Result.failure(e)
        }
    }

    suspend fun addProduct(product: Product): Result<Unit> {
        return try {
            Log.d(TAG, "➕ Adding product: ${product.name}")

            val productWithTime = product.copy(
                createdAt = com.google.firebase.Timestamp.now(),
                isAvailable = true
            )

            val docRef = db.collection("products").add(productWithTime.toMap()).await()
            Log.d(TAG, "✅ Product added with ID: ${docRef.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error adding product", e)
            Result.failure(e)
        }
    }

    suspend fun updateProduct(productId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Updating product: $productId")
            db.collection("products").document(productId).update(updates).await()
            Log.d(TAG, "✅ Product updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating product", e)
            Result.failure(e)
        }
    }
}