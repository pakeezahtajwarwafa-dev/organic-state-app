package com.example.organicstate.ui.adapters

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.organicstate.R
import com.example.organicstate.data.model.Notification
import com.example.organicstate.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationAdapter(
    private val onItemClick: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

        fun bind(notification: Notification) {
            binding.apply {
                tvTitle.text = notification.title
                tvMessage.text = notification.message

                // Format date
                notification.createdAt?.let {
                    tvTime.text = dateFormat.format(it.toDate())
                }

                // Type badge logic
                tvType.text = notification.type.uppercase()
                val typeColor = when (notification.type) {
                    "order" -> R.color.notification_order
                    "promotion" -> R.color.notification_promotion
                    else -> R.color.notification_system
                }
                tvType.setBackgroundResource(typeColor)

                // --- READ/UNREAD STYLING (FIXED) ---
                // We keep alpha at 1.0f so it is never "faded/blurry"
                root.alpha = 1.0f

                if (notification.isRead) {
                    // READ STATE: Normal weight and secondary grey color
                    tvTitle.setTypeface(null, Typeface.NORMAL)
                    tvTitle.setTextColor(ContextCompat.getColor(root.context, R.color.text_secondary))
                    tvMessage.setTextColor(ContextCompat.getColor(root.context, R.color.text_secondary))
                } else {
                    // UNREAD STATE: Bold weight and deep primary color
                    tvTitle.setTypeface(null, Typeface.BOLD)
                    tvTitle.setTextColor(ContextCompat.getColor(root.context, R.color.text_primary))
                    tvMessage.setTextColor(ContextCompat.getColor(root.context, R.color.text_primary))
                }

                // Click listener
                root.setOnClickListener { onItemClick(notification) }
            }
        }
    }

    private class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }
    }
}