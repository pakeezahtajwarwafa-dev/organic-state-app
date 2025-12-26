package com.example.organicstate.data.repository

import android.util.Log
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.model.User
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseManager.auth
    private val db = FirebaseManager.db

    companion object {
        private const val TAG = "AuthRepository"
    }

    // Register new user with optional phone and address
    suspend fun register(
        email: String,
        password: String,
        name: String,
        role: String,
        phone: String = "",
        address: String = ""
    ): Result<String> {
        return try {
            Log.d(TAG, "Starting registration for email: $email, role: $role")

            // Create auth user
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("User ID is null")

            Log.d(TAG, "Firebase Auth user created with ID: $userId")

            // Create user document in Firestore
            val user = User(
                id = userId,
                name = name,
                email = email,
                role = role,
                phone = phone,
                address = address
            )

            Log.d(TAG, "Creating Firestore document for user: $userId with phone: $phone, address: $address")

            db.collection("users")
                .document(userId)
                .set(user.toMap())
                .await()

            Log.d(TAG, "Firestore document created successfully")

            Result.success(userId)
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "Firebase Auth error: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Registration failed"))
        } catch (e: Exception) {
            Log.e(TAG, "Registration error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Login user
    suspend fun login(email: String, password: String): Result<String> {
        return try {
            Log.d(TAG, "Starting login for email: $email")
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("User ID is null")
            Log.d(TAG, "Login successful for user: $userId")
            Result.success(userId)
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "Login error: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Login failed"))
        }
    }

    // Get user data
    suspend fun getUserData(userId: String): Result<User> {
        return try {
            Log.d(TAG, "Fetching user data for: $userId")
            val doc = db.collection("users").document(userId).get().await()

            if (!doc.exists()) {
                Log.e(TAG, "User document not found for: $userId")
                throw Exception("User not found")
            }

            val user = doc.toObject(User::class.java)?.copy(id = doc.id)
                ?: throw Exception("Failed to parse user data")

            Log.d(TAG, "User data fetched: ${user.name}, role: ${user.role}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user data: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Get user role
    suspend fun getUserRole(userId: String): String? {
        return try {
            Log.d(TAG, "Fetching user role for: $userId")
            val doc = db.collection("users").document(userId).get().await()
            val role = doc.getString("role")
            Log.d(TAG, "User role fetched: $role")
            role
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user role: ${e.message}", e)
            null
        }
    }

    // Update user profile
    suspend fun updateProfile(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            Log.d(TAG, "Updating profile for user: $userId")
            db.collection("users")
                .document(userId)
                .update(updates)
                .await()
            Log.d(TAG, "Profile updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Send password reset email
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            Log.d(TAG, "Sending password reset email to: $email")
            auth.sendPasswordResetEmail(email).await()
            Log.d(TAG, "Password reset email sent successfully")
            Result.success(Unit)
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "Password reset error: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Failed to send reset email"))
        } catch (e: Exception) {
            Log.e(TAG, "Password reset error: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Logout
    fun logout() {
        Log.d(TAG, "User logged out")
        auth.signOut()
    }
}