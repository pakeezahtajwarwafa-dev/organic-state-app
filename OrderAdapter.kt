package com.example.organicstate.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.organicstate.R
import com.example.organicstate.data.model.Order
import com.example.organicstate.databinding.ItemOrderBinding
import java.text.SimpleDateFormat
import java.util.Locale

class OrderAdapter(
    private val onOrderClick: (Order) -> Unit,
    private val onConfirm: (Order) -> Unit,
    private val onDeliver: (Order) -> Unit
) : ListAdapter<Order, OrderAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(
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
        private val binding: ItemOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(order: Order) {
            binding.apply {
                // Order ID (shortened)
                tvOrderId.text = "Order #${order.id.take(8)}"

                // Customer info
                tvCustomerName.text = order.customerName
                tvAddress.text = order.address.ifEmpty { "No address provided" }
                tvPhone.text = order.phone.ifEmpty { "No phone provided" }

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

                // Button visibility based on status
                when (order.status) {
                    "pending" -> {
                        btnConfirm.visibility = View.VISIBLE
                        btnDeliver.visibility = View.GONE
                    }
                    "confirmed" -> {
                        btnConfirm.visibility = View.GONE
                        btnDeliver.visibility = View.VISIBLE
                    }
                    else -> {
                        btnConfirm.visibility = View.GONE
                        btnDeliver.visibility = View.GONE
                    }
                }

                // Click listeners
                root.setOnClickListener { onOrderClick(order) }
                btnConfirm.setOnClickListener { onConfirm(order) }
                btnDeliver.setOnClickListener { onDeliver(order) }
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