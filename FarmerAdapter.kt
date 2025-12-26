package com.example.organicstate.ui.customer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.organicstate.databinding.ItemFarmerBinding

class FarmerAdapter(
    private val onFarmerClick: (farmerId: String, farmerName: String) -> Unit
) : ListAdapter<Pair<String, String>, FarmerAdapter.FarmerViewHolder>(FarmerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FarmerViewHolder {
        val binding = ItemFarmerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FarmerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FarmerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FarmerViewHolder(
        private val binding: ItemFarmerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(farmer: Pair<String, String>) {
            val (farmerId, farmerName) = farmer

            binding.tvFarmerName.text = farmerName
            binding.tvFarmerRole.text = "Organic Farmer"

            binding.root.setOnClickListener {
                onFarmerClick(farmerId, farmerName)
            }
        }
    }

    class FarmerDiffCallback : DiffUtil.ItemCallback<Pair<String, String>>() {
        override fun areItemsTheSame(
            oldItem: Pair<String, String>,
            newItem: Pair<String, String>
        ): Boolean {
            return oldItem.first == newItem.first
        }

        override fun areContentsTheSame(
            oldItem: Pair<String, String>,
            newItem: Pair<String, String>
        ): Boolean {
            return oldItem == newItem
        }
    }
}