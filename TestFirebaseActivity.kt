package com.example.organicstate.ui.auth

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.organicstate.R
import com.example.organicstate.data.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TestFirebaseActivity : AppCompatActivity() {

    private lateinit var authRepo: AuthRepository
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_firebase)

        authRepo = AuthRepository()
        statusText = findViewById(R.id.statusText)

        findViewById<Button>(R.id.btnTestConnection).setOnClickListener {
            testConnection()
        }
    }

    private fun testConnection() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                statusText.text = "Testing connection..."

                // Test Firestore write
                val db = FirebaseFirestore.getInstance()
                db.collection("test")
                    .document("connection")
                    .set(mapOf("timestamp" to System.currentTimeMillis()))
                    .await()

                statusText.text = "✅ Firebase connected successfully!"
                Toast.makeText(this@TestFirebaseActivity, "Success!", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                statusText.text = "❌ Error: ${e.message}"
                Toast.makeText(this@TestFirebaseActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}