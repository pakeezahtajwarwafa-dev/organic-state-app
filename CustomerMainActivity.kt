package com.example.organicstate.ui.customer

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.organicstate.R
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.repository.NotificationRepository
import com.example.organicstate.databinding.ActivityCustomerMainBinding
import com.google.android.material.badge.BadgeDrawable
import kotlinx.coroutines.launch

class CustomerMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerMainBinding
    private val notificationRepository = NotificationRepository()
    private var alertsBadge: BadgeDrawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        setupNotificationBadge()

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        observeUnreadNotifications()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment: Fragment? = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_cart -> CartFragment()
                R.id.nav_offers -> OffersFragment() // Make sure OffersFragment.kt exists!
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
                isVisible = false
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Badge error", e)
        }
    }

    private fun observeUnreadNotifications() {
        val userId = FirebaseManager.getCurrentUserId() ?: return
        lifecycleScope.launch {
            notificationRepository.getUnreadCount(userId).collect { count ->
                alertsBadge?.apply {
                    number = count
                    isVisible = count > 0
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