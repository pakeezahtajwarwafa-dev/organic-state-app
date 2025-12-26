package com.example.organicstate.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.organicstate.R
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.databinding.ActivityAdminDashboardBinding
import com.example.organicstate.ui.auth.LoginActivity

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        // User Management
        binding.cardUserManagement.setOnClickListener {
            loadFragment(UserManagementFragment())
        }

        // Store Monitoring
        binding.cardStoreMonitoring.setOnClickListener {
            loadFragment(StoreMonitoringFragment())
        }

        // Complaints
        binding.cardComplaints.setOnClickListener {
            loadFragment(ComplaintsFragment())
        }

        // Broadcast Notifications
        binding.cardBroadcast.setOnClickListener {
            showBroadcastDialog()
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun loadFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showBroadcastDialog() {
        val dialog = BroadcastNotificationDialog()
        dialog.show(supportFragmentManager, "BroadcastDialog")
    }

    private fun logout() {
        FirebaseManager.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }
}