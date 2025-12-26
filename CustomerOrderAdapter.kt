package com.example.organicstate.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.organicstate.R
import com.example.organicstate.data.model.Order
import com.example.organicstate.databinding.ItemCustomerOrderBinding
import java.text.SimpleDateFormat
import java.util.Locale

class CustomerOrderAdapter(
    private val onOrderClick: (Order) -> Unit
) : ListAdapter<Order, CustomerOrderAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemCustomerOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderViewHolder(
        private val binding: ItemCustomerOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(order: Order) {
            binding.apply {
                // Order ID (shortened)
                tvOrderId.text = "Order #${order.id.take(8)}"

                // Farmer info - FIXED: Show farmer name from order
                val farmerDisplayName = if (order.farmerName.isNotEmpty()) {
                    order.farmerName
                } else {
                    "Farmer"
                }
                tvFarmerInfo.text = "From: $farmerDisplayName"

                // Items summary
                val itemsSummary = order.items.joinToString(", ") {
                    "${it.name} (x${it.quantity})"
                }
                tvItems.text = "Items: $itemsSummary"

                // Total
                tvTotal.text = "Total: ৳${String.format("%.2f", order.totalAmount)}"

                // Date
                order.createdAt?.let {
                    tvDate.text = dateFormat.format(it.toDate())
                }

                // Status badge
                tvStatus.text = order.status.uppercase()
                val statusColor = when (order.status) {
                    "pending" -> R.color.warning_orange
                    "confirmed" -> R.color.status_in_progress
                    "delivered" -> R.color.status_resolved
                    else -> R.color.text_secondary
                }
                tvStatus.setBackgroundResource(statusColor)

                // Status message
                val statusMessage = when (order.status) {
                    "pending" -> "⏳ Waiting for farmer confirmation"
                    "confirmed" -> "✅ Order confirmed! Preparing your items"
                    "delivered" -> "🎉 Order delivered! Hope you enjoyed it"
                    else -> "Status: ${order.status}"
                }
                tvStatusMessage.text = statusMessage

                // Click listener
                root.setOnClickListener { onOrderClick(order) }
            }
        }
    }

    private class OrderDiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }
    }
}