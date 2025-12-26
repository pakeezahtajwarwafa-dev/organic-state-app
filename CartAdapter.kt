package com.example.organicstate.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.organicstate.databinding.ItemCartBinding
import com.example.organicstate.utils.CartItem

class CartAdapter(
    private val onQuantityChanged: (String, Int) -> Unit,
    private val onRemoveItem: (String) -> Unit
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CartViewHolder(
        private val binding: ItemCartBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cartItem: CartItem) {
            binding.apply {
                val product = cartItem.product

                // Product details
                tvProductName.text = product.name
                tvProductPrice.text = "৳${String.format("%.2f", product.price)}"
                tvProductUnit.text = "per ${product.unit}"

                // Quantity - Display current quantity
                tvQuantity.text = cartItem.quantity.toString()

                // Subtotal - Calculate based on quantity
                val subtotal = product.price * cartItem.quantity
                tvSubtotal.text = "৳${String.format("%.2f", subtotal)}"

                // Image placeholder
                ivProductImage.setImageResource(android.R.drawable.ic_menu_gallery)

                // Decrease button
                btnDecrease.setOnClickListener {
                    if (cartItem.quantity > 1) {
                        val newQuantity = cartItem.quantity - 1
                        onQuantityChanged(product.id, newQuantity)
                    }
                }

                // Increase button
                btnIncrease.setOnClickListener {
                    if (cartItem.quantity < product.stock) {
                        val newQuantity = cartItem.quantity + 1
                        onQuantityChanged(product.id, newQuantity)
                    }
                }

                // Remove button
                btnRemove.setOnClickListener {
                    onRemoveItem(product.id)
                }
            }
        }
    }

    private class CartDiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            // Items are the same if they have the same product ID
            return oldItem.product.id == newItem.product.id
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            // Contents are the same if everything matches (using data class equality)
            // This will compare both product and quantity
            return oldItem == newItem
        }
    }
}