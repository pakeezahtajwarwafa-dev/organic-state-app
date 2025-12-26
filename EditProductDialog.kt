package com.example.organicstate.ui.farmer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.example.organicstate.databinding.DialogAddProductBinding
import com.example.organicstate.utils.ImgBBUploader
import com.example.organicstate.viewmodel.ProductViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProductDialog(private val product: Product) : DialogFragment() {

    var onProductUpdated: (() -> Unit)? = null

    private var _binding: DialogAddProductBinding? = null
    private val binding get() = _binding!!

    private val productViewModel: ProductViewModel by viewModels({ requireParentFragment() })

    private var selectedImageUri: Uri? = null
    private var isNewImageSelected = false

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let { uri ->
                isNewImageSelected = true
                Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(binding.ivProductImage)

                binding.tvImageStatus.text = "New image selected ✓"
                binding.tvImageStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // We use the SAME layout as Add Product
        _binding = DialogAddProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinners()
        fillExistingData()
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

    private fun fillExistingData() {
        binding.apply {
            tvDialogTitle.text = "Edit Product"
            btnSave.text = "Update"

            etProductName.setText(product.name)
            etDescription.setText(product.description)
            etPrice.setText(product.price.toString())
            etOriginalPrice.setText(product.originalPrice.toString())
            etStock.setText(product.stock.toString())

            // Pre-select Category
            val categoryAdapter = spinnerCategory.adapter as ArrayAdapter<String>
            val catPos = categoryAdapter.getPosition(product.category)
            if (catPos >= 0) spinnerCategory.setSelection(catPos)

            // Pre-select Unit
            val unitAdapter = spinnerUnit.adapter as ArrayAdapter<String>
            val unitPos = unitAdapter.getPosition(product.unit)
            if (unitPos >= 0) spinnerUnit.setSelection(unitPos)

            // Load current image
            if (product.images.isNotEmpty()) {
                Glide.with(this@EditProductDialog)
                    .load(product.images[0])
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(ivProductImage)
                tvImageStatus.text = "Current image loaded"
            }
        }
    }

    private fun setupListeners() {
        binding.btnSelectImage.setOnClickListener { openImagePicker() }
        binding.ivProductImage.setOnClickListener { openImagePicker() }
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSave.setOnClickListener { updateProduct() }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        imagePickerLauncher.launch(intent)
    }

    private fun updateProduct() {
        val name = binding.etProductName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val priceStr = binding.etPrice.text.toString().trim()
        val originalPriceStr = binding.etOriginalPrice.text.toString().trim()
        val stockStr = binding.etStock.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem.toString()
        val unit = binding.spinnerUnit.selectedItem.toString()

        if (name.isEmpty()) { binding.etProductName.error = "Required"; return }
        val price = priceStr.toDoubleOrNull() ?: 0.0
        val originalPrice = originalPriceStr.toDoubleOrNull() ?: 0.0
        val stock = stockStr.toIntOrNull() ?: 0

        if (originalPrice > 0 && originalPrice <= price) {
            binding.etOriginalPrice.error = "Original price must be higher than selling price"
            return
        }

        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // If a new image was picked, upload it. Otherwise, use the old image URL.
                var finalImages = product.images
                if (isNewImageSelected && selectedImageUri != null) {
                    val uploadResult = ImgBBUploader.uploadImage(requireContext(), selectedImageUri!!)
                    uploadResult.onSuccess { newUrl ->
                        finalImages = listOf(newUrl)
                    }
                }

                val label = if (originalPrice > price) "OFFER" else ""

                val updates = mapOf(
                    "name" to name,
                    "description" to description,
                    "price" to price,
                    "originalPrice" to originalPrice,
                    "discountLabel" to label,
                    "category" to category,
                    "stock" to stock,
                    "unit" to unit,
                    "images" to finalImages
                )

                withContext(Dispatchers.Main) {
                    productViewModel.updateProduct(product.id, updates)
                    onProductUpdated?.invoke()
                    showLoading(false)
                    dismiss()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !show
    }

    override fun onStart() {
        super.onStart()
        // FIX for the "slim" shape: Forces match_parent width
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}