package com.example.organicstate.ui.farmer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.databinding.ActivityFarmerChatsBinding
import com.example.organicstate.ui.customer.ChatActivity

class FarmerChatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFarmerChatsBinding
    private val viewModel: ChatListViewModel by viewModels()
    private lateinit var chatListAdapter: ChatListAdapter
    private lateinit var currentUserId: String

    companion object {
        private const val TAG = "FarmerChatsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFarmerChatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = FirebaseManager.getCurrentUserId() ?: ""
        if (currentUserId.isEmpty()) {
            Log.e(TAG, "❌ User not logged in")
            finish()
            return
        }

        Log.d(TAG, "🚀 FarmerChatsActivity started for user: $currentUserId")

        setupUI()
        setupObservers()
        loadChats()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        chatListAdapter = ChatListAdapter { chat ->
            Log.d(TAG, "💬 Opening chat:")
            Log.d(TAG, "   Chat ID: ${chat.id}")
            Log.d(TAG, "   Customer: ${chat.customerName} (${chat.customerId})")
            Log.d(TAG, "   Farmer: ${chat.farmerName} (${chat.farmerId})")
            Log.d(TAG, "   Current user (farmer): $currentUserId")

            // Open chat with customer
            // Pass customer info as the "other user"
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra(ChatActivity.EXTRA_FARMER_ID, chat.customerId)
                putExtra(ChatActivity.EXTRA_FARMER_NAME, chat.customerName)
                // NO NEED for EXTRA_IS_FARMER_VIEW - the activity will detect role from database
            }
            startActivity(intent)
        }

        binding.rvChats.apply {
            layoutManager = LinearLayoutManager(this@FarmerChatsActivity)
            adapter = chatListAdapter
        }
    }

    private fun setupObservers() {
        viewModel.chats.observe(this) { chats ->
            Log.d(TAG, "📋 Received ${chats.size} chats")
            chats.forEachIndexed { index, chat ->
                Log.d(TAG, "   ${index + 1}. ${chat.customerName} ↔️ ${chat.farmerName}")
            }

            chatListAdapter.submitList(chats)
            binding.tvEmptyState.visibility = if (chats.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun loadChats() {
        Log.d(TAG, "📥 Loading chats for farmer: $currentUserId")
        viewModel.loadUserChats(currentUserId)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - refreshing chats")
        loadChats()
    }
}
