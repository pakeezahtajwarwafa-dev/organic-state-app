package com.example.organicstate.ui.farmer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.model.Notification
import com.example.organicstate.data.model.Order
import com.example.organicstate.data.repository.NotificationRepository
import com.example.organicstate.data.repository.OrderRepository
import com.example.organicstate.databinding.FragmentOrdersBinding
import com.example.organicstate.ui.adapters.OrderAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    private val orderRepository = OrderRepository()
    private val notificationRepository = NotificationRepository()
    private lateinit var orderAdapter: OrderAdapter
    private var allOrders = listOf<Order>()

    companion object {
        private const val TAG = "OrdersFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG, "OrdersFragment created")
        setupRecyclerView()
        setupListeners()
        loadOrders()
    }

    private fun setupRecyclerView() {
        orderAdapter = OrderAdapter(
            onOrderClick = { order ->
                Log.d(TAG, "Order clicked: ${order.id}")
                showOrderDetails(order)
            },
            onConfirm = { order ->
                Log.d(TAG, "Confirming order: ${order.id}")
                updateOrderStatus(order, "confirmed")
            },
            onDeliver = { order ->
                Log.d(TAG, "Marking order as delivered: ${order.id}")
                updateOrderStatus(order, "delivered")
            }
        )

        binding.rvOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderAdapter
        }
        Log.d(TAG, "RecyclerView setup complete")
    }

    private fun setupListeners() {
        // Filter chips
        binding.chipAll.setOnClickListener {
            Log.d(TAG, "Filter: All")
            filterOrders("all")
        }

        binding.chipPending.setOnClickListener {
            Log.d(TAG, "Filter: Pending")
            filterOrders("pending")
        }

        binding.chipConfirmed.setOnClickListener {
            Log.d(TAG, "Filter: Confirmed")
            filterOrders("confirmed")
        }

        binding.chipDelivered.setOnClickListener {
            Log.d(TAG, "Filter: Delivered")
            filterOrders("delivered")
        }

        // Refresh
        binding.swipeRefresh.setOnRefreshListener {
            Log.d(TAG, "Refreshing orders")
            loadOrders()
        }
    }

    private fun loadOrders() {
        val farmerId = FirebaseManager.getCurrentUserId()
        if (farmerId == null) {
            Log.e(TAG, "No farmer ID found")
            Toast.makeText(requireContext(), "Error: Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Loading orders for farmer: $farmerId")
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                orderRepository.getOrdersByFarmer(farmerId).collect { orders ->
                    Log.d(TAG, "Received ${orders.size} orders")

                    launch(Dispatchers.Main) {
                        allOrders = orders
                        orderAdapter.submitList(orders)
                        binding.progressBar.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false

                        if (orders.isEmpty()) {
                            binding.tvEmptyState.visibility = View.VISIBLE
                            binding.tvEmptyState.text = "No orders received yet"
                        } else {
                            binding.tvEmptyState.visibility = View.GONE
                        }

                        Log.d(TAG, "Orders displayed successfully")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading orders", e)
                launch(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(
                        requireContext(),
                        "Error loading orders: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
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

        Log.d(TAG, "Filtered to ${filtered.size} orders with status: $status")
        orderAdapter.submitList(filtered)

        if (filtered.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.tvEmptyState.text = "No $status orders"
        } else {
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    private fun showOrderDetails(order: Order) {
        val itemsText = order.items.joinToString("\n") {
            "• ${it.name} (x${it.quantity}) - ৳${String.format("%.2f", it.price * it.quantity)}"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Order Details")
            .setMessage("""
                Order ID: ${order.id.take(12)}...
                Customer: ${order.customerName}
                Phone: ${order.phone}
                Address: ${order.address}
                
                Items:
                $itemsText
                
                Total: ৳${String.format("%.2f", order.totalAmount)}
                Status: ${order.status.uppercase()}
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun updateOrderStatus(order: Order, newStatus: String) {
        Log.d(TAG, "Updating order ${order.id} to status: $newStatus")
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Update order status
                val result = orderRepository.updateOrderStatus(order.id, newStatus)

                result.onSuccess {
                    Log.d(TAG, "Order status updated successfully")

                    // Send notification to customer
                    sendNotificationToCustomer(order, newStatus)

                    launch(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            "Order marked as $newStatus",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }.onFailure { e ->
                    Log.e(TAG, "Failed to update order status", e)
                    launch(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            "Error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception updating order status", e)
                launch(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private suspend fun sendNotificationToCustomer(order: Order, newStatus: String) {
        try {
            val (title, message) = when (newStatus) {
                "confirmed" -> Pair(
                    "Order Confirmed! 🎉",
                    "Your order #${order.id.take(8)} has been confirmed and is being prepared."
                )
                "delivered" -> Pair(
                    "Order Delivered! ✅",
                    "Your order #${order.id.take(8)} has been delivered. Enjoy your purchase!"
                )
                else -> Pair(
                    "Order Update",
                    "Your order #${order.id.take(8)} status: $newStatus"
                )
            }

            val notification = Notification(
                userId = order.customerId,
                title = title,
                message = message,
                type = "order",
                isRead = false,
                data = mapOf(
                    "orderId" to order.id,
                    "status" to newStatus
                )
            )

            notificationRepository.sendNotification(notification)
            Log.d(TAG, "Notification sent to customer: ${order.customerId}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}