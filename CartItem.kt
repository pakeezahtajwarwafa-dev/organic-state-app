//package com.example.organicstate.data.model
//
//import androidx.room.Entity
//import androidx.room.PrimaryKey
//
//@Entity(tableName = "cart_items")
//data class CartItem(
//    @PrimaryKey
//    val productId: String = "",
//    val productName: String = "",
//    val price: Double = 0.0,
//    val imageUrl: String = "",
//    val farmerName: String = "",
//    var quantity: Int = 1,
//    val unit: String = "kg",
//    val farmerId: String = ""
//) {
//    constructor() : this("", "", 0.0, "", "", 1, "kg", "")
//
//    fun getTotalPrice(): Double = price * quantity
//}