package com.example.organicstate.ui.farmer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.organicstate.R
import com.example.organicstate.data.local.FirebaseManager
import com.example.organicstate.data.model.Product
import com.example.organicstate.data.repository.AuthRepository
import com.example.organicstate.databinding.DialogAddProductBinding
import com.example.organicstate.utils.ImgBBUploader
import com.example.organicstate.viewmodel.ProductViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddProductDialog : DialogFragment() {

    private var _binding: DialogAddProductBinding? = null
    private val binding get() = _binding!!

    private val productViewModel: ProductViewModel by viewModels({ requireParentFragment() })
    private val authRepository = AuthRepository()

    private var selectedImageUri: Uri? = null

    companion object {
        private const val TAG = "AddProductDialog"
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(binding.ivProductImage)

                binding.tvImageStatus.text = "Image selected ✓"
                binding.tvImageStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinners()
        setupListeners()
    }

    private fun setupSpinners() {
        val categories = arrayOf("Vegetables", "Fruits", "Grains", "Dairy", "Poultry", "Seeds", "Fertilizers", "Other")
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spinnerCategory.adapter = categoryAdapter

        val units = arrayOf("kg", "piece", "liter", "dozen", "bundle")
        val unitAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, units)
        binding.spinnerUnit.adapter = unitAdapter
    }

    private fun setupListeners() {
        binding.btnSelectImage.setOnClickListener { openImagePicker() }
        binding.ivProductImage.setOnClickListener { openImagePicker() }
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSave.setOnClickListener { saveProduct() }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        imagePickerLauncher.launch(intent)
    }

    private fun saveProduct() {
        val name = binding.etProductName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val priceStr = binding.etPrice.text.toString().trim()
        val originalPriceStr = binding.etOriginalPrice.text.toString().trim()
        val stockStr = binding.etStock.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem.toString()
        val unit = binding.spinnerUnit.selectedItem.toString()

        // Validation
        if (name.isEmpty()) {
            binding.etProductName.error = "Product name is required"
            return
        }
        if (priceStr.isEmpty()) {
            binding.etPrice.error = "Price is required"
            return
        }
        if (stockStr.isEmpty()) {
            binding.etStock.error = "Stock quantity is required"
            return
        }

        val price = priceStr.toDoubleOrNull()
        if (price == null || price <= 0) {
            binding.etPrice.error = "Enter valid price"
            return
        }

        val originalPrice = originalPriceStr.toDoubleOrNull() ?: 0.0
        val stock = stockStr.toIntOrNull()
        if (stock == null || stock < 0) {
            binding.etStock.error = "Enter valid stock"
            return
        }

        // Validation: Original Price must be greater than Selling Price if set
        if (originalPrice > 0 && originalPrice <= price) {
            binding.etOriginalPrice.error = "Original price must be higher than selling price"
            return
        }

        val farmerId = FirebaseManager.getCurrentUserId()
        if (farmerId == null) {
            Toast.makeText(requireContext(), "Error: Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Starting product save process...")

                // Upload image
                val imageUrls = mutableListOf<String>()
                selectedImageUri?.let { uri ->
                    Log.d(TAG, "Uploading image...")
                    val uploadResult = ImgBBUploader.uploadImage(requireContext(), uri)
                    uploadResult.onSuccess {
                        imageUrls.add(it)
                        Log.d(TAG, "Image uploaded successfully: $it")
                    }.onFailure {
                        Log.e(TAG, "Image upload failed: ${it.message}")
                    }
                }

                // Get farmer name
                val userResult = authRepository.getUserData(farmerId)
                val farmerName = userResult.getOrNull()?.name ?: "Unknown Farmer"
                Log.d(TAG, "Farmer name: $farmerName")

                // Auto-set discount label if there's a deal
                val label = if (originalPrice > price) "OFFER" else ""

                // CRITICAL FIX: Explicitly set createdAt and isAvailable
                val product = Product(
                    id = "", // Will be set by Firestore
                    name = name,
                    description = description,
                    price = price,
                    originalPrice = originalPrice,
                    discountLabel = label,
                    isPromoted = false,
                    category = category,
                    farmerId = farmerId,
                    farmerName = farmerName,
                    stock = stock,
                    unit = unit,
                    images = imageUrls,
                    rating = 0.0,
                    reviewCount = 0,
                    createdAt = Timestamp.now(), // FIXED: Explicitly set timestamp
                    isAvailable = true // FIXED: Explicitly set to true
                )

                Log.d(TAG, "Product object created: $product")
                Log.d(TAG, "CreatedAt: ${product.createdAt}")
                Log.d(TAG, "IsAvailable: ${product.isAvailable}")

                withContext(Dispatchers.Main) {
                    // Add product through ViewModel
                    productViewModel.addProduct(product)

                    Log.d(TAG, "Product added successfully")
                    Toast.makeText(requireContext(), "Product added successfully!", Toast.LENGTH_SHORT).show()

                    showLoading(false)
                    dismiss()

                    // Refresh the parent fragment
                    (parentFragment as? StoreFragment)?.let {
                        // The observer in StoreFragment will automatically update
                        Log.d(TAG, "Parent fragment notified")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving product", e)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.message ?: "Failed to add product"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !show
        binding.btnCancel.isEnabled = !show
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}