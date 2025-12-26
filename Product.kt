package com.example.organicstate.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Product(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var price: Double = 0.0,
    var originalPrice: Double = 0.0,

    // ✅ FIXED: Use @PropertyName to ensure correct Firestore field mapping
    @get:PropertyName("isPromoted")
    @set:PropertyName("isPromoted")
    var isPromoted: Boolean = false,

    var discountLabel: String = "",
    var category: String = "",
    var farmerId: String = "",
    var farmerName: String = "",
    var stock: Int = 0,
    var unit: String = "kg",
    var images: List<String> = emptyList(),
    var rating: Double = 0.0,
    var reviewCount: Int = 0,
    var createdAt: Timestamp? = null,

    // ✅ FIXED: Use @PropertyName to ensure correct Firestore field mapping
    @get:PropertyName("isAvailable")
    @set:PropertyName("isAvailable")
    var isAvailable: Boolean = true,

    // NEW: Add ratingDistribution for review stats
    var ratingDistribution: Map<String, Int> = mapOf(
        "5" to 0,
        "4" to 0,
        "3" to 0,
        "2" to 0,
        "1" to 0
    )
) {
    // No-argument constructor for Firestore
    constructor() : this(
        "", "", "", 0.0, 0.0, false, "", "", "", "",
        0, "kg", emptyList(), 0.0, 0, null, true, mapOf()
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "description" to description,
            "price" to price,
            "originalPrice" to originalPrice,
            "isPromoted" to isPromoted,
            "discountLabel" to discountLabel,
            "category" to category,
            "farmerId" to farmerId,
            "farmerName" to farmerName,
            "stock" to stock,
            "unit" to unit,
            "images" to images,
            "rating" to rating,
            "reviewCount" to reviewCount,
            "createdAt" to createdAt,
            "isAvailable" to isAvailable,
            "ratingDistribution" to ratingDistribution
        )
    }
}