package com.example.organicstate.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.organicstate.data.model.Product
import com.example.organicstate.utils.CartItem
import com.example.organicstate.utils.CartManager

class CartViewModel : ViewModel() {

    // Delegate to CartManager singleton
    val cartItems: LiveData<List<CartItem>> = CartManager.cartItems
    val totalAmount: LiveData<Double> = CartManager.totalAmount

    fun addToCart(product: Product, quantity: Int = 1) {
        CartManager.addToCart(product, quantity)
    }

    fun removeFromCart(productId: String) {
        CartManager.removeFromCart(productId)
    }

    fun updateQuantity(productId: String, quantity: Int) {
        CartManager.updateQuantity(productId, quantity)
    }

    fun clearCart() {
        CartManager.clearCart()
    }

    fun getCartCount(): Int {
        return CartManager.getCartCount()
    }
}