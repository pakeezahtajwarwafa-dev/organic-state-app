package com.example.organicstate.ui.customer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.organicstate.databinding.FragmentOffersBinding
import com.example.organicstate.ui.adapters.ProductAdapter
import com.example.organicstate.viewmodel.ProductViewModel

class OffersFragment : Fragment() {

    private var _binding: FragmentOffersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductViewModel by viewModels()
    private lateinit var adapter: ProductAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOffersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ProductAdapter(
            onItemClick = { product ->
                val intent = Intent(requireContext(), ProductDetailActivity::class.java)
                intent.putExtra("product_id", product.id)
                startActivity(intent)
            },
            onAddToCart = { product -> /* Cart logic */ }
        )

        binding.rvOffers.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvOffers.adapter = adapter

        viewModel.products.observe(viewLifecycleOwner) { products ->
            adapter.submitList(products)
            // FIXED: Changed tvNoOffers to tvEmptyState to match your XML
            binding.tvEmptyState.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadPromotedProducts()
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.loadPromotedProducts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}