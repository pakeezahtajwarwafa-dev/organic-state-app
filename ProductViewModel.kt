package com.example.organicstate.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.example.organicstate.data.model.Product
import com.example.organicstate.data.repository.ProductRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ProductViewModel : ViewModel() {
    private val repository = ProductRepository()

    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

    private val _productDetail = MutableLiveData<Result<Product?>>()
    val productDetail: LiveData<Result<Product?>> = _productDetail

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var allProductsJob: Job? = null
    private var farmerProductsJob: Job? = null
    private var promotedProductsJob: Job? = null

    private var isListenerActive = false

    companion object {
        private const val TAG = "ProductViewModel"
    }

    init {
        Log.d(TAG, "🎬 ViewModel initialized")
    }

    fun getProductById(productId: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = repository.getProductById(productId)
            _productDetail.value = result
            _loading.value = false
        }
    }

    // CRITICAL FIX: Only start listener once
    fun loadAllProducts() {
        if (isListenerActive) {
            Log.d(TAG, "⏭️ Listener already active, skipping...")
            return
        }

        Log.d(TAG, "🔄 Starting products listener...")
        isListenerActive = true

        allProductsJob = viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                repository.getAllProducts().collect { productList ->
                    Log.d(TAG, "📦 Received ${productList.size} products")
                    _products.postValue(productList)
                    _loading.postValue(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in products listener", e)
                _error.postValue(e.message)
                _loading.postValue(false)
                isListenerActive = false
            }
        }
    }

    fun loadPromotedProducts() {
        promotedProductsJob?.cancel()

        promotedProductsJob = viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                repository.getPromotedProducts().collect { productList ->
                    _products.postValue(productList)
                    _loading.postValue(false)
                }
            } catch (e: Exception) {
                _error.postValue(e.message)
                _loading.postValue(false)
            }
        }
    }

    fun loadFarmerProducts(farmerId: String) {
        farmerProductsJob?.cancel()

        farmerProductsJob = viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                repository.getProductsByFarmer(farmerId).collect { productList ->
                    _products.postValue(productList)
                    _loading.postValue(false)
                }
            } catch (e: Exception) {
                _error.postValue(e.message)
                _loading.postValue(false)
            }
        }
    }

    fun searchProducts(query: String) {
        viewModelScope.launch {
            _loading.value = true
            repository.searchProducts(query).onSuccess { list -> _products.value = list }
            _loading.value = false
        }
    }

    fun addProduct(product: Product) {
        viewModelScope.launch {
            val result = repository.addProduct(product)
            if (result.isSuccess) {
                Log.d(TAG, "✅ Product added successfully")
            } else {
                Log.e(TAG, "❌ Failed to add product: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun updateProduct(productId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            repository.updateProduct(productId, updates)
        }
    }

    fun refreshProducts() {
        Log.d(TAG, "🔄 Refresh requested - listener is already active")
        // No need to do anything - the listener is already active and will auto-update
    }

    override fun onCleared() {
        super.onCleared()
        isListenerActive = false
        allProductsJob?.cancel()
        farmerProductsJob?.cancel()
        promotedProductsJob?.cancel()
        Log.d(TAG, "🔚 ViewModel cleared")
    }
}