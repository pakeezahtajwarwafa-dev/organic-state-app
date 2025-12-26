package com.example.organicstate.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.organicstate.R
import com.example.organicstate.data.model.User
import com.example.organicstate.databinding.ItemUserBinding

class UserAdapter(
    private val onUserClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit,
    private val onToggleRoleClick: (User) -> Unit
) : ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(
        private val binding: ItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.apply {
                tvUserName.text = user.name
                tvUserEmail.text = user.email
                tvUserRole.text = user.role.uppercase()
                tvUserPhone.text = if (user.phone.isNotEmpty()) user.phone else "No phone"

                // Role badge color
                val roleColor = when (user.role) {
                    "admin" -> R.color.role_admin
                    "farmer" -> R.color.role_farmer
                    else -> R.color.role_customer
                }
                tvUserRole.setBackgroundResource(roleColor)

                // Click listeners
                root.setOnClickListener { onUserClick(user) }
                btnChangeRole.setOnClickListener { onToggleRoleClick(user) }
                btnDelete.setOnClickListener { onDeleteClick(user) }
            }
        }
    }

    private class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}