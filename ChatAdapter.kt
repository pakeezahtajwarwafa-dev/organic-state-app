package com.example.organicstate.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.organicstate.R
import com.example.organicstate.data.model.ChatMessage
import com.example.organicstate.databinding.ItemMessageAiBinding
import com.example.organicstate.databinding.ItemMessageUserBinding

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isUser) VIEW_TYPE_USER else VIEW_TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val binding = ItemMessageUserBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                UserMessageViewHolder(binding)
            }
            else -> {
                val binding = ItemMessageAiBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                AIMessageViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is UserMessageViewHolder -> holder.bind(getItem(position))
            is AIMessageViewHolder -> holder.bind(getItem(position))
        }
    }

    // User Message ViewHolder
    inner class UserMessageViewHolder(
        private val binding: ItemMessageUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.apply {
                // Set text
                if (message.text.isNotEmpty()) {
                    tvMessage.text = message.text
                    tvMessage.visibility = View.VISIBLE
                } else {
                    tvMessage.visibility = View.GONE
                }

                // Set image if present
                if (message.imageUrl.isNotEmpty()) {
                    ivImage.visibility = View.VISIBLE
                    Glide.with(root.context)
                        .load(message.imageUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .into(ivImage)
                } else {
                    ivImage.visibility = View.GONE
                }

                // Set timestamp
                tvTimestamp.text = message.getFormattedTime()
            }
        }
    }

    // AI Message ViewHolder
    inner class AIMessageViewHolder(
        private val binding: ItemMessageAiBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.apply {
                if (message.isTyping) {
                    // Show typing indicator
                    tvMessage.visibility = View.GONE
                    typingIndicator.visibility = View.VISIBLE
                } else {
                    // Show message
                    tvMessage.visibility = View.VISIBLE
                    typingIndicator.visibility = View.GONE

                    tvMessage.text = message.text

                    // Set timestamp
                    tvTimestamp.text = message.getFormattedTime()

                    // Change color for errors
                    if (message.isError) {
                        tvMessage.setTextColor(
                            root.context.getColor(R.color.error_red)
                        )
                    } else {
                        tvMessage.setTextColor(
                            root.context.getColor(R.color.text_primary)
                        )
                    }
                }
            }
        }
    }

    private class ChatDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}