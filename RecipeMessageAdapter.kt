package com.example.organicstate.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.organicstate.R
import com.example.organicstate.data.model.RecipeMessage
import com.example.organicstate.databinding.ItemRecipeMessageBinding

class RecipeMessageAdapter : ListAdapter<RecipeMessage, RecipeMessageAdapter.MessageViewHolder>(
    MessageDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemRecipeMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(
        private val binding: ItemRecipeMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: RecipeMessage) {
            binding.apply {
                // Show/hide appropriate message bubble
                if (message.isUser) {
                    cvUserMessage.visibility = View.VISIBLE
                    cvAiMessage.visibility = View.GONE
                    tvUserMessage.text = message.text
                    tvUserTime.text = message.getFormattedTime()
                } else {
                    cvUserMessage.visibility = View.GONE
                    cvAiMessage.visibility = View.VISIBLE

                    when {
                        message.isTyping -> {
                            tvAiMessage.text = "🤔 Thinking..."
                            progressTyping.visibility = View.VISIBLE
                            tvAiMessage.alpha = 0.7f
                        }
                        message.isError -> {
                            tvAiMessage.text = "❌ ${message.text}"
                            progressTyping.visibility = View.GONE
                            tvAiMessage.alpha = 1f
                            cvAiMessage.setCardBackgroundColor(
                                itemView.context.getColor(R.color.error_red).let {
                                    android.graphics.Color.argb(30,
                                        android.graphics.Color.red(it),
                                        android.graphics.Color.green(it),
                                        android.graphics.Color.blue(it)
                                    )
                                }
                            )
                        }
                        message.isSystem -> {
                            tvAiMessage.text = "ℹ️ ${message.text}"
                            progressTyping.visibility = View.GONE
                            tvAiMessage.alpha = 0.8f
                            cvAiMessage.setCardBackgroundColor(
                                itemView.context.getColor(R.color.accent_green).let {
                                    android.graphics.Color.argb(30,
                                        android.graphics.Color.red(it),
                                        android.graphics.Color.green(it),
                                        android.graphics.Color.blue(it)
                                    )
                                }
                            )
                        }
                        else -> {
                            tvAiMessage.text = message.text
                            progressTyping.visibility = View.GONE
                            tvAiMessage.alpha = 1f
                            cvAiMessage.setCardBackgroundColor(
                                itemView.context.getColor(android.R.color.white)
                            )
                        }
                    }

                    tvAiTime.text = message.getFormattedTime()
                }
            }
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<RecipeMessage>() {
        override fun areItemsTheSame(oldItem: RecipeMessage, newItem: RecipeMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RecipeMessage, newItem: RecipeMessage): Boolean {
            return oldItem == newItem
        }
    }
}