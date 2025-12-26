package com.example.organicstate.ui.customer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.repository.AuthRepository
import com.example.organicstate.databinding.FragmentProfileBinding
import com.example.organicstate.ui.auth.LoginActivity
import com.example.organicstate.ui.dialogs.ComplaintDialog
import com.example.organicstate.ui.farmer.FarmerMainActivity
import com.example.organicstate.ui.farmer.StatsFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val authRepository = AuthRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserData()
        setupListeners()
        setupUIBasedOnUserRole()
    }

    private fun setupUIBasedOnUserRole() {
        // Check if this is a farmer
        val activity = requireActivity()

        if (activity is FarmerMainActivity) {
            // For farmers: Show "Sales Analytics" instead of "Order History"
            binding.btnOrderHistory.visibility = View.GONE
            binding.btnSalesAnalytics.visibility = View.VISIBLE
        } else {
            // For customers: Show "Order History"
            binding.btnOrderHistory.visibility = View.VISIBLE
            binding.btnSalesAnalytics.visibility = View.GONE
        }
    }

    private fun loadUserData() {
        val userId = FirebaseManager.getCurrentUserId() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val result = authRepository.getUserData(userId)
            result.onSuccess { user ->
                withContext(Dispatchers.Main) {
                    binding.tvUserName.text = user.name
                    binding.tvUserEmail.text = user.email
                    binding.tvUserPhone.text = user.phone.ifEmpty { "Not provided" }
                    binding.tvUserAddress.text = user.address.ifEmpty { "Not provided" }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(requireContext(), com.example.organicstate.ui.profile.EditProfileActivity::class.java)
            startActivity(intent)
        }

        // For Customers: Order History
        binding.btnOrderHistory.setOnClickListener {
            showOrderHistory()
        }

        // For Farmers: Sales Analytics
        binding.btnSalesAnalytics.setOnClickListener {
            showSalesAnalytics()
        }

        // File Complaint
        binding.btnFileComplaint.setOnClickListener {
            showComplaintDialog()
        }

        binding.btnSettings.setOnClickListener {
            val intent = Intent(requireContext(), com.example.organicstate.ui.profile.SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun showOrderHistory() {
        val fragment = CustomerOrdersFragment()
        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }

    // NEW: Show sales analytics for farmers
    private fun showSalesAnalytics() {
        val fragment = StatsFragment()
        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showComplaintDialog() {
        val dialog = ComplaintDialog()
        dialog.show(childFragmentManager, "ComplaintDialog")
    }

    private fun logout() {
        FirebaseManager.signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}