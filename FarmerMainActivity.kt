package com.example.organicstate.ui.farmer

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.organicstate.R
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.repository.NotificationRepository
import com.example.organicstate.databinding.ActivityFarmerMainBinding
import com.example.organicstate.ui.customer.AlertsFragment
import com.example.organicstate.ui.customer.ProfileFragment
import com.google.android.material.badge.BadgeDrawable
import kotlinx.coroutines.launch

class FarmerMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFarmerMainBinding
    private val notificationRepository = NotificationRepository()
    private var alertsBadge: BadgeDrawable? = null

    companion object {
        private const val TAG = "FarmerMainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFarmerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        setupNotificationBadge()

        // Load store fragment by default
        if (savedInstanceState == null) {
            loadFragment(StoreFragment())
        }

        observeUnreadNotifications()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_store -> StoreFragment()
                R.id.nav_buy -> BuyFragment()
                R.id.nav_orders -> OrdersFragment()
                R.id.nav_alerts -> {
                    clearNotificationBadge()
                    AlertsFragment()
                }
                R.id.nav_profile -> ProfileFragment()
                else -> null
            }

            fragment?.let {
                loadFragment(it)
                true
            } ?: false
        }
    }

    private fun setupNotificationBadge() {
        try {
            alertsBadge = binding.bottomNavigation.getOrCreateBadge(R.id.nav_alerts)
            alertsBadge?.apply {
                backgroundColor = getColor(R.color.error_red)
                badgeGravity = BadgeDrawable.TOP_END
                isVisible = false
            }
            Log.d(TAG, "Badge setup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up badge", e)
        }
    }

    private fun observeUnreadNotifications() {
        val userId = FirebaseManager.getCurrentUserId() ?: return

        lifecycleScope.launch {
            notificationRepository.getUnreadCount(userId).collect { count ->
                Log.d(TAG, "Unread count updated: $count")
                if (count > 0) {
                    alertsBadge?.apply {
                        number = count
                        isVisible = true
                    }
                } else {
                    alertsBadge?.isVisible = false
                }
            }
        }
    }

    private fun clearNotificationBadge() {
        alertsBadge?.isVisible = false
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}