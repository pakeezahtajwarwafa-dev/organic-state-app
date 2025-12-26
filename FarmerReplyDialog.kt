package com.example.organicstate.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.organicstate.data.model.Review
import com.example.organicstate.data.repository.ReviewRepository
import com.example.organicstate.databinding.DialogFarmerReplyBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FarmerReplyDialog(
    context: Context,
    private val review: Review,
    private val onReplySent: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogFarmerReplyBinding
    private val reviewRepository = ReviewRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogFarmerReplyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setBackgroundDrawableResource(android.R.color.transparent)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        binding.apply {
            // Show review details
            tvReviewerName.text = review.userName
            tvReviewText.text = review.comment
            ratingBarReview.rating = review.rating

            // Pre-fill if editing existing reply
            if (review.farmerReply.isNotEmpty()) {
                etReply.setText(review.farmerReply)
                btnSendReply.text = "Update Reply"
            }
        }
    }

    private fun setupListeners() {
        binding.apply {
            btnCancel.setOnClickListener {
                dismiss()
            }

            btnSendReply.setOnClickListener {
                sendReply()
            }
        }
    }

    private fun sendReply() {
        val replyText = binding.etReply.text.toString().trim()

        if (replyText.isEmpty()) {
            Toast.makeText(context, "Please write a reply", Toast.LENGTH_SHORT).show()
            return
        }

        if (replyText.length < 10) {
            Toast.makeText(context, "Reply is too short (minimum 10 characters)", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            val result = reviewRepository.addFarmerReply(
                productId = review.productId,
                reviewId = review.id,
                farmerReply = replyText
            )

            withContext(Dispatchers.Main) {
                showLoading(false)

                result.onSuccess {
                    Toast.makeText(
                        context,
                        "Reply sent successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    onReplySent()
                    dismiss()
                }.onFailure { error ->
                    Toast.makeText(
                        context,
                        "Error: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
            btnSendReply.isEnabled = !show
            btnCancel.isEnabled = !show
            etReply.isEnabled = !show
        }
    }
}