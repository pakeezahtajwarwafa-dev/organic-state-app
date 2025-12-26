package com.example.organicstate.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.organicstate.databinding.ActivityForgotPasswordBinding
import com.example.organicstate.viewmodel.AuthViewModel
import com.example.organicstate.viewmodel.PasswordResetState

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnSendReset.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            if (validateEmail(email)) {
                viewModel.sendPasswordResetEmail(email)
            }
        }

        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.tvBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun setupObservers() {
        viewModel.passwordResetState.observe(this) { state ->
            when (state) {
                is PasswordResetState.Idle -> {
                    showLoading(false)
                }
                is PasswordResetState.Loading -> {
                    showLoading(true)
                }
                is PasswordResetState.Success -> {
                    showLoading(false)
                    showSuccessState()
                }
                is PasswordResetState.Error -> {
                    showLoading(false)
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun validateEmail(email: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Invalid email format"
            return false
        }
        return true
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSendReset.isEnabled = !show
    }

    private fun showSuccessState() {
        // Hide form, show success message
        binding.layoutForm.visibility = View.GONE
        binding.layoutSuccess.visibility = View.VISIBLE

        Toast.makeText(
            this,
            "Password reset email sent! Check your inbox.",
            Toast.LENGTH_LONG
        ).show()
    }
}