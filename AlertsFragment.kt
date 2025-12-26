package com.example.organicstate.ui.customer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.model.Notification
import com.example.organicstate.data.repository.NotificationRepository
import com.example.organicstate.databinding.FragmentAlertsBinding
import com.example.organicstate.ui.adapters.NotificationAdapter
import kotlinx.coroutines.launch

class AlertsFragment : Fragment() {

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!

    private lateinit var notificationAdapter: NotificationAdapter
    private val notificationRepository = NotificationRepository()

    private var allNotifications: List<Notification> = emptyList()
    private var currentFilter: String = "all"

    companion object {
        private const val TAG = "AlertsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = FirebaseManager.getCurrentUserId()

        if (userId == null) {
            Log.e(TAG, "User not logged in")
            showError("Please login to view notifications")
            return
        }

        setupRecyclerView()
        setupCategoryFilters()
        loadNotifications()
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            onItemClick = { notification ->
                if (!notification.isRead) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        notificationRepository.markAsRead(notification.id)
                    }
                }
            }
        )

        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationAdapter
        }
    }

    private fun setupCategoryFilters() {
        binding.chipAll.setOnClickListener {
            currentFilter = "all"
            applyFilter()
        }

        binding.chipOrders.setOnClickListener {
            currentFilter = "order"
            applyFilter()
        }

        binding.chipPromotions.setOnClickListener {
            currentFilter = "promotion"
            applyFilter()
        }

        binding.chipSystem.setOnClickListener {
            currentFilter = "system"
            applyFilter()
        }

        binding.chipGeneral.setOnClickListener {
            currentFilter = "general"
            applyFilter()
        }
    }

    private fun loadNotifications() {
        val userId = FirebaseManager.getCurrentUserId() ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                notificationRepository.getNotifications(userId).collect { notifications ->
                    Log.d(TAG, "Received ${notifications.size} notifications")
                    allNotifications = notifications
                    applyFilter()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading notifications", e)
            }
        }
    }

    private fun applyFilter() {
        val filteredNotifications = if (currentFilter == "all") {
            allNotifications
        } else {
            allNotifications.filter { it.type == currentFilter }
        }

        Log.d(TAG, "Filtered to ${filteredNotifications.size} notifications (filter: $currentFilter)")

        if (filteredNotifications.isEmpty()) {
            showEmptyState()
        } else {
            showNotifications(filteredNotifications)
        }
    }

    private fun showNotifications(notifications: List<Notification>) {
        binding.tvEmptyState.visibility = View.GONE
        binding.rvNotifications.visibility = View.VISIBLE
        notificationAdapter.submitList(notifications)
    }

    private fun showEmptyState() {
        binding.tvEmptyState.visibility = View.VISIBLE
        binding.tvEmptyState.text = when (currentFilter) {
            "all" -> "No notifications"
            "order" -> "No order notifications"
            "promotion" -> "No promotions"
            "system" -> "No system alerts"
            "general" -> "No general notifications"
            else -> "No notifications"
        }
        binding.rvNotifications.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.tvEmptyState.visibility = View.VISIBLE
        binding.tvEmptyState.text = message
        binding.rvNotifications.visibility = View.GONE
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}