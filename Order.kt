package com.example.organicstate.data.model

import com.google.firebase.Timestamp

data class OrderItem(
    val productId: String = "",
    val name: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0
) {
    constructor() : this("", "", 0, 0.0)
}

data class Order(
    val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val farmerId: String = "",
    val farmerName: String = "",  // ADDED: Farmer name
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val status: String = "pending", // pending, confirmed, shipped, delivered, cancelled
    val address: String = "",
    val phone: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    constructor() : this("", "", "", "", "", emptyList(), 0.0, "pending", "", "", null, null)

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "customerId" to customerId,
            "customerName" to customerName,
            "farmerId" to farmerId,
            "farmerName" to farmerName,  // ADDED: Include farmer name
            "items" to items.map { mapOf(
                "productId" to it.productId,
                "name" to it.name,
                "quantity" to it.quantity,
                "price" to it.price
            )},
            "totalAmount" to totalAmount,
            "status" to status,
            "address" to address,
            "phone" to phone,
            "createdAt" to (createdAt ?: Timestamp.now()),
            "updatedAt" to Timestamp.now()
        )
    }
}