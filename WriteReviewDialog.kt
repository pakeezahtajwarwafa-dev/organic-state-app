package com.example.organicstate.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.model.Review
import com.example.organicstate.databinding.DialogWriteReviewBinding
import com.example.organicstate.viewmodel.ReviewViewModel
import com.google.firebase.Timestamp

class WriteReviewDialog : DialogFragment() {

    private var _binding: DialogWriteReviewBinding? = null
    private val binding get() = _binding!!

    private val reviewViewModel: ReviewViewModel by viewModels()

    private var productId: String = ""
    private var productName: String = ""
    private var existingReview: Review? = null // For editing

    companion object {
        private const val ARG_PRODUCT_ID = "product_id"
        private const val ARG_PRODUCT_NAME = "product_name"
        private const val ARG_EXISTING_REVIEW = "existing_review"

        fun newInstance(productId: String, productName: String, existingReview: Review? = null): WriteReviewDialog {
            val fragment = WriteReviewDialog()
            val args = Bundle().apply {
                putString(ARG_PRODUCT_ID, productId)
                putString(ARG_PRODUCT_NAME, productName)
                // Note: You'll need to make Review Serializable or Parcelable to pass it
            }
            fragment.arguments = args
            fragment.existingReview = existingReview
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            productId = it.getString(ARG_PRODUCT_ID) ?: ""
            productName = it.getString(ARG_PRODUCT_NAME) ?: ""
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogWriteReviewBinding.inflate(LayoutInflater.from(context))

        setupUI()
        setupListeners()

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun setupUI() {
        binding.tvProductName.text = productName

        // If editing existing review
        existingReview?.let { review ->
            binding.tvDialogTitle.text = "Edit Review"
            binding.btnSubmit.text = "Update"
            binding.ratingBar.rating = review.rating
            binding.etComment.setText(review.comment)
        }

        // Rating bar listener
        binding.ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            binding.tvRatingText.text = when (rating.toInt()) {
                5 -> "Excellent"
                4 -> "Good"
                3 -> "Average"
                2 -> "Poor"
                1 -> "Terrible"
                else -> ""
            }
        }

        // CRITICAL FIX: Check if user has purchased (verified purchase)
        val userId = FirebaseManager.getCurrentUserId() ?: return
        reviewViewModel.checkPurchaseHistory(userId, productId) { isPurchased ->
            if (isPurchased) {
                binding.llVerifiedBadge.visibility = View.VISIBLE
            } else {
                binding.llVerifiedBadge.visibility = View.GONE
            }
        }
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSubmit.setOnClickListener {
            if (existingReview != null) {
                updateReview()
            } else {
                submitReview()
            }
        }
    }

    private fun submitReview() {
        val userId = FirebaseManager.getCurrentUserId()
        if (userId == null) {
            Toast.makeText(context, "Please login to review", Toast.LENGTH_SHORT).show()
            return
        }

        val rating = binding.ratingBar.rating
        if (rating == 0f) {
            Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show()
            return
        }

        val comment = binding.etComment.text.toString().trim()

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false

        // Get user info
        val userName = FirebaseManager.auth.currentUser?.displayName ?: "Anonymous"

        val review = Review(
            productId = productId,
            userId = userId,
            userName = userName,
            rating = rating,
            comment = comment,
            timestamp = Timestamp.now(),
            isVerifiedPurchase = binding.llVerifiedBadge.visibility == View.VISIBLE
        )

        reviewViewModel.submitReview(review) { result ->
            binding.progressBar.visibility = View.GONE
            binding.btnSubmit.isEnabled = true

            result.onSuccess {
                Toast.makeText(context, "Review submitted successfully!", Toast.LENGTH_SHORT).show()
                dismiss()
                // Refresh product detail page
                (activity as? com.example.organicstate.ui.customer.ProductDetailActivity)?.let {
                    // This will trigger a refresh
                }
            }.onFailure { error ->
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateReview() {
        val rating = binding.ratingBar.rating
        if (rating == 0f) {
            Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show()
            return
        }

        val comment = binding.etComment.text.toString().trim()

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false

        existingReview?.let { review ->
            reviewViewModel.updateReview(productId, review.id, rating, comment) { result ->
                binding.progressBar.visibility = View.GONE
                binding.btnSubmit.isEnabled = true

                result.onSuccess {
                    Toast.makeText(context, "Review updated successfully!", Toast.LENGTH_SHORT).show()
                    dismiss()
                }.onFailure { error ->
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
