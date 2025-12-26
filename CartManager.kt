package com.example.organicstate.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.model.Product

data class CartItem(
    val product: Product,
    val quantity: Int = 1
)

object CartManager {

    private val _cartItems = MutableLiveData<List<CartItem>>(emptyList())
    val cartItems: LiveData<List<CartItem>> = _cartItems

    private val _totalAmount = MutableLiveData<Double>(0.0)
    val totalAmount: LiveData<Double> = _totalAmount

    fun addToCart(product: Product, quantity: Int = 1): Boolean {
        // FIXED: Prevent farmers from adding their own products
        val currentUserId = FirebaseManager.getCurrentUserId()
        if (currentUserId != null && product.farmerId == currentUserId) {
            // Cannot add own product to cart
            return false
        }

        val currentItems = _cartItems.value?.toMutableList() ?: mutableListOf()

        val existingItemIndex = currentItems.indexOfFirst { it.product.id == product.id }
        if (existingItemIndex != -1) {
            // Update existing item
            val existingItem = currentItems[existingItemIndex]
            currentItems[existingItemIndex] = existingItem.copy(quantity = existingItem.quantity + quantity)
        } else {
            // Add new item
            currentItems.add(CartItem(product, quantity))
        }

        _cartItems.value = currentItems.toList()
        calculateTotal()
        return true
    }

    fun removeFromCart(productId: String) {
        val currentItems = _cartItems.value?.toMutableList() ?: mutableListOf()
        currentItems.removeAll { it.product.id == productId }

        _cartItems.value = currentItems.toList()
        calculateTotal()
    }

    fun updateQuantity(productId: String, quantity: Int) {
        if (quantity < 1) return

        val currentItems = _cartItems.value?.toMutableList() ?: mutableListOf()
        val itemIndex = currentItems.indexOfFirst { it.product.id == productId }

        if (itemIndex != -1) {
            val oldItem = currentItems[itemIndex]
            currentItems[itemIndex] = oldItem.copy(quantity = quantity)

            _cartItems.value = currentItems.toList()
            calculateTotal()
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _totalAmount.value = 0.0
    }

    fun getCartCount(): Int {
        return _cartItems.value?.size ?: 0
    }

    private fun calculateTotal() {
        val total = _cartItems.value?.sumOf { it.product.price * it.quantity } ?: 0.0
        _totalAmount.value = total
    }
}