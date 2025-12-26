package com.example.organicstate.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.organicstate.databinding.ActivityRoleSelectionBinding

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding
    private var selectedRole: String = ""

    companion object {
        private const val ADMIN_PASSWORD = "admin123" // Same secret password
        const val EXTRA_ROLE = "selected_role"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.cardCustomer.setOnClickListener {
            selectRole("customer")
        }

        binding.cardFarmer.setOnClickListener {
            selectRole("farmer")
        }

        // SECRET: Long press on title to reveal admin option (KEPT ORIGINAL CODE)
        binding.tvTitle.setOnLongClickListener {
            showAdminPasswordDialog()
            true
        }

        binding.btnContinue.setOnClickListener {
            if (selectedRole.isEmpty()) {
                Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show()
            } else {
                // Navigate to registration form with selected role
                val intent = Intent(this, RegisterFormActivity::class.java)
                intent.putExtra(EXTRA_ROLE, selectedRole)
                startActivity(intent)
            }
        }

        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    private fun selectRole(role: String) {
        selectedRole = role

        // Reset both cards
        binding.cardCustomer.strokeWidth = 0
        binding.cardFarmer.strokeWidth = 0

        // Highlight selected card
        when (role) {
            "customer" -> binding.cardCustomer.strokeWidth = 4
            "farmer" -> binding.cardFarmer.strokeWidth = 4
            "admin" -> {
                // Admin selected - no visual selection needed
                binding.cardCustomer.strokeWidth = 0
                binding.cardFarmer.strokeWidth = 0
            }
        }

        binding.btnContinue.isEnabled = true
    }

    // SECRET ADMIN PASSWORD DIALOG (ORIGINAL CODE KEPT)
    private fun showAdminPasswordDialog() {
        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

        AlertDialog.Builder(this)
            .setTitle("Admin Access")
            .setMessage("Enter admin password:")
            .setView(input)
            .setPositiveButton("Continue") { _, _ ->
                val enteredPassword = input.text.toString()
                if (enteredPassword == ADMIN_PASSWORD) {
                    selectRole("admin")
                    Toast.makeText(this, "Admin role selected", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Incorrect admin password", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}