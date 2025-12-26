//package com.example.organicstate.data.local
//
//import androidx.lifecycle.LiveData
//import androidx.room.*
//
//@Dao
//interface CartDao {
//    @Query("SELECT * FROM cart_items")
//    fun getAllCartItems(): LiveData<List<com.example.organicstate.data.model.CartItem>>
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertCartItem(item: com.example.organicstate.data.model.CartItem)
//
//    @Update
//    suspend fun updateCartItem(item: com.example.organicstate.data.model.CartItem)
//
//    @Delete
//    suspend fun deleteCartItem(item: com.example.organicstate.data.model.CartItem)
//
//    @Query("DELETE FROM cart_items")
//    suspend fun clearCart()
//
//    @Query("SELECT COUNT(*) FROM cart_items")
//    fun getCartCount(): LiveData<Int>
//
//    @Query("SELECT * FROM cart_items WHERE productId = :productId")
//    suspend fun getCartItemById(productId: String): com.example.organicstate.data.model.CartItem?
//}