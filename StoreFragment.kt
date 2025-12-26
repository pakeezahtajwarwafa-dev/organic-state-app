package com.example.organicstate.ui.farmer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.model.Product
import com.example.organicstate.data.repository.ChatRepository
import com.example.organicstate.databinding.FragmentStoreBinding
import com.example.organicstate.ui.adapters.ProductAdapter
import com.example.organicstate.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

class StoreFragment : Fragment() {

    private var _binding: FragmentStoreBinding? = null
    private val binding get() = _binding!!

    private val productViewModel: ProductViewModel by viewModels()
    private val chatRepository = ChatRepository()
    private lateinit var productAdapter: ProductAdapter

    private var allProducts: List<Product> = emptyList()
    private var currentCategory: CategoryFilter = CategoryFilter.ALL
    private var searchQuery: String = ""

    companion object {
        private const val TAG = "StoreFragment"
    }

    enum class CategoryFilter {
        ALL, GROCERIES, SEEDS, FERTILIZERS
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "🔍 Fragment created")

        setupRecyclerView()
        setupObservers()
        setupListeners()
        observeUnreadChats()

        loadProducts()
    }

    private fun observeUnreadChats() {
        val userId = FirebaseManager.getCurrentUserId() ?: return

        Log.d(TAG, "🔔 Setting up unread chat observer for user: $userId")

        lifecycleScope.launch {
            chatRepository.getUnreadChatCount(userId).collect { count ->
                Log.d(TAG, "💬 Unread chat count updated: $count")

                if (count > 0) {
                    binding.tvChatBadge.text = if (count > 99) "99+" else count.toString()
                    binding.tvChatBadge.visibility = View.VISIBLE
                    Log.d(TAG, "✅ Badge shown with count: $count")
                } else {
                    binding.tvChatBadge.visibility = View.GONE
                    Log.d(TAG, "❌ Badge hidden (no unread)")
                }
            }
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            onItemClick = { product ->
                val editDialog = EditProductDialog(product)
                editDialog.onProductUpdated = {
                    loadProducts()
                }
                editDialog.show(childFragmentManager, "EditProductDialog")
            },
            onAddToCart = null
        )

        binding.rvMyProducts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productAdapter
        }
    }

    private fun setupObservers() {
        productViewModel.products.observe(viewLifecycleOwner) { products ->
            Log.d(TAG, "Products received: ${products.size}")
            allProducts = products.sortedByDescending {
                it.createdAt?.toDate()?.time ?: 0
            }
            applyFilters()
        }

        productViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupListeners() {
        Log.d(TAG, "🔧 Setting up listeners")

        // Add Product FAB
        binding.fabAddProduct.setOnClickListener {
            Log.d(TAG, "➕ Add Product FAB clicked")
            showAddProductDialog()
        }

        // AI Assistant button
        binding.btnAIAssistant.setOnClickListener {
            Log.d(TAG, "🤖 AI Assistant clicked")
            openAIChatbot()
        }

        // Farmer Chat button
        binding.btnFarmerChat.setOnClickListener {
            Log.d(TAG, "💬 Farmer Chat button clicked")
            openFarmerChats()
        }

        // Category chips
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

        // Search functionality
        binding.etSearch.setOnEditorActionListener { v, _, _ ->
            searchQuery = v.text.toString().trim()
            Log.d(TAG, "Search query: $searchQuery")
            applyFilters()
            true
        }

        // Clear search when text changes to empty
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

        // Swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            loadProducts()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun applyFilters() {
        var filteredProducts = allProducts

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

        // Apply search filter
        if (searchQuery.isNotEmpty()) {
            filteredProducts = filteredProducts.filter { product ->
                product.name.contains(searchQuery, ignoreCase = true) ||
                        product.description.contains(searchQuery, ignoreCase = true) ||
                        product.category.contains(searchQuery, ignoreCase = true)
            }
        }

        Log.d(TAG, "Displaying ${filteredProducts.size} products after filters")

        // Update UI
        productAdapter.submitList(filteredProducts)

        if (filteredProducts.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.tvEmptyState.text = when {
                searchQuery.isNotEmpty() -> "No products found for \"$searchQuery\""
                currentCategory != CategoryFilter.ALL -> "No ${currentCategory.name.lowercase()} products"
                else -> "No products yet\nTap + to add products"
            }
        } else {
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    private fun loadProducts() {
        val farmerId = FirebaseManager.getCurrentUserId() ?: return
        productViewModel.loadFarmerProducts(farmerId)
    }

    private fun showAddProductDialog() {
        val dialog = AddProductDialog()
        dialog.show(childFragmentManager, "AddProductDialog")
    }

    private fun openAIChatbot() {
        val intent = Intent(requireContext(), AIChatbotActivity::class.java)
        startActivity(intent)
    }

    private fun openFarmerChats() {
        Log.d(TAG, "🚀 openFarmerChats() called")
        try {
            val intent = Intent(requireContext(), FarmerChatsActivity::class.java)
            startActivity(intent)
            Log.d(TAG, "✅ Intent started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error starting FarmerChatsActivity", e)
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadProducts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //chatBadge = null
        _binding = null
    }
}