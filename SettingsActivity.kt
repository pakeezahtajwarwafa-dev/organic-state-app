package com.example.organicstate.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.organicstate.R
import com.example.organicstate.databinding.ActivitySettingsBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        setupListeners()
        loadSettings()
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmation()
        }

        binding.switchOrderNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("order_notifications", isChecked).apply()
            Toast.makeText(
                this,
                if (isChecked) "Order notifications enabled" else "Order notifications disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.switchPromotionNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("promotion_notifications", isChecked).apply()
            Toast.makeText(
                this,
                if (isChecked) "Promotion notifications enabled" else "Promotion notifications disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnAbout.setOnClickListener {
            showAboutDialog()
        }

        binding.btnPrivacyPolicy.setOnClickListener {
            showPrivacyPolicyDialog()
        }
    }

    private fun loadSettings() {
        binding.switchOrderNotifications.isChecked = prefs.getBoolean("order_notifications", true)
        binding.switchPromotionNotifications.isChecked = prefs.getBoolean("promotion_notifications", true)
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)

        val etCurrentPassword = dialogView.findViewById<TextInputEditText>(R.id.etCurrentPassword)
        val etNewPassword = dialogView.findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<TextInputEditText>(R.id.etConfirmPassword)

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change") { _, _ ->
                val currentPassword = etCurrentPassword.text.toString().trim()
                val newPassword = etNewPassword.text.toString().trim()
                val confirmPassword = etConfirmPassword.text.toString().trim()

                // Validation
                when {
                    currentPassword.isEmpty() -> {
                        Toast.makeText(this, "Enter current password", Toast.LENGTH_SHORT).show()
                    }
                    newPassword.isEmpty() -> {
                        Toast.makeText(this, "Enter new password", Toast.LENGTH_SHORT).show()
                    }
                    newPassword.length < 6 -> {
                        Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    }
                    newPassword != confirmPassword -> {
                        Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        changePassword(currentPassword, newPassword)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null || user.email == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Re-authenticate user before changing password
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                // Now update the password
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Password changed successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Failed to change password: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Current password is incorrect",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                Toast.makeText(this, "Account deletion coming soon", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About Organic State")
            .setMessage("""
                Organic State - Agricultural Marketplace
                Version 1.0.0
                
                Connect farmers directly with customers for fresh organic produce.
                
                Features:
                • Buy fresh organic products
                • Sell your farm products
                • Real-time order tracking
                • Secure payments
                
                © 2025 Organic State. All rights reserved.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showPrivacyPolicyDialog() {
        AlertDialog.Builder(this)
            .setTitle("Privacy Policy")
            .setMessage("""
                Privacy Policy
                
                We value your privacy and are committed to protecting your personal information.
                
                Information We Collect:
                • Name, email, phone number
                • Delivery address
                • Order history
                • Product listings (for farmers)
                
                How We Use Your Information:
                • Process orders
                • Facilitate communication
                • Improve our services
                • Send order updates
                
                Your information is never shared with third parties without your consent.
                
                For questions, contact: support@organicstate.com
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }
}