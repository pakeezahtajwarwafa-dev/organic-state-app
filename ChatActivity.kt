package com.example.organicstate.ui.customer

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.repository.ChatRepository
import com.example.organicstate.databinding.ActivityChatBinding
import com.example.organicstate.ui.adapters.MessageAdapter
import com.example.organicstate.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var messageAdapter: MessageAdapter
    private val repository = ChatRepository()

    private lateinit var chatId: String
    private lateinit var otherUserId: String
    private lateinit var otherUserName: String
    private lateinit var currentUserId: String
    private lateinit var currentUserName: String

    companion object {
        private const val TAG = "ChatActivity"
        const val EXTRA_FARMER_ID = "farmer_id"
        const val EXTRA_FARMER_NAME = "farmer_name"
        const val EXTRA_IS_FARMER_VIEW = "is_farmer_view"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get other user info from intent
        otherUserId = intent.getStringExtra(EXTRA_FARMER_ID) ?: ""
        otherUserName = intent.getStringExtra(EXTRA_FARMER_NAME) ?: ""

        if (otherUserId.isEmpty() || otherUserName.isEmpty()) {
            Toast.makeText(this, "Error: Invalid user information", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Get current user info
        currentUserId = FirebaseManager.getCurrentUserId() ?: ""
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d(TAG, "=== ChatActivity Started ===")
        Log.d(TAG, "Current User ID: $currentUserId")
        Log.d(TAG, "Other User ID: $otherUserId")
        Log.d(TAG, "Other User Name: $otherUserName")

        getUserInfoAndSetup()
    }

    private fun getUserInfoAndSetup() {
        lifecycleScope.launch {
            try {
                // Get current user info from Firestore
                Log.d(TAG, "Fetching current user info from Firestore...")
                val userDoc = FirebaseManager.db.collection("users")
                    .document(currentUserId)
                    .get()
                    .await()

                if (!userDoc.exists()) {
                    Log.e(TAG, "❌ User document does not exist!")
                    Toast.makeText(this@ChatActivity, "User data not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                currentUserName = userDoc.getString("name") ?: "User"
                val currentUserRole = userDoc.getString("role") ?: "customer"

                Log.d(TAG, "✅ Current user loaded:")
                Log.d(TAG, "   Name: $currentUserName")
                Log.d(TAG, "   Role: $currentUserRole")
                Log.d(TAG, "   ID: $currentUserId")
                Log.d(TAG, "")
                Log.d(TAG, "Other user:")
                Log.d(TAG, "   Name: $otherUserName")
                Log.d(TAG, "   ID: $otherUserId")
                Log.d(TAG, "")

                // Determine who is customer and who is farmer based on ACTUAL role
                val (customerId, customerName, farmerId, farmerName) = if (currentUserRole == "farmer") {
                    // I am the farmer, the other person is the customer
                    Log.d(TAG, "📋 Role Assignment: Current user is FARMER, other user is CUSTOMER")
                    Quadruple(otherUserId, otherUserName, currentUserId, currentUserName)
                } else {
                    // I am the customer, the other person is the farmer
                    Log.d(TAG, "📋 Role Assignment: Current user is CUSTOMER, other user is FARMER")
                    Quadruple(currentUserId, currentUserName, otherUserId, otherUserName)
                }

                Log.d(TAG, "")
                Log.d(TAG, "🎯 Final Chat Configuration:")
                Log.d(TAG, "   Customer: $customerName (ID: $customerId)")
                Log.d(TAG, "   Farmer: $farmerName (ID: $farmerId)")
                Log.d(TAG, "")

                // Get or create chat with correct IDs
                Log.d(TAG, "Creating/fetching chat...")
                val result = repository.getOrCreateChat(
                    customerId = customerId,
                    customerName = customerName,
                    farmerId = farmerId,
                    farmerName = farmerName
                )

                if (result.isSuccess) {
                    chatId = result.getOrNull() ?: ""
                    Log.d(TAG, "✅ Chat ready! Chat ID: $chatId")
                    Log.d(TAG, "===================")

                    setupUI()
                    setupObservers()
                    loadMessages()
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "❌ Failed to create/fetch chat: ${error?.message}", error)
                    Toast.makeText(
                        this@ChatActivity,
                        "Error creating chat: ${error?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in getUserInfoAndSetup", e)
                Toast.makeText(
                    this@ChatActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun setupUI() {
        Log.d(TAG, "Setting up UI...")

        // Set other user's name in toolbar
        binding.tvFarmerName.text = otherUserName

        // Setup RecyclerView
        messageAdapter = MessageAdapter(currentUserId)
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true  // Start from bottom
            }
            adapter = messageAdapter
        }

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Send button
        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        // Mark messages as read when activity opens
        if (::chatId.isInitialized) {
            viewModel.markAsRead(chatId, currentUserId)
        }

        Log.d(TAG, "✅ UI setup complete")
    }

    private fun setupObservers() {
        Log.d(TAG, "Setting up observers...")

        viewModel.messages.observe(this) { messages ->
            Log.d(TAG, "📬 Received ${messages.size} messages")
            messageAdapter.submitList(messages) {
                // Scroll to bottom after list is updated
                if (messages.isNotEmpty()) {
                    binding.rvMessages.scrollToPosition(messages.size - 1)
                }
            }

            // Update empty state
            binding.tvEmptyState.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            Log.d(TAG, "Loading state: $isLoading")
        }

        viewModel.error.observe(this) { errorMessage ->
            if (errorMessage != null) {
                Log.e(TAG, "❌ Error from ViewModel: $errorMessage")
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.messageSent.observe(this) { sent ->
            if (sent) {
                Log.d(TAG, "✅ Message sent successfully")
                binding.etMessage.text?.clear()
            }
        }

        Log.d(TAG, "✅ Observers setup complete")
    }

    private fun loadMessages() {
        if (!::chatId.isInitialized) {
            Log.e(TAG, "❌ Cannot load messages: chatId not initialized")
            return
        }

        Log.d(TAG, "📥 Loading messages for chat: $chatId")
        viewModel.loadMessages(chatId)
    }

    private fun sendMessage() {
        val messageText = binding.etMessage.text.toString().trim()

        if (messageText.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
            return
        }

        if (!::chatId.isInitialized) {
            Log.e(TAG, "❌ Cannot send message: chatId not initialized")
            Toast.makeText(this, "Chat not ready", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "📤 Sending message:")
        Log.d(TAG, "   Chat ID: $chatId")
        Log.d(TAG, "   Sender: $currentUserName ($currentUserId)")
        Log.d(TAG, "   Message: $messageText")

        viewModel.sendMessage(
            chatId = chatId,
            senderId = currentUserId,
            senderName = currentUserName,
            messageText = messageText
        )
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() called")

        // Mark messages as read when returning to chat
        if (::chatId.isInitialized) {
            viewModel.markAsRead(chatId, currentUserId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ChatActivity destroyed")
    }
}

// Helper data class
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)