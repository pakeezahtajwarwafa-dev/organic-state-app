package com.example.organicstate.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.organicstate.data.model.CustomerMessage
import com.example.organicstate.databinding.ItemMessageReceivedBinding
import com.example.organicstate.databinding.ItemMessageSentBinding
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val currentUserId: String
) : ListAdapter<CustomerMessage, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val binding = ItemMessageSentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            SentMessageViewHolder(binding)
        } else {
            val binding = ItemMessageReceivedBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            ReceivedMessageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (holder is SentMessageViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message)
        }
    }

    inner class SentMessageViewHolder(
        private val binding: ItemMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: CustomerMessage) {
            binding.tvMessage.text = message.message
            binding.tvTime.text = formatTime(message.timestamp?.toDate())
        }
    }

    inner class ReceivedMessageViewHolder(
        private val binding: ItemMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: CustomerMessage) {
            binding.tvMessage.text = message.message
            binding.tvSenderName.text = message.senderName
            binding.tvTime.text = formatTime(message.timestamp?.toDate())
        }
    }

    private fun formatTime(date: Date?): String {
        if (date == null) return ""

        val now = Calendar.getInstance()
        val messageTime = Calendar.getInstance().apply { time = date }

        return when {
            // Same day - show time only
            now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == messageTime.get(Calendar.DAY_OF_YEAR) -> {
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
            }
            // Yesterday
            now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) - messageTime.get(Calendar.DAY_OF_YEAR) == 1 -> {
                "Yesterday ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)}"
            }
            // Same year - show date without year
            now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
                SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(date)
            }
            // Different year - show full date
            else -> {
                SimpleDateFormat("MMM dd yyyy, hh:mm a", Locale.getDefault()).format(date)
            }
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<CustomerMessage>() {
        override fun areItemsTheSame(oldItem: CustomerMessage, newItem: CustomerMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CustomerMessage, newItem: CustomerMessage): Boolean {
            return oldItem == newItem
        }
    }
}