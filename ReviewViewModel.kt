package com.example.organicstate.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.organicstate.data.model.Review
import com.example.organicstate.data.repository.ReviewRepository
import kotlinx.coroutines.launch

class ReviewViewModel : ViewModel() {

    private val repository = ReviewRepository()

    private val _reviews = MutableLiveData<List<Review>>()
    val reviews: LiveData<List<Review>> = _reviews

    private val _userReview = MutableLiveData<Review?>()
    val userReview: LiveData<Review?> = _userReview

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadReviews(productId: String) {
        viewModelScope.launch {
            _loading.value = true
            repository.getProductReviews(productId).collect { reviewList ->
                _reviews.value = reviewList
                _loading.value = false
            }
        }
    }

    fun getUserReview(productId: String, userId: String) {
        viewModelScope.launch {
            val review = repository.getUserReview(productId, userId)
            _userReview.value = review
        }
    }

    fun submitReview(review: Review, callback: (Result<String>) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            val result = repository.submitReview(review)
            _loading.value = false
            callback(result)
        }
    }

    fun updateReview(
        productId: String,
        reviewId: String,
        newRating: Float,
        newComment: String,
        callback: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            _loading.value = true
            val result = repository.updateReview(productId, reviewId, newRating, newComment)
            _loading.value = false
            callback(result)
        }
    }

    fun deleteReview(productId: String, reviewId: String, callback: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            // This calls the delete method in your Repository
            val result = repository.deleteReview(productId, reviewId)
            _loading.value = false
            callback(result)
        }
    }

    fun checkPurchaseHistory(userId: String, productId: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val hasPurchased = repository.hasUserPurchasedProduct(userId, productId)
            callback(hasPurchased)
        }
    }
}