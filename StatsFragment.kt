package com.example.organicstate.ui.farmer

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.organicstate.R
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.repository.OrderRepository
import com.example.organicstate.databinding.FragmentStatsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val orderRepository = OrderRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChart()
        loadStats()
    }

    private fun setupChart() {
        binding.salesChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            legend.textColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
            legend.textSize = 12f

            // X-axis configuration
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                textSize = 10f
            }

            // Left Y-axis (Revenue)
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E0E0E0")
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                textSize = 10f
                axisMinimum = 0f
            }

            // Right Y-axis (Orders)
            axisRight.apply {
                setDrawGridLines(false)
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                textSize = 10f
                axisMinimum = 0f
            }
        }
    }

    private fun loadStats() {
        val farmerId = FirebaseManager.getCurrentUserId() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            orderRepository.getOrdersByFarmer(farmerId).collect { orders ->
                launch(Dispatchers.Main) {
                    // Get current month and last month data
                    val calendar = Calendar.getInstance()
                    val currentMonth = calendar.get(Calendar.MONTH)
                    val currentYear = calendar.get(Calendar.YEAR)

                    calendar.add(Calendar.MONTH, -1)
                    val lastMonth = calendar.get(Calendar.MONTH)
                    val lastMonthYear = calendar.get(Calendar.YEAR)

                    // Filter orders by month
                    val currentMonthOrders = orders.filter { order ->
                        val orderDate = Calendar.getInstance().apply {
                            order.createdAt?.let { timestamp ->
                                timeInMillis = timestamp.toDate().time
                            }
                        }
                        orderDate.get(Calendar.MONTH) == currentMonth &&
                                orderDate.get(Calendar.YEAR) == currentYear
                    }

                    val lastMonthOrders = orders.filter { order ->
                        val orderDate = Calendar.getInstance().apply {
                            order.createdAt?.let { timestamp ->
                                timeInMillis = timestamp.toDate().time
                            }
                        }
                        orderDate.get(Calendar.MONTH) == lastMonth &&
                                orderDate.get(Calendar.YEAR) == lastMonthYear
                    }

                    // Calculate current stats
                    val totalRevenue = currentMonthOrders.filter { it.status == "delivered" }.sumOf { it.totalAmount }
                    val totalOrders = currentMonthOrders.size

                    // Calculate product sales
                    val productSales = mutableMapOf<String, Pair<Int, Double>>()
                    currentMonthOrders.filter { it.status == "delivered" }.forEach { order ->
                        order.items.forEach { item ->
                            val current = productSales[item.name] ?: Pair(0, 0.0)
                            productSales[item.name] = Pair(
                                current.first + item.quantity,
                                current.second + (item.price * item.quantity)
                            )
                        }
                    }
                    val totalProductsSold = productSales.values.sumOf { it.first }
                    val uniqueCustomers = currentMonthOrders.map { it.customerId }.distinct().size

                    // Calculate last month stats
                    val lastMonthRevenue = lastMonthOrders.filter { it.status == "delivered" }.sumOf { it.totalAmount }
                    val lastMonthTotalOrders = lastMonthOrders.size
                    val lastMonthProductsSold = lastMonthOrders.filter { it.status == "delivered" }
                        .flatMap { it.items }.sumOf { it.quantity }
                    val lastMonthCustomers = lastMonthOrders.map { it.customerId }.distinct().size

                    // Calculate percentage changes
                    val revenueChange = calculatePercentageChange(lastMonthRevenue, totalRevenue)
                    val ordersChange = calculatePercentageChange(lastMonthTotalOrders.toDouble(), totalOrders.toDouble())
                    val productsChange = calculatePercentageChange(lastMonthProductsSold.toDouble(), totalProductsSold.toDouble())
                    val customersChange = calculatePercentageChange(lastMonthCustomers.toDouble(), uniqueCustomers.toDouble())

                    // Display stats
                    binding.tvTotalRevenue.text = "৳${String.format("%,.0f", totalRevenue)}"
                    binding.tvRevenueChange.text = formatChange(revenueChange)
                    binding.tvRevenueChange.setTextColor(getChangeColor(revenueChange))

                    binding.tvTotalOrders.text = totalOrders.toString()
                    binding.tvOrdersChange.text = formatChange(ordersChange)
                    binding.tvOrdersChange.setTextColor(getChangeColor(ordersChange))

                    binding.tvProductsSold.text = totalProductsSold.toString()
                    binding.tvProductsChange.text = formatChange(productsChange)
                    binding.tvProductsChange.setTextColor(getChangeColor(productsChange))

                    binding.tvCustomers.text = uniqueCustomers.toString()
                    binding.tvCustomersChange.text = formatChange(customersChange)
                    binding.tvCustomersChange.setTextColor(getChangeColor(customersChange))

                    // Display top 3 products
                    displayTopProducts(productSales)

                    // NEW: Display sales trend chart
                    displaySalesTrendChart(orders)
                }
            }
        }
    }

    private fun displaySalesTrendChart(orders: List<com.example.organicstate.data.model.Order>) {
        // Get last 6 months data
        val calendar = Calendar.getInstance()
        val monthLabels = mutableListOf<String>()
        val monthlyRevenue = mutableMapOf<String, Double>()
        val monthlyOrders = mutableMapOf<String, Int>()

        // Initialize last 6 months
        for (i in 5 downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.MONTH, -i)
            val monthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
            val monthLabel = SimpleDateFormat("MMM", Locale.getDefault()).format(calendar.time)

            monthLabels.add(monthLabel)
            monthlyRevenue[monthKey] = 0.0
            monthlyOrders[monthKey] = 0
        }

        // Aggregate orders by month
        orders.forEach { order ->
            order.createdAt?.let { timestamp ->
                val orderCalendar = Calendar.getInstance().apply {
                    timeInMillis = timestamp.toDate().time
                }
                val monthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(orderCalendar.time)

                if (monthlyRevenue.containsKey(monthKey)) {
                    monthlyOrders[monthKey] = (monthlyOrders[monthKey] ?: 0) + 1
                    if (order.status == "delivered") {
                        monthlyRevenue[monthKey] = (monthlyRevenue[monthKey] ?: 0.0) + order.totalAmount
                    }
                }
            }
        }

        // Prepare chart data
        val revenueEntries = mutableListOf<Entry>()
        val ordersEntries = mutableListOf<Entry>()

        monthlyRevenue.keys.sorted().forEachIndexed { index, monthKey ->
            revenueEntries.add(Entry(index.toFloat(), monthlyRevenue[monthKey]?.toFloat() ?: 0f))
            ordersEntries.add(Entry(index.toFloat(), monthlyOrders[monthKey]?.toFloat() ?: 0f))
        }

        // Create datasets
        val revenueDataSet = LineDataSet(revenueEntries, "Revenue (৳)").apply {
            color = ContextCompat.getColor(requireContext(), R.color.primary_green)
            lineWidth = 2.5f
            circleRadius = 4f
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.primary_green))
            circleHoleRadius = 2f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            axisDependency = YAxis.AxisDependency.LEFT
        }

        val ordersDataSet = LineDataSet(ordersEntries, "Orders").apply {
            color = ContextCompat.getColor(requireContext(), R.color.accent_brown)
            lineWidth = 2.5f
            circleRadius = 4f
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.accent_brown))
            circleHoleRadius = 2f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            axisDependency = YAxis.AxisDependency.RIGHT
        }

        // Set data to chart
        val lineData = LineData(revenueDataSet, ordersDataSet)
        binding.salesChart.data = lineData

        // Set X-axis labels
        binding.salesChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return monthLabels.getOrNull(value.toInt()) ?: ""
            }
        }

        binding.salesChart.invalidate() // Refresh chart
    }

    private fun calculatePercentageChange(oldValue: Double, newValue: Double): Double {
        if (oldValue == 0.0) return if (newValue > 0) 100.0 else 0.0
        return ((newValue - oldValue) / oldValue) * 100
    }

    private fun formatChange(change: Double): String {
        val symbol = if (change >= 0) "↑" else "↓"
        return "$symbol ${String.format("%.1f", Math.abs(change))}% from last month"
    }

    private fun getChangeColor(change: Double): Int {
        return if (change >= 0) {
            requireContext().getColor(android.R.color.holo_green_dark)
        } else {
            requireContext().getColor(android.R.color.holo_red_dark)
        }
    }

    private fun displayTopProducts(productSales: Map<String, Pair<Int, Double>>) {
        binding.topProductsContainer.removeAllViews()

        val topProducts = productSales.entries
            .sortedByDescending { it.value.second }
            .take(3)

        if (topProducts.isEmpty()) {
            val noDataView = TextView(requireContext()).apply {
                text = "No products sold yet"
                textSize = 14f
                setTextColor(requireContext().getColor(R.color.text_secondary))
                setPadding(0, 16, 0, 16)
            }
            binding.topProductsContainer.addView(noDataView)
            return
        }

        topProducts.forEachIndexed { index, entry ->
            val productName = entry.key
            val quantity = entry.value.first
            val revenue = entry.value.second

            val productView = layoutInflater.inflate(
                R.layout.item_top_product,
                binding.topProductsContainer,
                false
            )

            productView.findViewById<TextView>(R.id.tvProductRank).text = (index + 1).toString()
            productView.findViewById<TextView>(R.id.tvProductName).text = productName
            productView.findViewById<TextView>(R.id.tvProductUnits).text = "$quantity units sold"
            productView.findViewById<TextView>(R.id.tvProductRevenue).text = "৳${String.format("%,.0f", revenue)}"

            // Change badge color based on rank
            val badgeColor = when (index) {
                0 -> android.graphics.Color.parseColor("#FFB300") // Gold
                1 -> android.graphics.Color.parseColor("#C0C0C0") // Silver
                2 -> android.graphics.Color.parseColor("#CD7F32") // Bronze
                else -> android.graphics.Color.parseColor("#4CAF50")
            }
            productView.findViewById<TextView>(R.id.tvProductRank).background.setTint(badgeColor)

            binding.topProductsContainer.addView(productView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}