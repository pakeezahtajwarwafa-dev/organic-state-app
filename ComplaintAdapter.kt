package com.example.organicstate.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.organicstate.R
import com.example.organicstate.data.model.Complaint
import com.example.organicstate.databinding.ItemComplaintBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ComplaintAdapter(
    private val onComplaintClick: (Complaint) -> Unit,
    private val onStatusChange: (Complaint, String) -> Unit
) : ListAdapter<Complaint, ComplaintAdapter.ComplaintViewHolder>(ComplaintDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComplaintViewHolder {
        val binding = ItemComplaintBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ComplaintViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ComplaintViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ComplaintViewHolder(
        private val binding: ItemComplaintBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(complaint: Complaint) {
            binding.apply {
                tvUserName.text = complaint.userName
                tvSubject.text = complaint.subject
                tvDescription.text = complaint.description
                tvStatus.text = complaint.status.uppercase()

                // Format date
                complaint.createdAt?.let {
                    tvDate.text = dateFormat.format(it.toDate())
                }

                // Status badge color
                val statusColor = when (complaint.status) {
                    "open" -> R.color.status_open
                    "in-progress" -> R.color.status_in_progress
                    "resolved" -> R.color.status_resolved
                    else -> R.color.status_open
                }
                tvStatus.setBackgroundResource(statusColor)

                // Click listeners
                root.setOnClickListener { onComplaintClick(complaint) }

                btnMarkInProgress.setOnClickListener {
                    onStatusChange(complaint, "in-progress")
                }

                btnMarkResolved.setOnClickListener {
                    onStatusChange(complaint, "resolved")
                }
            }
        }
    }

    private class ComplaintDiffCallback : DiffUtil.ItemCallback<Complaint>() {
        override fun areItemsTheSame(oldItem: Complaint, newItem: Complaint): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Complaint, newItem: Complaint): Boolean {
            return oldItem == newItem
        }
    }
}