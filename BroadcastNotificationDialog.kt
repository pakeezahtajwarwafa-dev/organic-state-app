package com.example.organicstate.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.organicstate.data.model.Notification
import com.example.organicstate.data.repository.NotificationRepository
import com.example.organicstate.databinding.DialogBroadcastNotificationBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class BroadcastNotificationDialog : DialogFragment() {

    private var _binding: DialogBroadcastNotificationBinding? = null
    private val binding get() = _binding!!

    private val notificationRepository = NotificationRepository()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogBroadcastNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupListeners()
    }

    private fun setupSpinners() {
        val audiences = arrayOf("All Users", "Customers Only", "Farmers Only")
        val audienceAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            audiences
        )
        binding.spinnerAudience.adapter = audienceAdapter

        val types = arrayOf("Promotion", "System", "General")
        val typeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            types
        )
        binding.spinnerType.adapter = typeAdapter
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSend.setOnClickListener {
            sendBroadcast()
        }
    }

    private fun sendBroadcast() {
        val title = binding.etTitle.text.toString().trim()
        val message = binding.etMessage.text.toString().trim()
        val audience = binding.spinnerAudience.selectedItem.toString()
        val type = binding.spinnerType.selectedItem.toString().lowercase()

        if (title.isEmpty()) {
            binding.etTitle.error = "Title is required"
            return
        }
        if (message.isEmpty()) {
            binding.etMessage.error = "Message is required"
            return
        }

        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userIds = when (audience) {
                    "All Users" -> getUserIdsByRole(listOf("customer", "farmer"))
                    "Customers Only" -> getUserIdsByRole(listOf("customer"))
                    "Farmers Only" -> getUserIdsByRole(listOf("farmer"))
                    else -> emptyList()
                }

                userIds.forEach { userId ->
                    val notification = Notification(
                        userId = userId,
                        title = title,
                        message = message,
                        type = type,
                        isRead = false
                    )
                    notificationRepository.sendNotification(notification)
                }

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "Broadcast sent to ${userIds.size} users!",
                        Toast.LENGTH_LONG
                    ).show()
                    dismiss()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun getUserIdsByRole(roles: List<String>): List<String> {
        return try {
            val snapshot = db.collection("users").whereIn("role", roles).get().await()
            snapshot.documents.mapNotNull { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSend.isEnabled = !show
        binding.btnCancel.isEnabled = !show
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}