package com.example.organicstate.ui.farmer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.organicstate.data.model.Chat
import com.example.organicstate.databinding.ItemChatBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatListAdapter(
    private val onChatClick: (Chat) -> Unit
) : ListAdapter<Chat, ChatListAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatViewHolder(
        private val binding: ItemChatBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: Chat) {
            binding.tvName.text = chat.customerName
            binding.tvLastMessage.text = chat.lastMessage
            binding.tvTime.text = formatTime(chat.lastMessageTime?.toDate())

            binding.root.setOnClickListener {
                onChatClick(chat)
            }
        }

        private fun formatTime(date: Date?): String {
            if (date == null) return ""

            val now = Calendar.getInstance()
            val messageTime = Calendar.getInstance().apply { time = date }

            return when {
                now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) &&
                        now.get(Calendar.DAY_OF_YEAR) == messageTime.get(Calendar.DAY_OF_YEAR) -> {
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
                }
                else -> {
                    SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
                }
            }
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<Chat>() {
        override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem == newItem
        }
    }
}