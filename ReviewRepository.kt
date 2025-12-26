package com.example.organicstate.data.repository

import android.util.Log
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.model.Review
import com.example.organicstate.data.model.Order
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ReviewRepository {

    private val db = FirebaseManager.db
    private val TAG = "ReviewRepository"

    /**
     * Submit a new review for a product
     */
    suspend fun submitReview(review: Review): Result<String> {
        return try {
            // First, check if user already reviewed this product
            val existingReview = getUserReview(review.productId, review.userId)
            if (existingReview != null) {
                return Result.failure(Exception("You have already reviewed this product"))
            }

            // Add review to subcollection
            val docRef = db.collection("products")
                .document(review.productId)
                .collection("reviews")
                .add(review.toMap())
                .await()

            // Update product rating and review count
            updateProductRating(review.productId)

            Log.d(TAG, "Review submitted successfully: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting review", e)
            Result.failure(e)
        }
    }

    /**
     * Get all reviews for a product
     */
    fun getProductReviews(productId: String): Flow<List<Review>> = callbackFlow {
        val listener = db.collection("products")
            .document(productId)
            .collection("reviews")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error loading reviews", error)
                    close(error)
                    return@addSnapshotListener
                }

                val reviews = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Review::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                Log.d(TAG, "Loaded ${reviews.size} reviews for product $productId")
                trySend(reviews)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get a specific user's review for a product
     */
    suspend fun getUserReview(productId: String, userId: String): Review? {
        return try {
            val snapshot = db.collection("products")
                .document(productId)
                .collection("reviews")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toObject(Review::class.java)?.copy(
                id = snapshot.documents.first().id
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user review", e)
            null
        }
    }

    /**
     * Update an existing review
     */
    suspend fun updateReview(
        productId: String,
        reviewId: String,
        newRating: Float,
        newComment: String
    ): Result<Unit> {
        return try {
            db.collection("products")
                .document(productId)
                .collection("reviews")
                .document(reviewId)
                .update(
                    mapOf(
                        "rating" to newRating,
                        "comment" to newComment,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                )
                .await()

            // Recalculate product rating
            updateProductRating(productId)

            Log.d(TAG, "Review updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating review", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a review
     */
    suspend fun deleteReview(productId: String, reviewId: String): Result<Unit> {
        return try {
            db.collection("products")
                .document(productId)
                .collection("reviews")
                .document(reviewId)
                .delete()
                .await()

            // Recalculate product rating
            updateProductRating(productId)

            Log.d(TAG, "Review deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting review", e)
            Result.failure(e)
        }
    }

    /**
     * Add farmer reply to a review
     */
    suspend fun addFarmerReply(
        productId: String,
        reviewId: String,
        farmerReply: String
    ): Result<Unit> {
        return try {
            db.collection("products")
                .document(productId)
                .collection("reviews")
                .document(reviewId)
                .update(
                    mapOf(
                        "farmerReply" to farmerReply,
                        "farmerReplyTimestamp" to FieldValue.serverTimestamp()
                    )
                )
                .await()

            Log.d(TAG, "Farmer reply added successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding farmer reply", e)
            Result.failure(e)
        }
    }

    /**
     * Check if user has purchased the product (for verified purchase badge)
     */
    suspend fun hasUserPurchasedProduct(userId: String, productId: String): Boolean {
        return try {
            val snapshot = db.collection("orders")
                .whereEqualTo("customerId", userId)
                .whereIn("status", listOf("delivered", "confirmed", "shipped"))
                .get()
                .await()

            // Check if any order contains this product
            snapshot.documents.any { doc ->
                val order = doc.toObject(Order::class.java)
                order?.items?.any { it.productId == productId } == true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking purchase history", e)
            false
        }
    }

    /**
     * Recalculate and update product rating based on all reviews
     */
    suspend fun updateProductRating(productId: String) {
        try {
            // Get all reviews for this product
            val snapshot = db.collection("products")
                .document(productId)
                .collection("reviews")
                .get()
                .await()

            val reviews = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Review::class.java)
            }

            if (reviews.isEmpty()) {
                // No reviews, set rating to 0
                db.collection("products")
                    .document(productId)
                    .update(
                        mapOf(
                            "rating" to 0.0,
                            "reviewCount" to 0,
                            "ratingDistribution" to mapOf(
                                "5" to 0, "4" to 0, "3" to 0, "2" to 0, "1" to 0
                            )
                        )
                    )
                    .await()
                return
            }

            // Calculate average rating
            val totalRating = reviews.sumOf { it.rating.toDouble() }
            val averageRating = totalRating / reviews.size
            val reviewCount = reviews.size

            // Calculate rating distribution
            val distribution = mutableMapOf(
                "5" to 0, "4" to 0, "3" to 0, "2" to 0, "1" to 0
            )

            reviews.forEach { review ->
                val starKey = review.rating.toInt().toString()
                distribution[starKey] = (distribution[starKey] ?: 0) + 1
            }

            // Update product document
            db.collection("products")
                .document(productId)
                .update(
                    mapOf(
                        "rating" to averageRating,
                        "reviewCount" to reviewCount,
                        "ratingDistribution" to distribution
                    )
                )
                .await()

            Log.d(TAG, "Product rating updated: $averageRating ($reviewCount reviews)")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating product rating", e)
        }
    }

    /**
     * Get reviews by farmer (for farmer dashboard)
     */
    fun getReviewsByFarmer(farmerId: String): Flow<List<Pair<String, Review>>> = callbackFlow {
        val listener = db.collection("products")
            .whereEqualTo("farmerId", farmerId)
            .addSnapshotListener { productsSnapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val allReviews = mutableListOf<Pair<String, Review>>()

                productsSnapshot?.documents?.forEach { productDoc ->
                    // For each product, get its reviews
                    db.collection("products")
                        .document(productDoc.id)
                        .collection("reviews")
                        .get()
                        .addOnSuccessListener { reviewsSnapshot ->
                            val productReviews = reviewsSnapshot.documents.mapNotNull { reviewDoc ->
                                reviewDoc.toObject(Review::class.java)?.let { review ->
                                    productDoc.id to review.copy(id = reviewDoc.id)
                                }
                            }
                            allReviews.addAll(productReviews)
                            trySend(allReviews.toList())
                        }
                }
            }

        awaitClose { listener.remove() }
    }
}