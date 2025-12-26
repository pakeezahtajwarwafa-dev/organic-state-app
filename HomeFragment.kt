package com.example.organicstate.ui.customer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.model.Product
import com.example.organicstate.data.repository.ChatRepository
import com.example.organicstate.databinding.FragmentHomeBinding
import com.example.organicstate.ui.adapters.ProductAdapter
import com.example.organicstate.viewmodel.CartViewModel
import com.example.organicstate.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val productViewModel: ProductViewModel by activityViewModels()
    private val cartViewModel: CartViewModel by activityViewModels()
    private val chatRepository = ChatRepository()

    private lateinit var productAdapter: ProductAdapter
    private var allProducts: List<Product> = emptyList()
    private var currentCategory: CategoryFilter = CategoryFilter.ALL
    private var searchQuery: String = ""

    companion object {
        private const val TAG = "HomeFragment"
    }

    enum class CategoryFilter {
        ALL, GROCERIES, SEEDS, FERTILIZERS
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()
        observeUnreadChats()

        productViewModel.loadAllProducts()
    }

    private fun observeUnreadChats() {
        val userId = FirebaseManager.getCurrentUserId() ?: return

        Log.d(TAG, "📢 Setting up unread chat observer for user: $userId")

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
                try {
                    val intent = Intent(requireContext(), ProductDetailActivity::class.java)
                    intent.putExtra("product_id", product.id)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to product detail", e)
                    Toast.makeText(requireContext(), "Error opening product", Toast.LENGTH_SHORT).show()
                }
            },
            onAddToCart = { product ->
                try {
                    cartViewModel.addToCart(product)
                    Toast.makeText(requireContext(), "${product.name} added to cart", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding to cart", e)
                    Toast.makeText(requireContext(), "Error adding to cart", Toast.LENGTH_SHORT).show()
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
            Log.d(TAG, "📦 Products received: ${products.size}")

            allProducts = products.sortedByDescending {
                it.createdAt?.toDate()?.time ?: 0
            }

            applyFilters()
        }

        productViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        productViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                Log.e(TAG, "ViewModel error: $errorMessage")
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.tvEmptyState.text = "Error: $errorMessage"
            }
        }
    }

    private fun setupListeners() {
        // NEW: Recipe AI Button
        binding.btnRecipeAI.setOnClickListener {
            try {
                val intent = Intent(requireContext(), RecipeGeneratorActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error opening Recipe AI", e)
                Toast.makeText(requireContext(), "Error opening Recipe AI", Toast.LENGTH_SHORT).show()
            }
        }

        // Existing Chat Button
        binding.btnChat.setOnClickListener {
            showFarmerListDialog()
        }

        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            currentCategory = when (checkedIds.firstOrNull()) {
                binding.chipAll.id -> CategoryFilter.ALL
                binding.chipGroceries.id -> CategoryFilter.GROCERIES
                binding.chipSeeds.id -> CategoryFilter.SEEDS
                binding.chipFertilizers.id -> CategoryFilter.FERTILIZERS
                else -> CategoryFilter.ALL
            }
            applyFilters()
        }

        binding.etSearch.setOnEditorActionListener { v, _, _ ->
            searchQuery = v.text.toString().trim()
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
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun showFarmerListDialog() {
        val dialog = FarmerListBottomSheet()
        dialog.show(childFragmentManager, "FarmerListBottomSheet")
    }

    private fun applyFilters() {
        var filteredProducts = allProducts

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

        if (searchQuery.isNotEmpty()) {
            filteredProducts = filteredProducts.filter { product ->
                product.name.contains(searchQuery, ignoreCase = true) ||
                        product.description.contains(searchQuery, ignoreCase = true) ||
                        product.category.contains(searchQuery, ignoreCase = true) ||
                        product.farmerName.contains(searchQuery, ignoreCase = true)
            }
        }

        productAdapter.submitList(filteredProducts.toList())
        binding.tvEmptyState.visibility = if (filteredProducts.isEmpty()) View.VISIBLE else View.GONE

        if (filteredProducts.isEmpty()) {
            binding.tvEmptyState.text = when {
                searchQuery.isNotEmpty() -> "No products found for \"$searchQuery\""
                currentCategory == CategoryFilter.GROCERIES -> "No groceries available"
                currentCategory == CategoryFilter.SEEDS -> "No seeds available"
                currentCategory == CategoryFilter.FERTILIZERS -> "No fertilizers available"
                else -> "No products available"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}