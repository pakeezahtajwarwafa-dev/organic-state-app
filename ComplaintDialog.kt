package com.example.organicstate.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.model.Complaint
import com.example.organicstate.databinding.DialogComplaintBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ComplaintDialog : DialogFragment() {

    private var _binding: DialogComplaintBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogComplaintBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSubmit.setOnClickListener {
            submitComplaint()
        }
    }

    private fun submitComplaint() {
        val subject = binding.etSubject.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        // Validation
        if (subject.isEmpty()) {
            binding.etSubject.error = "Subject is required"
            return
        }
        if (description.isEmpty()) {
            binding.etDescription.error = "Description is required"
            return
        }

        val userId = FirebaseManager.getCurrentUserId()
        if (userId == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get user data to get name
                val userDoc = db.collection("users").document(userId).get().await()
                val userName = userDoc.getString("name") ?: "Unknown User"

                // Create complaint
                val complaint = Complaint(
                    userId = userId,
                    userName = userName,
                    subject = subject,
                    description = description,
                    status = "open",
                    createdAt = Timestamp.now()
                )

                // Save to Firestore
                db.collection("complaints")
                    .add(complaint.toMap())
                    .await()

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "Complaint submitted successfully!",
                        Toast.LENGTH_LONG
                    ).show()
                    dismiss()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSubmit.isEnabled = !show
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