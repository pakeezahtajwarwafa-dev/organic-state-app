package com.example.organicstate.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.organicstate.R
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.model.Review
import com.example.organicstate.databinding.ItemReviewBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ReviewAdapter(
    private val onEditClick: ((Review) -> Unit)? = null,
    private val onDeleteClick: ((Review) -> Unit)? = null
) : ListAdapter<Review, ReviewAdapter.ReviewViewHolder>(ReviewDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReviewViewHolder(
        private val binding: ItemReviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(review: Review) {
            binding.apply {
                tvUserName.text = review.userName
                tvRating.text = review.rating.toString()
                tvComment.text = review.comment

                review.timestamp?.let {
                    tvTime.text = review.getFormattedTime()
                }

                // Verified purchase badge
                if (review.isVerifiedPurchase) {
                    tvVerifiedPurchase.visibility = View.VISIBLE
                } else {
                    tvVerifiedPurchase.visibility = View.GONE
                }

                // Farmer reply
                if (review.farmerReply.isNotEmpty()) {
                    layoutFarmerReply.visibility = View.VISIBLE
                    tvFarmerReply.text = review.farmerReply
                } else {
                    layoutFarmerReply.visibility = View.GONE
                }

                // CRITICAL FIX: Show edit/delete options if this is the current user's review
                val currentUserId = FirebaseManager.getCurrentUserId()
                if (currentUserId == review.userId && (onEditClick != null || onDeleteClick != null)) {
                    llReviewActions.visibility = View.VISIBLE

                    btnEditReview.setOnClickListener {
                        onEditClick?.invoke(review)
                    }

                    btnDeleteReview.setOnClickListener {
                        onDeleteClick?.invoke(review)
                    }
                } else {
                    llReviewActions.visibility = View.GONE
                }
            }
        }
    }

    private class ReviewDiffCallback : DiffUtil.ItemCallback<Review>() {
        override fun areItemsTheSame(oldItem: Review, newItem: Review): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Review, newItem: Review): Boolean {
            return oldItem == newItem
        }
    }
}