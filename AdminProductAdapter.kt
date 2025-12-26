package com.example.organicstate.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.organicstate.R
import com.example.organicstate.data.model.Product
import com.example.organicstate.databinding.ItemAdminProductBinding

class AdminProductAdapter(
    private var products: List<Product>,
    private val onDeleteClick: (Product) -> Unit,
    private val onPromoteToggle: (Product, Boolean) -> Unit
) : RecyclerView.Adapter<AdminProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemAdminProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount() = products.size

    fun updateList(newList: List<Product>) {
        products = newList
        notifyDataSetChanged()
    }

    inner class ProductViewHolder(private val binding: ItemAdminProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.apply {
                tvProductName.text = product.name
                tvFarmerName.text = "By ${product.farmerName}"

                // Show if there is an active discount from farmer
                if (product.originalPrice > product.price) {
                    tvPriceInfo.text = "SALE: ৳${product.price} (Was ৳${product.originalPrice})"
                    tvPriceInfo.setTextColor(root.context.getColor(R.color.error_red))
                } else {
                    tvPriceInfo.text = "Regular: ৳${product.price}"
                    tvPriceInfo.setTextColor(root.context.getColor(R.color.primary_green))
                }

                // Promote Switch logic
                switchPromote.setOnCheckedChangeListener(null) // Prevent loop
                switchPromote.isChecked = product.isPromoted
                switchPromote.setOnCheckedChangeListener { _, isChecked ->
                    onPromoteToggle(product, isChecked)
                }

                if (product.images.isNotEmpty()) {
                    Glide.with(itemView.context).load(product.images[0]).into(ivProduct)
                }

                btnDelete.setOnClickListener { onDeleteClick(product) }
            }
        }
    }
}