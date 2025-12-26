package com.example.organicstate.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.organicstate.data.model.Complaint
import com.example.organicstate.databinding.FragmentComplaintsBinding
import com.example.organicstate.ui.adapters.ComplaintAdapter
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ComplaintsFragment : Fragment() {

    private var _binding: FragmentComplaintsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private lateinit var complaintAdapter: ComplaintAdapter
    private var allComplaints = mutableListOf<Complaint>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComplaintsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        loadComplaints()
    }

    private fun setupRecyclerView() {
        complaintAdapter = ComplaintAdapter(
            onComplaintClick = { complaint ->
                showComplaintDetailsDialog(complaint)
            },
            onStatusChange = { complaint, newStatus ->
                updateComplaintStatus(complaint, newStatus)
            }
        )

        binding.rvComplaints.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = complaintAdapter
        }
    }

    private fun setupListeners() {
        // Filter tabs
        binding.chipAll.setOnClickListener {
            filterComplaints("all")
        }

        binding.chipOpen.setOnClickListener {
            filterComplaints("open")
        }

        binding.chipInProgress.setOnClickListener {
            filterComplaints("in-progress")
        }

        binding.chipResolved.setOnClickListener {
            filterComplaints("resolved")
        }

        // Search
        binding.etSearch.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString().trim()
            searchComplaints(query)
            true
        }

        // Refresh
        binding.swipeRefresh.setOnRefreshListener {
            loadComplaints()
        }
    }

    private fun loadComplaints() {
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("complaints")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val complaints = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Complaint::class.java)?.copy(id = doc.id)
                }

                withContext(Dispatchers.Main) {
                    allComplaints.clear()
                    allComplaints.addAll(complaints)
                    complaintAdapter.submitList(complaints)

                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    binding.tvEmptyState.visibility =
                        if (complaints.isEmpty()) View.VISIBLE else View.GONE

                    updateStats(complaints)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(
                        requireContext(),
                        "Error loading complaints: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun filterComplaints(status: String) {
        val filteredComplaints = if (status == "all") {
            allComplaints
        } else {
            allComplaints.filter { it.status == status }
        }

        complaintAdapter.submitList(filteredComplaints)
        binding.tvEmptyState.visibility =
            if (filteredComplaints.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun searchComplaints(query: String) {
        if (query.isEmpty()) {
            complaintAdapter.submitList(allComplaints)
            return
        }

        val searchResults = allComplaints.filter { complaint ->
            complaint.subject.contains(query, ignoreCase = true) ||
                    complaint.description.contains(query, ignoreCase = true) ||
                    complaint.userName.contains(query, ignoreCase = true)
        }

        complaintAdapter.submitList(searchResults)
        binding.tvEmptyState.visibility =
            if (searchResults.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateStats(complaints: List<Complaint>) {
        val total = complaints.size
        val open = complaints.count { it.status == "open" }
        val inProgress = complaints.count { it.status == "in-progress" }
        val resolved = complaints.count { it.status == "resolved" }

        binding.tvTotalComplaints.text = "Total: $total"
        binding.tvOpenCount.text = "Open: $open"
        binding.tvInProgressCount.text = "In Progress: $inProgress"
        binding.tvResolvedCount.text = "Resolved: $resolved"
    }

    private fun showComplaintDetailsDialog(complaint: Complaint) {
        val statusOptions = arrayOf("open", "in-progress", "resolved")

        AlertDialog.Builder(requireContext())
            .setTitle("Complaint Details")
            .setMessage("""
                From: ${complaint.userName}
                Subject: ${complaint.subject}
                Description: ${complaint.description}
                Status: ${complaint.status}
                Created: ${complaint.createdAt?.toDate()}
            """.trimIndent())
            .setPositiveButton("Change Status") { _, _ ->
                showStatusChangeDialog(complaint, statusOptions)
            }
            .setNeutralButton("Close", null)
            .setNegativeButton("Delete") { _, _ ->
                showDeleteConfirmation(complaint)
            }
            .show()
    }

    private fun showStatusChangeDialog(complaint: Complaint, statusOptions: Array<String>) {
        var selectedStatus = complaint.status

        AlertDialog.Builder(requireContext())
            .setTitle("Change Status")
            .setSingleChoiceItems(statusOptions, statusOptions.indexOf(complaint.status)) { _, which ->
                selectedStatus = statusOptions[which]
            }
            .setPositiveButton("Update") { _, _ ->
                updateComplaintStatus(complaint, selectedStatus)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateComplaintStatus(complaint: Complaint, newStatus: String) {
        if (newStatus == complaint.status) {
            Toast.makeText(requireContext(), "Status is already $newStatus", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val updates = mutableMapOf<String, Any>(
                    "status" to newStatus
                )

                // Add resolvedAt timestamp if status is resolved
                if (newStatus == "resolved") {
                    updates["resolvedAt"] = Timestamp.now()
                }

                db.collection("complaints")
                    .document(complaint.id)
                    .update(updates)
                    .await()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Status updated to $newStatus",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadComplaints()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Error updating status: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showDeleteConfirmation(complaint: Complaint) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Complaint")
            .setMessage("Are you sure you want to delete this complaint?")
            .setPositiveButton("Delete") { _, _ ->
                deleteComplaint(complaint)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteComplaint(complaint: Complaint) {
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("complaints")
                    .document(complaint.id)
                    .delete()
                    .await()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Complaint deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadComplaints()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Error deleting complaint: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}