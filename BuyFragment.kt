package com.example.organicstate.ui.farmer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.model.Product
import com.example.organicstate.databinding.FragmentBuyBinding
import com.example.organicstate.ui.adapters.ProductAdapter
import com.example.organicstate.ui.customer.ProductDetailActivity
import com.example.organicstate.utils.CartManager
import com.example.organicstate.viewmodel.ProductViewModel

class BuyFragment : Fragment() {

    private var _binding: FragmentBuyBinding? = null
    private val binding get() = _binding!!

    private val productViewModel: ProductViewModel by activityViewModels()
    private lateinit var productAdapter: ProductAdapter

    private var allProducts: List<Product> = emptyList()
    private var currentCategory: CategoryFilter = CategoryFilter.ALL
    private var searchQuery: String = ""

    private var isFirstLoad = true  // Track if this is the first load

    companion object {
        private const val TAG = "BuyFragment"
    }

    enum class CategoryFilter {
        ALL, GROCERIES, SEEDS, FERTILIZERS
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBuyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()

        // CRITICAL FIX: Start the listener
        productViewModel.loadAllProducts()

    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            onItemClick = { product ->
                try {
                    val intent = Intent(requireContext(), ProductDetailActivity::class.java)
                    intent.putExtra("product_id", product.id)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening product", e)
                    Toast.makeText(requireContext(), "Error opening product", Toast.LENGTH_SHORT).show()
                }
            },
            onAddToCart = { product ->
                val added = CartManager.addToCart(product)
                if (added) {
                    Toast.makeText(requireContext(), "${product.name} added to cart", Toast.LENGTH_SHORT).show()
                    updateCartBadge()
                } else {
                    Toast.makeText(requireContext(), "Cannot add your own product to cart", Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = productAdapter
        }
    }

    private fun setupObservers() {
        productViewModel.products.observe(viewLifecycleOwner) { products ->
            Log.d(TAG, "📦 Products received in Fragment: ${products.size}")

            // Sort by createdAt before storing
            allProducts = products.sortedByDescending {
                it.createdAt?.toDate()?.time ?: 0
            }

            Log.d(TAG, "✅ Products sorted: ${allProducts.size}")
            allProducts.take(3).forEach { product ->
                Log.d(TAG, "   - ${product.name}, farmerId: ${product.farmerId}")
            }

            applyFilters()
        }

        productViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.swipeRefresh.isRefreshing = isLoading
        }

        CartManager.cartItems.observe(viewLifecycleOwner) {
            updateCartBadge()
        }
    }

    private fun setupListeners() {
        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            currentCategory = when (checkedIds.firstOrNull()) {
                binding.chipAll.id -> CategoryFilter.ALL
                binding.chipGroceries.id -> CategoryFilter.GROCERIES
                binding.chipSeeds.id -> CategoryFilter.SEEDS
                binding.chipFertilizers.id -> CategoryFilter.FERTILIZERS
                else -> CategoryFilter.ALL
            }
            Log.d(TAG, "Category changed to: $currentCategory")
            applyFilters()
        }

        binding.etSearch.setOnEditorActionListener { v, _, _ ->
            searchQuery = v.text.toString().trim()
            Log.d(TAG, "Search query: $searchQuery")
            applyFilters()
            true
        }

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty() && searchQuery.isNotEmpty()) {
                    searchQuery = ""
                    applyFilters()
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.swipeRefresh.setOnRefreshListener {
            // Don't reload - the listener is already active
           // Log.d(TAG, "🔄 Swipe refresh - listener already active")
            binding.swipeRefresh.isRefreshing = false
        }

        binding.fabCart.setOnClickListener {
            openCart()
        }
    }

    private fun applyFilters() {
        val currentFarmerId = FirebaseManager.getCurrentUserId()
        Log.d(TAG, "Current farmer ID: $currentFarmerId")

        // Filter products from OTHER farmers only
        var filteredProducts = allProducts.filter { product ->
            val isNotCurrentFarmer = product.farmerId != currentFarmerId
            val isAvailable = product.isAvailable
            isNotCurrentFarmer && isAvailable
        }

        Log.d(TAG, "After farmer filter: ${filteredProducts.size} products")

        // Apply category filter
        filteredProducts = when (currentCategory) {
            CategoryFilter.ALL -> filteredProducts
            CategoryFilter.GROCERIES -> filteredProducts.filter {
                !it.category.equals("Seeds", ignoreCase = true) &&
                        !it.category.equals("Fertilizers", ignoreCase = true)
            }
            CategoryFilter.SEEDS -> filteredProducts.filter {
                it.category.equals("Seeds", ignoreCase = true)
            }
            CategoryFilter.FERTILIZERS -> filteredProducts.filter {
                it.category.equals("Fertilizers", ignoreCase = true)
            }
        }

        Log.d(TAG, "After category filter: ${filteredProducts.size} products")

        // Apply search filter
        if (searchQuery.isNotEmpty()) {
            filteredProducts = filteredProducts.filter { product ->
                product.name.contains(searchQuery, ignoreCase = true) ||
                        product.description.contains(searchQuery, ignoreCase = true) ||
                        product.category.contains(searchQuery, ignoreCase = true) ||
                        product.farmerName.contains(searchQuery, ignoreCase = true)
            }
            Log.d(TAG, "After search filter: ${filteredProducts.size} products")
        }

        Log.d(TAG, "📤 Submitting ${filteredProducts.size} products to adapter")
        productAdapter.submitList(filteredProducts.toList()) // 🔥 Add .toList() to create new list

        // Show empty state
        if (filteredProducts.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.tvEmptyState.text = when {
                searchQuery.isNotEmpty() -> "No products found for \"$searchQuery\""
                currentCategory == CategoryFilter.ALL -> "No products available from other farmers"
                currentCategory == CategoryFilter.GROCERIES -> "No groceries available"
                currentCategory == CategoryFilter.SEEDS -> "No seeds available"
                currentCategory == CategoryFilter.FERTILIZERS -> "No fertilizers available"
                else -> "No products available"
            }
        } else {
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    private fun updateCartBadge() {
        val cartCount = CartManager.getCartCount()
        if (cartCount > 0) {
            binding.tvCartBadge.visibility = View.VISIBLE
            binding.tvCartBadge.text = cartCount.toString()
        } else {
            binding.tvCartBadge.visibility = View.GONE
        }
    }

    private fun openCart() {
        val cartFragment = FarmerCartFragment()
        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, cartFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}