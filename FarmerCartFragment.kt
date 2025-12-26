package com.example.organicstate.ui.farmer

import com.example.organicstate.utils.CartItem
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.model.Order
import com.example.organicstate.data.model.OrderItem
import com.example.organicstate.data.model.Notification
import com.example.organicstate.data.repository.AuthRepository
import com.example.organicstate.data.repository.OrderRepository
import com.example.organicstate.data.repository.NotificationRepository
import com.example.organicstate.databinding.FragmentCartBinding
import com.example.organicstate.ui.adapters.CartAdapter
import com.example.organicstate.viewmodel.CartViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FarmerCartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private val cartViewModel: CartViewModel by activityViewModels()
    private lateinit var cartAdapter: CartAdapter

    private val authRepository = AuthRepository()
    private val orderRepository = OrderRepository()
    private val notificationRepository = NotificationRepository() // ADDED

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onQuantityChanged = { productId, quantity ->
                cartViewModel.updateQuantity(productId, quantity)
            },
            onRemoveItem = { productId ->
                cartViewModel.removeFromCart(productId)
                Toast.makeText(requireContext(), "Item removed", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvCartItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
            itemAnimator = null
        }
    }

    private fun setupObservers() {
        cartViewModel.cartItems.observe(viewLifecycleOwner) { items ->
            cartAdapter.submitList(items)

            binding.tvEmptyCart.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            binding.layoutCheckout.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        }

        cartViewModel.totalAmount.observe(viewLifecycleOwner) { total ->
            binding.tvTotalAmount.text = "৳${String.format("%.2f", total)}"
        }
    }

    private fun setupListeners() {
        binding.btnCheckout.setOnClickListener {
            placeOrder()
        }
    }

    private fun placeOrder() {
        val items = cartViewModel.cartItems.value
        if (items.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = FirebaseManager.getCurrentUserId() ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.btnCheckout.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get user data (farmer in this case)
                val userResult = authRepository.getUserData(userId)
                val user = userResult.getOrNull()

                if (user == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
                        binding.btnCheckout.isEnabled = true
                    }
                    return@launch
                }

                // Check stock availability
                val stockCheckFailed = checkStockAvailability(items)
                if (stockCheckFailed != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Not enough stock for ${stockCheckFailed.product.name}",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.progressBar.visibility = View.GONE
                        binding.btnCheckout.isEnabled = true
                    }
                    return@launch
                }

                // Group items by seller (other farmers)
                val itemsByFarmer = items.groupBy { it.product.farmerId }

                // Create separate order for each seller
                itemsByFarmer.forEach { (sellerId, sellerItems) ->
                    val orderItems = sellerItems.map { cartItem ->
                        OrderItem(
                            productId = cartItem.product.id,
                            name = cartItem.product.name,
                            quantity = cartItem.quantity,
                            price = cartItem.product.price
                        )
                    }

                    val total = sellerItems.sumOf { it.product.price * it.quantity }
                    val farmerName = sellerItems.firstOrNull()?.product?.farmerName ?: "Unknown Farmer"

                    val order = Order(
                        customerId = userId,
                        customerName = user.name,
                        farmerId = sellerId,
                        farmerName = farmerName,
                        items = orderItems,
                        totalAmount = total,
                        status = "pending",
                        address = user.address.ifEmpty { "No address provided" },
                        phone = user.phone.ifEmpty { "No phone provided" }
                    )

                    orderRepository.createOrder(order)

                    // Send notifications
                    sendBuyerNotification(userId, user.name, total)
                    sendSellerNotification(sellerId, farmerName, user.name, total)

                    // Reduce stock for each product
                    sellerItems.forEach { cartItem ->
                        reduceProductStock(cartItem.product.id, cartItem.quantity)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Order placed successfully!", Toast.LENGTH_LONG).show()
                    cartViewModel.clearCart()
                    binding.progressBar.visibility = View.GONE
                    binding.btnCheckout.isEnabled = true

                    // Navigate back to buy fragment
                    parentFragmentManager.popBackStack()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.progressBar.visibility = View.GONE
                    binding.btnCheckout.isEnabled = true
                }
            }
        }
    }

    // Send notification to buyer farmer
    private suspend fun sendBuyerNotification(
        buyerId: String,
        buyerName: String,
        totalAmount: Double
    ) {
        try {
            val notification = Notification(
                userId = buyerId,
                title = "Order Placed Successfully",
                message = "Your order of ৳${String.format("%.2f", totalAmount)} has been placed and is pending confirmation.",
                type = "order",
                isRead = false
            )
            notificationRepository.sendNotification(notification)
            Log.d("FarmerCartFragment", "Buyer notification sent")
        } catch (e: Exception) {
            Log.e("FarmerCartFragment", "Error sending buyer notification: ${e.message}")
        }
    }

    // Send notification to seller farmer
    private suspend fun sendSellerNotification(
        sellerId: String,
        sellerName: String,
        buyerName: String,
        totalAmount: Double
    ) {
        try {
            val notification = Notification(
                userId = sellerId,
                title = "New Order Received",
                message = "$buyerName placed an order worth ৳${String.format("%.2f", totalAmount)}. Please review and confirm.",
                type = "order",
                isRead = false
            )
            notificationRepository.sendNotification(notification)
            Log.d("FarmerCartFragment", "Seller notification sent")
        } catch (e: Exception) {
            Log.e("FarmerCartFragment", "Error sending seller notification: ${e.message}")
        }
    }

    // Check stock availability
    private suspend fun checkStockAvailability(items: List<CartItem>): CartItem? {
        return try {
            for (item in items) {
                val productDoc = FirebaseManager.db.collection("products")
                    .document(item.product.id)
                    .get()
                    .await()

                val currentStock = productDoc.getLong("stock")?.toInt() ?: 0

                if (currentStock < item.quantity) {
                    return item
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    // Reduce stock
    private suspend fun reduceProductStock(productId: String, quantityBought: Int) {
        try {
            val productRef = FirebaseManager.db.collection("products").document(productId)

            FirebaseManager.db.runTransaction { transaction ->
                val snapshot = transaction.get(productRef)
                val currentStock = snapshot.getLong("stock")?.toInt() ?: 0
                val newStock = (currentStock - quantityBought).coerceAtLeast(0)

                transaction.update(productRef, "stock", newStock)

                // Get farmer ID and product info for notification
                val farmerId = snapshot.getString("farmerId") ?: ""
                val productName = snapshot.getString("name") ?: "Product"

                // Send stock notification to farmer after transaction
                if (farmerId.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        sendStockNotification(farmerId, productId, productName, newStock)
                    }
                }

                null
            }.await()

        } catch (e: Exception) {
            Log.e("FarmerCartFragment", "Error reducing stock: ${e.message}")
        }
    }

    // ADDED: Send stock alert notification to farmer
    private suspend fun sendStockNotification(farmerId: String, productId: String, productName: String, newStock: Int) {
        try {
            val notification = when {
                newStock == 0 -> Notification(
                    userId = farmerId,
                    title = "Product Out of Stock ❌",
                    message = "Your product '$productName' is out of stock. Please restock soon to continue selling.",
                    type = "system",
                    isRead = false,
                    data = mapOf("productId" to productId, "stock" to newStock.toString())
                )
                newStock <= 5 -> Notification(
                    userId = farmerId,
                    title = "Low Stock Alert ⚠️",
                    message = "Your product '$productName' is running low with only $newStock units left.",
                    type = "system",
                    isRead = false,
                    data = mapOf("productId" to productId, "stock" to newStock.toString())
                )
                else -> null
            }

            notification?.let {
                notificationRepository.sendNotification(it)
                Log.d("FarmerCartFragment", "Stock notification sent to farmer: $farmerId")
            }
        } catch (e: Exception) {
            Log.e("FarmerCartFragment", "Error sending stock notification: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}