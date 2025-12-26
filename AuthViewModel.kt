package com.example.organicstate.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.organicstate.data.repository.AuthRepository
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val userId: String, val role: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class PasswordResetState {
    object Idle : PasswordResetState()
    object Loading : PasswordResetState()
    object Success : PasswordResetState()
    data class Error(val message: String) : PasswordResetState()
}

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    private val _passwordResetState = MutableLiveData<PasswordResetState>(PasswordResetState.Idle)
    val passwordResetState: LiveData<PasswordResetState> = _passwordResetState

    companion object {
        private const val TAG = "AuthViewModel"
    }

    fun register(
        email: String,
        password: String,
        name: String,
        role: String,
        phone: String = "",
        address: String = ""
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            Log.d(TAG, "Starting registration: email=$email, role=$role")

            val result = repository.register(email, password, name, role, phone, address)

            if (result.isSuccess) {
                val userId = result.getOrNull() ?: ""
                Log.d(TAG, "Registration successful: userId=$userId")
                _authState.value = AuthState.Success(userId, role)
            } else {
                val error = result.exceptionOrNull()?.message ?: "Registration failed"
                Log.e(TAG, "Registration failed: $error")
                _authState.value = AuthState.Error(error)
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            Log.d(TAG, "Starting login: email=$email")

            val result = repository.login(email, password)

            if (result.isSuccess) {
                val userId = result.getOrNull() ?: ""
                Log.d(TAG, "Login successful: userId=$userId")

                // Get user role
                val role = repository.getUserRole(userId)
                if (role != null) {
                    Log.d(TAG, "User role: $role")
                    _authState.value = AuthState.Success(userId, role)
                } else {
                    Log.e(TAG, "Failed to get user role")
                    _authState.value = AuthState.Error("Failed to retrieve user role")
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "Login failed"
                Log.e(TAG, "Login failed: $error")
                _authState.value = AuthState.Error(error)
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _passwordResetState.value = PasswordResetState.Loading
            Log.d(TAG, "Sending password reset email to: $email")

            val result = repository.sendPasswordResetEmail(email)

            if (result.isSuccess) {
                Log.d(TAG, "Password reset email sent successfully")
                _passwordResetState.value = PasswordResetState.Success
            } else {
                val error = result.exceptionOrNull()?.message ?: "Failed to send reset email"
                Log.e(TAG, "Password reset failed: $error")
                _passwordResetState.value = PasswordResetState.Error(error)
            }
        }
    }

    fun logout() {
        repository.logout()
        _authState.value = AuthState.Idle
    }
}