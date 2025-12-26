package com.example.organicstate.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.organicstate.R
import com.example.organicstate.data.model.Product
import com.example.organicstate.databinding.FragmentStoreMonitoringBinding
import com.example.organicstate.ui.adapters.AdminProductAdapter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class StoreMonitoringFragment : Fragment() {

    private var _binding: FragmentStoreMonitoringBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private lateinit var productAdapter: AdminProductAdapter
    private var allProducts = mutableListOf<Product>()
    private var filteredProducts = mutableListOf<Product>()
    private var currentFilter = FilterType.ALL
    private var searchQuery = ""

    enum class FilterType {
        ALL, AVAILABLE, UNAVAILABLE
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStoreMonitoringBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFilters()
        setupSearch()
        setupSwipeRefresh()
        loadProducts()
    }

    private fun setupRecyclerView() {
        productAdapter = AdminProductAdapter(
            products = emptyList(),
            onDeleteClick = { product -> showDeleteConfirmation(product) },
            onPromoteToggle = { product, isPromoted -> updatePromotionStatus(product, isPromoted) }
        )
        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productAdapter
        }
    }

    private fun setupFilters() {
        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = FilterType.ALL
                applyFilters()
            }
        }

        binding.chipAvailable.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = FilterType.AVAILABLE
                applyFilters()
            }
        }

        binding.chipUnavailable.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = FilterType.UNAVAILABLE
                applyFilters()
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s.toString().trim()
                applyFilters()
            }
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadProducts()
        }
    }

    private fun applyFilters() {
        filteredProducts.clear()

        // First apply category filter
        val categoryFiltered = when (currentFilter) {
            FilterType.ALL -> allProducts
            FilterType.AVAILABLE -> allProducts.filter { it.stock > 0 }
            FilterType.UNAVAILABLE -> allProducts.filter { it.stock <= 0 }
        }

        // Then apply search filter
        filteredProducts.addAll(
            if (searchQuery.isEmpty()) {
                categoryFiltered
            } else {
                categoryFiltered.filter { product ->
                    product.name.contains(searchQuery, ignoreCase = true) ||
                            product.farmerName?.contains(searchQuery, ignoreCase = true) == true ||
                            product.category.contains(searchQuery, ignoreCase = true)
                }
            }
        )

        // Update UI
        productAdapter.updateList(filteredProducts)
        updateStats()
        updateEmptyState()
    }

    private fun updateStats() {
        val availableCount = allProducts.count { it.stock > 0 }
        val unavailableCount = allProducts.count { it.stock <= 0 }

        binding.tvTotalProducts.text = "Total: ${allProducts.size}"
        binding.tvAvailableCount.text = "Available: $availableCount"
        binding.tvUnavailableCount.text = "Unavailable: $unavailableCount"
    }

    private fun updateEmptyState() {
        if (filteredProducts.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvProducts.visibility = View.GONE
            binding.tvEmptyState.text = when {
                searchQuery.isNotEmpty() -> "No products found for '$searchQuery'"
                currentFilter == FilterType.AVAILABLE -> "No available products"
                currentFilter == FilterType.UNAVAILABLE -> "No unavailable products"
                else -> "No products found"
            }
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvProducts.visibility = View.VISIBLE
        }
    }

    private fun updatePromotionStatus(product: Product, isPromoted: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("products").document(product.id)
                    .update("isPromoted", isPromoted)
                    .await()

                // Update local list
                val index = allProducts.indexOfFirst { it.id == product.id }
                if (index != -1) {
                    allProducts[index] = allProducts[index].copy(isPromoted = isPromoted)
                }

                withContext(Dispatchers.Main) {
                    val message = if (isPromoted) "Added to Offers Tab" else "Removed from Offers Tab"
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    applyFilters()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadProducts() {
        binding.progressBar.visibility = View.VISIBLE
        binding.swipeRefresh.isRefreshing = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("products").get().await()
                val products = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                }

                withContext(Dispatchers.Main) {
                    allProducts.clear()
                    allProducts.addAll(products)
                    applyFilters()
                    binding.progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showDeleteConfirmation(product: Product) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete '${product.name}'?")
            .setPositiveButton("Delete") { _, _ -> deleteProduct(product) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteProduct(product: Product) {
        binding.progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("products").document(product.id).delete().await()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Product deleted", Toast.LENGTH_SHORT).show()
                    loadProducts()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}