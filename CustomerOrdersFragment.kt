package com.example.organicstate.ui.customer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.model.Order
import com.example.organicstate.data.repository.OrderRepository
import com.example.organicstate.databinding.FragmentCustomerOrdersBinding
import com.example.organicstate.ui.adapters.CustomerOrderAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CustomerOrdersFragment : Fragment() {

    private var _binding: FragmentCustomerOrdersBinding? = null
    private val binding get() = _binding!!

    private val orderRepository = OrderRepository()
    private lateinit var orderAdapter: CustomerOrderAdapter
    private var allOrders = listOf<Order>()

    companion object {
        private const val TAG = "CustomerOrdersFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        loadOrders()
    }

    private fun setupRecyclerView() {
        orderAdapter = CustomerOrderAdapter { order ->
            showOrderDetails(order)
        }

        binding.rvOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderAdapter
        }
    }

    private fun setupListeners() {
        // Filter chips
        binding.chipAll.setOnClickListener {
            filterOrders("all")
        }

        binding.chipPending.setOnClickListener {
            filterOrders("pending")
        }

        binding.chipConfirmed.setOnClickListener {
            filterOrders("confirmed")
        }

        binding.chipDelivered.setOnClickListener {
            filterOrders("delivered")
        }

        // Refresh
        binding.swipeRefresh.setOnRefreshListener {
            loadOrders()
        }
    }

    private fun loadOrders() {
        val customerId = FirebaseManager.getCurrentUserId() ?: return

        Log.d(TAG, "Loading orders for customer: $customerId")
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                orderRepository.getOrdersByCustomer(customerId).collect { orders ->
                    Log.d(TAG, "Received ${orders.size} orders")

                    launch(Dispatchers.Main) {
                        allOrders = orders
                        orderAdapter.submitList(orders)
                        binding.progressBar.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false

                        if (orders.isEmpty()) {
                            binding.tvEmptyState.visibility = View.VISIBLE
                        } else {
                            binding.tvEmptyState.visibility = View.GONE
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading orders", e)
                launch(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun filterOrders(status: String) {
        val filtered = if (status == "all") {
            allOrders
        } else {
            allOrders.filter { it.status == status }
        }

        orderAdapter.submitList(filtered)

        if (filtered.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
        } else {
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    private fun showOrderDetails(order: Order) {
        val itemsText = order.items.joinToString("\n") {
            "• ${it.name} (x${it.quantity}) - ৳${String.format("%.2f", it.price * it.quantity)}"
        }

        val statusMessage = when (order.status) {
            "pending" -> "⏳ Your order is pending farmer confirmation"
            "confirmed" -> "✅ Your order has been confirmed and is being prepared"
            "delivered" -> "🎉 Your order has been delivered!"
            else -> "Status: ${order.status}"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Order Details")
            .setMessage("""
                Order ID: ${order.id.take(12)}...
                
                Items:
                $itemsText
                
                Delivery Address: ${order.address}
                Contact: ${order.phone}
                
                Total: ৳${String.format("%.2f", order.totalAmount)}
                
                $statusMessage
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}