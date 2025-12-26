package com.example.organicstate.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.organicstate.databinding.ActivityRegisterFormBinding
import com.example.organicstate.ui.admin.AdminDashboardActivity
import com.example.organicstate.ui.customer.CustomerMainActivity
import com.example.organicstate.ui.farmer.FarmerMainActivity
import com.example.organicstate.viewmodel.AuthState
import com.example.organicstate.viewmodel.AuthViewModel

class RegisterFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterFormBinding
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var selectedRole: String

    companion object {
        private const val TAG = "RegisterFormActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedRole = intent.getStringExtra(RoleSelectionActivity.EXTRA_ROLE) ?: ""

        if (selectedRole.isEmpty()) {
            Toast.makeText(this, "Role not selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupObservers()
        setupListeners()
    }

    private fun setupUI() {
        // Show/hide phone and address fields based on role
        if (selectedRole == "admin") {
            binding.tilPhone.visibility = View.GONE
            binding.tilAddress.visibility = View.GONE
            binding.tvRoleInfo.text = "Admin Account"
        } else {
            binding.tilPhone.visibility = View.VISIBLE
            binding.tilAddress.visibility = View.VISIBLE
            binding.tvRoleInfo.text = when (selectedRole) {
                "customer" -> "Customer Account"
                "farmer" -> "Farmer Account"
                else -> "Account"
            }
        }
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()

            if (validateInput(name, email, password, confirmPassword, phone, address)) {
                Log.d(TAG, "Registering with role: $selectedRole")
                viewModel.register(email, password, name, selectedRole, phone, address)
            }
        }

        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    private fun setupObservers() {
        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> {
                    showLoading(true)
                }
                is AuthState.Success -> {
                    showLoading(false)
                    Toast.makeText(this, "Welcome to Organic State!", Toast.LENGTH_SHORT).show()
                    navigateBasedOnRole(state.role)
                }
                is AuthState.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
                else -> {
                    showLoading(false)
                }
            }
        }
    }

    private fun validateInput(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        phone: String,
        address: String
    ): Boolean {
        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            return false
        }
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Invalid email format"
            return false
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Password is required"
            return false
        }
        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            return false
        }
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            return false
        }

        // Validate phone and address only for customer/farmer
        if (selectedRole != "admin") {
            if (phone.isEmpty()) {
                binding.etPhone.error = "Phone number is required"
                return false
            }
            if (phone.length < 10) {
                binding.etPhone.error = "Invalid phone number"
                return false
            }
            if (address.isEmpty()) {
                binding.etAddress.error = "Address is required"
                return false
            }
        }

        return true
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !show
    }

    private fun navigateBasedOnRole(role: String) {
        val intent = when (role.lowercase()) {
            "customer" -> Intent(this, CustomerMainActivity::class.java)
            "farmer" -> Intent(this, FarmerMainActivity::class.java)
            "admin" -> Intent(this, AdminDashboardActivity::class.java)
            else -> {
                Toast.makeText(this, "Unknown role", Toast.LENGTH_SHORT).show()
                return
            }
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}