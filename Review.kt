package com.example.organicstate.data.model

import com.google.firebase.Timestamp

data class Review(
    val id: String = "",
    val productId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImage: String = "",
    val rating: Float = 0f,           // 1-5 stars
    val comment: String = "",
    val timestamp: Timestamp? = null,
    val isVerifiedPurchase: Boolean = false,
    val likes: Int = 0,
    val farmerId: String = "",        // Product owner for farmer replies
    val farmerReply: String = "",     // Farmer's response
    val farmerReplyTimestamp: Timestamp? = null
) {
    // No-argument constructor for Firestore
    constructor() : this(
        "", "", "", "", "", 0f, "", null, false, 0, "", "", null
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "productId" to productId,
            "userId" to userId,
            "userName" to userName,
            "userProfileImage" to userProfileImage,
            "rating" to rating,
            "comment" to comment,
            "timestamp" to (timestamp ?: Timestamp.now()),
            "isVerifiedPurchase" to isVerifiedPurchase,
            "likes" to likes,
            "farmerId" to farmerId,
            "farmerReply" to farmerReply,
            "farmerReplyTimestamp" to farmerReplyTimestamp
        )
    }

    // Helper function to format timestamp
    fun getFormattedTime(): String {
        val now = System.currentTimeMillis()
        val reviewTime = timestamp?.toDate()?.time ?: now
        val diff = now - reviewTime

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000} minutes ago"
            diff < 86400000 -> "${diff / 3600000} hours ago"
            diff < 604800000 -> "${diff / 86400000} days ago"
            else -> "${diff / 604800000} weeks ago"
        }
    }
}

// Rating distribution data class
data class RatingDistribution(
    val fiveStar: Int = 0,
    val fourStar: Int = 0,
    val threeStar: Int = 0,
    val twoStar: Int = 0,
    val oneStar: Int = 0
) {
    fun getTotalReviews(): Int {
        return fiveStar + fourStar + threeStar + twoStar + oneStar
    }

    fun getPercentage(stars: Int): Int {
        val total = getTotalReviews()
        if (total == 0) return 0

        val count = when (stars) {
            5 -> fiveStar
            4 -> fourStar
            3 -> threeStar
            2 -> twoStar
            1 -> oneStar
            else -> 0
        }

        return (count * 100 / total)
    }

    fun toMap(): Map<String, Int> {
        return mapOf(
            "5" to fiveStar,
            "4" to fourStar,
            "3" to threeStar,
            "2" to twoStar,
            "1" to oneStar
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>?): RatingDistribution {
            if (map == null) return RatingDistribution()

            return RatingDistribution(
                fiveStar = (map["5"] as? Long)?.toInt() ?: 0,
                fourStar = (map["4"] as? Long)?.toInt() ?: 0,
                threeStar = (map["3"] as? Long)?.toInt() ?: 0,
                twoStar = (map["2"] as? Long)?.toInt() ?: 0,
                oneStar = (map["1"] as? Long)?.toInt() ?: 0
            )
        }
    }
}