package com.example.organicstate.ui.customer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.organicstate.data.repository.ChatRepository
import com.example.organicstate.databinding.BottomSheetFarmerListBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class FarmerListBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFarmerListBinding? = null
    private val binding get() = _binding!!
    private val repository = ChatRepository()
    private lateinit var farmerAdapter: FarmerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFarmerListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadFarmers()
    }

    private fun setupRecyclerView() {
        farmerAdapter = FarmerAdapter { farmerId, farmerName ->
            openChat(farmerId, farmerName)
        }

        binding.rvFarmers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = farmerAdapter
        }
    }

    private fun loadFarmers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        lifecycleScope.launch {
            val result = repository.getUniqueFarmers()

            if (result.isSuccess) {
                val farmers = result.getOrNull() ?: emptyList()

                if (farmers.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyState.text = "No farmers available yet"
                } else {
                    farmerAdapter.submitList(farmers)
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Error loading farmers",
                    Toast.LENGTH_SHORT
                ).show()
            }

            binding.progressBar.visibility = View.GONE
        }
    }

    private fun openChat(farmerId: String, farmerName: String) {
        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra(ChatActivity.EXTRA_FARMER_ID, farmerId)
            putExtra(ChatActivity.EXTRA_FARMER_NAME, farmerName)
        }
        startActivity(intent)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}