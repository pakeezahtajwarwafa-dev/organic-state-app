package com.example.organicstate.ui.customer

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.organicstate.R
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.model.Product
import com.example.organicstate.data.model.Review
import com.example.organicstate.databinding.ActivityProductDetailBinding
import com.example.organicstate.ui.adapters.ReviewAdapter
import com.example.organicstate.ui.dialogs.WriteReviewDialog
import com.example.organicstate.viewmodel.CartViewModel
import com.example.organicstate.viewmodel.ProductViewModel
import com.example.organicstate.viewmodel.ReviewViewModel

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private val productViewModel: ProductViewModel by viewModels()
    private val cartViewModel: CartViewModel by viewModels()
    private val reviewViewModel: ReviewViewModel by viewModels()

    private var currentProduct: Product? = null
    private var currentQuantity = 1
    private var userReview: Review? = null

    private lateinit var reviewAdapter: ReviewAdapter

    companion object {
        private const val TAG = "ProductDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val productId = intent.getStringExtra("product_id")
        if (productId.isNullOrEmpty()) {
            Toast.makeText(this, "Product ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupRecyclerView()
        setupListeners()
        loadProductDetails(productId)
    }

    private fun setupRecyclerView() {
        // CRITICAL FIX: Pass callbacks for edit/delete
        reviewAdapter = ReviewAdapter(
            onEditClick = { review ->
                showEditReviewDialog(review)
            },
            onDeleteClick = { review ->
                showDeleteConfirmation(review)
            }
        )
        binding.rvReviews.apply {
            layoutManager = LinearLayoutManager(this@ProductDetailActivity)
            adapter = reviewAdapter
        }
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.btnIncreaseQuantity.setOnClickListener {
            val maxStock = currentProduct?.stock ?: 0
            if (currentQuantity < maxStock) {
                currentQuantity++
                updateQuantityDisplay()
            } else {
                Toast.makeText(this, "Maximum stock reached", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDecreaseQuantity.setOnClickListener {
            if (currentQuantity > 1) {
                currentQuantity--
                updateQuantityDisplay()
            }
        }

        binding.btnAddToCart.setOnClickListener {
            currentProduct?.let { product ->
                if (product.stock >= currentQuantity) {
                    cartViewModel.addToCart(product, currentQuantity)
                    Toast.makeText(
                        this,
                        "${product.name} added to cart (x$currentQuantity)",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(this, "Not enough stock available", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnWriteReview.setOnClickListener {
            currentProduct?.let { product ->
                val userId = FirebaseManager.getCurrentUserId()
                if (userId == null) {
                    Toast.makeText(this, "Please login to review", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // CRITICAL FIX: Check if user has already reviewed
                reviewViewModel.checkPurchaseHistory(userId, product.id) { hasPurchased ->
                    if (!hasPurchased) {
                        AlertDialog.Builder(this)
                            .setTitle("Purchase Required")
                            .setMessage("You can only review products you've purchased and received.")
                            .setPositiveButton("OK", null)
                            .show()
                        return@checkPurchaseHistory
                    }

                    // User has purchased - check if they already reviewed
                    if (userReview != null) {
                        // Show edit dialog
                        showEditReviewDialog(userReview!!)
                    } else {
                        // Show write dialog
                        showWriteReviewDialog(product)
                    }
                }
            }
        }
    }

    private fun loadProductDetails(productId: String) {
        binding.progressBar.visibility = View.VISIBLE

        productViewModel.getProductById(productId)

        productViewModel.productDetail.observe(this) { result ->
            result.onSuccess { product ->
                if (product != null) {
                    currentProduct = product
                    displayProductDetails(product)
                    loadReviews(product.id)
                    checkUserReview(product.id)
                } else {
                    Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
                binding.progressBar.visibility = View.GONE
            }.onFailure { exception ->
                Log.e(TAG, "Error loading product", exception)
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                finish()
            }
        }
    }

    private fun displayProductDetails(product: Product) {
        binding.apply {
            tvProductName.text = product.name
            tvProductDescription.text = product.description
            tvProductPrice.text = "৳${String.format("%.2f", product.price)}"
            tvProductUnit.text = product.unit
            tvFarmerName.text = "Sold by: ${product.farmerName}"
            tvCategory.text = "Category: ${product.category}"

            // Stock display with proper styling
            when {
                product.stock == 0 -> {
                    tvStock.text = "Out of Stock"
                    tvStock.setTextColor(getColor(R.color.error_red))
                    btnAddToCart.isEnabled = false
                    btnAddToCart.text = "Out of Stock"
                }
                product.stock <= 5 -> {
                    tvStock.text = "Only ${product.stock} ${product.unit} left"
                    tvStock.setTextColor(getColor(R.color.warning_orange))
                }
                else -> {
                    tvStock.text = "${product.stock} ${product.unit} available"
                    tvStock.setTextColor(getColor(R.color.primary_green))
                }
            }

            // Load product image
            if (product.images.isNotEmpty()) {
                Glide.with(this@ProductDetailActivity)
                    .load(product.images[0])
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(ivProductImage)
            } else {
                ivProductImage.setImageResource(R.drawable.ic_image_placeholder)
            }

            updateQuantityDisplay()
        }
    }

    private fun loadReviews(productId: String) {
        reviewViewModel.loadReviews(productId)

        reviewViewModel.reviews.observe(this) { reviews ->
            Log.d(TAG, "Loaded ${reviews.size} reviews")

            if (reviews.isEmpty()) {
                binding.rvReviews.visibility = View.GONE
                binding.tvNoReviews.visibility = View.VISIBLE
                binding.llRatingSummary.visibility = View.GONE
            } else {
                binding.rvReviews.visibility = View.VISIBLE
                binding.tvNoReviews.visibility = View.GONE
                binding.llRatingSummary.visibility = View.VISIBLE

                reviewAdapter.submitList(reviews)
                updateRatingSummary(reviews)
            }
        }
    }

    private fun updateRatingSummary(reviews: List<Review>) {
        if (reviews.isEmpty()) return

        val avgRating = reviews.map { it.rating }.average()
        val totalReviews = reviews.size

        val distribution = mutableMapOf(5 to 0, 4 to 0, 3 to 0, 2 to 0, 1 to 0)
        reviews.forEach { review ->
            val stars = review.rating.toInt()
            distribution[stars] = (distribution[stars] ?: 0) + 1
        }

        binding.apply {
            tvOverallRating.text = String.format("%.1f", avgRating)
            tvTotalReviews.text = "$totalReviews reviews"

            distribution.forEach { (stars, count) ->
                val percentage = if (totalReviews > 0) (count * 100 / totalReviews) else 0
                when (stars) {
                    5 -> {
                        progressBar5Star.progress = percentage
                        tvCount5Star.text = "$percentage%"
                    }
                    4 -> {
                        progressBar4Star.progress = percentage
                        tvCount4Star.text = "$percentage%"
                    }
                    3 -> {
                        progressBar3Star.progress = percentage
                        tvCount3Star.text = "$percentage%"
                    }
                    2 -> {
                        progressBar2Star.progress = percentage
                        tvCount2Star.text = "$percentage%"
                    }
                    1 -> {
                        progressBar1Star.progress = percentage
                        tvCount1Star.text = "$percentage%"
                    }
                }
            }
        }
    }

    private fun checkUserReview(productId: String) {
        val userId = FirebaseManager.getCurrentUserId() ?: return

        reviewViewModel.getUserReview(productId, userId)

        reviewViewModel.userReview.observe(this) { review ->
            userReview = review
            binding.btnWriteReview.text = if (review != null) {
                "Edit Your Review"
            } else {
                "Write a Review"
            }
        }
    }

    private fun showWriteReviewDialog(product: Product) {
        val dialog = WriteReviewDialog.newInstance(product.id, product.name)
        dialog.show(supportFragmentManager, "WriteReviewDialog")
    }

    private fun showEditReviewDialog(review: Review) {
        val dialog = WriteReviewDialog.newInstance(
            currentProduct?.id ?: "",
            currentProduct?.name ?: "",
            review
        )
        dialog.show(supportFragmentManager, "EditReviewDialog")
    }

    private fun showDeleteConfirmation(review: Review) {
        AlertDialog.Builder(this)
            .setTitle("Delete Review")
            .setMessage("Are you sure you want to delete your review?")
            .setPositiveButton("Delete") { _, _ ->
                deleteReview(review)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteReview(review: Review) {
        binding.progressBar.visibility = View.VISIBLE

        reviewViewModel.deleteReview(currentProduct?.id ?: "", review.id) { result ->
            binding.progressBar.visibility = View.GONE

            result.onSuccess {
                Toast.makeText(this, "Review deleted successfully", Toast.LENGTH_SHORT).show()
                // Refresh reviews
                currentProduct?.let { loadReviews(it.id) }
                checkUserReview(currentProduct?.id ?: "")
            }.onFailure { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateQuantityDisplay() {
        val product = currentProduct ?: return

        binding.tvQuantity.text = currentQuantity.toString()

        val totalPrice = product.price * currentQuantity
        binding.tvTotalPrice.text = "Total: ৳${String.format("%.2f", totalPrice)}"

        binding.btnIncreaseQuantity.isEnabled = currentQuantity < product.stock
        binding.btnDecreaseQuantity.isEnabled = currentQuantity > 1
    }

    fun deleteReview(productId: String, reviewId: String, callback: (Result<Unit>) -> Unit) {
        reviewViewModel.deleteReview(productId, reviewId, callback)
    }
}