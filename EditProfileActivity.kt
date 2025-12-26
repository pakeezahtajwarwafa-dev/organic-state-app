package com.example.organicstate.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.repository.AuthRepository
import com.example.organicstate.databinding.ActivityEditProfileBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        loadUserData()
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadUserData() {
        val userId = FirebaseManager.getCurrentUserId() ?: return

        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            val result = authRepository.getUserData(userId)

            withContext(Dispatchers.Main) {
                showLoading(false)

                result.onSuccess { user ->
                    binding.etName.setText(user.name)
                    binding.etEmail.setText(user.email)
                    binding.etPhone.setText(user.phone)
                    binding.etAddress.setText(user.address)
                }.onFailure { e ->
                    Toast.makeText(
                        this@EditProfileActivity,
                        "Error loading profile: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()

        // Validation
        if (name.isEmpty()) {
            binding.etName.error = "Name is required"
            return
        }

        val userId = FirebaseManager.getCurrentUserId() ?: return

        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            val updates = mapOf(
                "name" to name,
                "phone" to phone,
                "address" to address
            )

            val result = authRepository.updateProfile(userId, updates)

            withContext(Dispatchers.Main) {
                showLoading(false)

                result.onSuccess {
                    Toast.makeText(
                        this@EditProfileActivity,
                        "Profile updated successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }.onFailure { e ->
                    Toast.makeText(
                        this@EditProfileActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !show
    }
}