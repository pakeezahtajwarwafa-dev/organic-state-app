package com.example.organicstate.ui.adapters

import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.organicstate.R
import com.example.organicstate.data.model.Product
import com.example.organicstate.databinding.ItemProductBinding

class ProductAdapter(
    private val onItemClick: (Product) -> Unit,
    private val onAddToCart: ((Product) -> Unit)? = null
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    companion object {
        private const val TAG = "ProductAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // 🔥 FIX: Override submitList to add logging and force refresh
    override fun submitList(list: List<Product>?) {
        Log.d(TAG, "📝 submitList called with ${list?.size ?: 0} items")
        super.submitList(list?.toList()) // Create new list to force diff
    }

    inner class ProductViewHolder(
        private val binding: ItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.apply {
                // Product details
                tvProductName.text = product.name
                tvFarmerName.text = product.farmerName
                tvCategory.text = product.category

                // Discount display with strikethrough
                if (product.originalPrice > 0 && product.originalPrice > product.price) {
                    // Show original price with strikethrough
                    tvOriginalPrice.visibility = View.VISIBLE
                    tvOriginalPrice.text = "৳${String.format("%.2f", product.originalPrice)}"
                    tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

                    // Show discounted price in green
                    tvProductPrice.text = "৳${String.format("%.2f", product.price)}"
                    tvProductPrice.setTextColor(ContextCompat.getColor(root.context, R.color.primary_green))

                    // Show discount badge
                    if (product.discountLabel.isNotEmpty()) {
                        tvDiscountBadge.visibility = View.VISIBLE
                        tvDiscountBadge.text = product.discountLabel
                    } else {
                        // Calculate discount percentage
                        val discountPercent = ((product.originalPrice - product.price) / product.originalPrice * 100).toInt()
                        tvDiscountBadge.visibility = View.VISIBLE
                        tvDiscountBadge.text = "$discountPercent% OFF"
                    }
                } else {
                    // No discount
                    tvOriginalPrice.visibility = View.GONE
                    tvProductPrice.text = "৳${String.format("%.2f", product.price)}"
                    tvDiscountBadge.visibility = View.GONE
                }

                tvProductUnit.text = "per ${product.unit}"

                // Stock status with proper styling
                when {
                    product.stock == 0 -> {
                        tvStock.text = "Out of Stock"
                        tvStock.setTextColor(ContextCompat.getColor(root.context, R.color.error_red))
                        tvStock.visibility = View.VISIBLE
                        btnAddToCart.isEnabled = false
                        btnAddToCart.alpha = 0.5f
                    }
                    product.stock <= 5 -> {
                        tvStock.text = "Only ${product.stock} ${product.unit} left"
                        tvStock.setTextColor(ContextCompat.getColor(root.context, R.color.warning_orange))
                        tvStock.visibility = View.VISIBLE
                        btnAddToCart.isEnabled = true
                        btnAddToCart.alpha = 1f
                    }
                    else -> {
                        tvStock.text = "${product.stock} ${product.unit} available"
                        tvStock.setTextColor(ContextCompat.getColor(root.context, R.color.primary_green))
                        tvStock.visibility = View.VISIBLE
                        btnAddToCart.isEnabled = true
                        btnAddToCart.alpha = 1f
                    }
                }

                // Image loading
                if (product.images.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(product.images[0])
                        .centerCrop()
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(ivProductImage)
                } else {
                    ivProductImage.setImageResource(R.drawable.ic_image_placeholder)
                }

                // Show/hide add to cart button
                if (onAddToCart != null) {
                    btnAddToCart.visibility = View.VISIBLE
                    btnAddToCart.setOnClickListener {
                        if (product.stock > 0) {
                            onAddToCart.invoke(product)
                        }
                    }
                } else {
                    btnAddToCart.visibility = View.GONE
                }

                // Click listener
                root.setOnClickListener { onItemClick(product) }
            }
        }
    }

    private class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}