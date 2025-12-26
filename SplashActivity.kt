package com.example.organicstate.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.organicstate.R
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.repository.AuthRepository
import com.example.organicstate.ui.admin.AdminDashboardActivity
import com.example.organicstate.ui.customer.CustomerMainActivity
import com.example.organicstate.ui.farmer.FarmerMainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Check authentication status after 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthStatus()
        }, 2000)
    }

    private fun checkAuthStatus() {
        val currentUser = FirebaseManager.getCurrentUser()

        if (currentUser != null) {
            // User is logged in, get their role
            CoroutineScope(Dispatchers.IO).launch {
                val role = authRepository.getUserRole(currentUser.uid)
                withContext(Dispatchers.Main) {
                    navigateBasedOnRole(role)
                }
            }
        } else {
            // User not logged in, go to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun navigateBasedOnRole(role: String?) {
        val intent = when (role) {
            "customer" -> Intent(this, CustomerMainActivity::class.java)
            "farmer" -> Intent(this, FarmerMainActivity::class.java)
            "admin" -> Intent(this, AdminDashboardActivity::class.java)
            else -> Intent(this, LoginActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}