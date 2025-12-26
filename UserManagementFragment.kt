package com.example.organicstate.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.organicstate.data.model.User
import com.example.organicstate.databinding.FragmentUserManagementBinding
import com.example.organicstate.ui.adapters.UserAdapter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserManagementFragment : Fragment() {

    private var _binding: FragmentUserManagementBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private lateinit var userAdapter: UserAdapter
    private var allUsers = mutableListOf<User>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        loadUsers()
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(
            onUserClick = { user ->
                showUserDetailsDialog(user)
            },
            onDeleteClick = { user ->
                showDeleteConfirmation(user)
            },
            onToggleRoleClick = { user ->
                showChangeRoleDialog(user)
            }
        )

        binding.rvUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }
    }

    private fun setupListeners() {
        // Filter tabs
        binding.chipAll.setOnClickListener {
            filterUsers("all")
        }

        binding.chipCustomers.setOnClickListener {
            filterUsers("customer")
        }

        binding.chipFarmers.setOnClickListener {
            filterUsers("farmer")
        }

        binding.chipAdmins.setOnClickListener {
            filterUsers("admin")
        }

        // Search
        binding.etSearch.setOnEditorActionListener { v, _, _ ->
            val query = v.text.toString().trim()
            searchUsers(query)
            true
        }

        // Refresh
        binding.swipeRefresh.setOnRefreshListener {
            loadUsers()
        }
    }

    private fun loadUsers() {
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("users").get().await()
                val users = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                }

                withContext(Dispatchers.Main) {
                    allUsers.clear()
                    allUsers.addAll(users)
                    userAdapter.submitList(users)

                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    binding.tvEmptyState.visibility =
                        if (users.isEmpty()) View.VISIBLE else View.GONE

                    updateStats(users)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(
                        requireContext(),
                        "Error loading users: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun filterUsers(role: String) {
        val filteredUsers = if (role == "all") {
            allUsers
        } else {
            allUsers.filter { it.role == role }
        }

        userAdapter.submitList(filteredUsers)
        binding.tvEmptyState.visibility =
            if (filteredUsers.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun searchUsers(query: String) {
        if (query.isEmpty()) {
            userAdapter.submitList(allUsers)
            return
        }

        val searchResults = allUsers.filter { user ->
            user.name.contains(query, ignoreCase = true) ||
                    user.email.contains(query, ignoreCase = true) ||
                    user.phone.contains(query, ignoreCase = true)
        }

        userAdapter.submitList(searchResults)
        binding.tvEmptyState.visibility =
            if (searchResults.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateStats(users: List<User>) {
        val totalUsers = users.size
        val customers = users.count { it.role == "customer" }
        val farmers = users.count { it.role == "farmer" }
        val admins = users.count { it.role == "admin" }

        binding.tvTotalUsers.text = "Total: $totalUsers"
        binding.tvCustomersCount.text = "Customers: $customers"
        binding.tvFarmersCount.text = "Farmers: $farmers"
        binding.tvAdminsCount.text = "Admins: $admins"
    }

    private fun showUserDetailsDialog(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("User Details")
            .setMessage("""
                Name: ${user.name}
                Email: ${user.email}
                Role: ${user.role}
                Phone: ${user.phone.ifEmpty { "Not provided" }}
                Address: ${user.address.ifEmpty { "Not provided" }}
            """.trimIndent())
            .setPositiveButton("OK", null)
            .setNeutralButton("Change Role") { _, _ ->
                showChangeRoleDialog(user)
            }
            .setNegativeButton("Delete User") { _, _ ->
                showDeleteConfirmation(user)
            }
            .show()
    }

    private fun showChangeRoleDialog(user: User) {
        val roles = arrayOf("customer", "farmer", "admin")
        var selectedRole = user.role

        AlertDialog.Builder(requireContext())
            .setTitle("Change Role for ${user.name}")
            .setSingleChoiceItems(roles, roles.indexOf(user.role)) { _, which ->
                selectedRole = roles[which]
            }
            .setPositiveButton("Change") { _, _ ->
                changeUserRole(user, selectedRole)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun changeUserRole(user: User, newRole: String) {
        if (newRole == user.role) {
            Toast.makeText(requireContext(), "Role is already $newRole", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.collection("users")
                    .document(user.id)
                    .update("role", newRole)
                    .await()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Role changed to $newRole successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadUsers()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Error changing role: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showDeleteConfirmation(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete ${user.name}? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteUser(user)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUser(user: User) {
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Delete user document
                db.collection("users")
                    .document(user.id)
                    .delete()
                    .await()

                // Note: In production, you should also:
                // 1. Delete user from Firebase Authentication
                // 2. Delete user's products, orders, etc.
                // This requires Firebase Admin SDK or Cloud Functions

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "User deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadUsers()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Error deleting user: ${e.message}",
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