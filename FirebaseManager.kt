package com.example.organicstate.data.local

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object FirebaseManager {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    val storage: FirebaseStorage = FirebaseStorage.getInstance()

    // Get current user
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    // Get current user ID
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // Check if user is logged in
    fun isLoggedIn(): Boolean = getCurrentUser() != null

    // Sign out
    fun signOut() {
        auth.signOut()
    }
}
