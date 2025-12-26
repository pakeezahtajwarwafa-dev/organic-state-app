package com.example.organicstate.ui.farmer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.organicstate.R
import com.example.organicstate.data.model.ChatMessage
import com.example.organicstate.data.remote.OpenRouterAPI
import com.example.organicstate.databinding.ActivityAichatbotBinding
import com.example.organicstate.ui.adapters.ChatAdapter
import com.example.organicstate.utils.ImageHelper
import kotlinx.coroutines.launch
import java.io.File

class AIChatbotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAichatbotBinding
    private lateinit var chatAdapter: ChatAdapter
    private val api = OpenRouterAPI()

    private val messages = mutableListOf<ChatMessage>()
    private var currentImageUri: Uri? = null
    private var currentImageBase64: String? = null

    // Camera
    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentImageUri != null) {
            handleImageSelected(currentImageUri!!)
        }
    }

    // Gallery
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleImageSelected(it) }
    }

    // Camera permission
    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAichatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        showWelcomeMessage()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@AIChatbotActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupListeners() {
        binding.apply {
            // Back button
            ivBack.setOnClickListener {
                finish()
            }

            // Send message
            btnSend.setOnClickListener {
                sendMessage()
            }

            // Camera
            btnCamera.setOnClickListener {
                checkCameraPermissionAndOpen()
            }

            // Gallery
            btnGallery.setOnClickListener {
                pickImage.launch("image/*")
            }

            // Quick actions
            chipPests.setOnClickListener {
                etMessage.setText("How do I identify and control pests on my crops?")
            }

            chipDiseases.setOnClickListener {
                etMessage.setText("What are common crop diseases and their treatments?")
            }

            chipTips.setOnClickListener {
                etMessage.setText("Give me some general farming tips")
            }

            chipWeather.setOnClickListener {
                etMessage.setText("How does weather affect my crops?")
            }

            // Remove image
            btnRemoveImage.setOnClickListener {
                clearImageSelection()
            }
        }
    }

    private fun showWelcomeMessage() {
        val welcome = ChatMessage.createAIMessage(
            "🌾 Hello! I'm your AI farming assistant.\n\n" +
                    "I can help you with:\n" +
                    "• Crop disease identification\n" +
                    "• Pest control advice\n" +
                    "• Growing tips\n" +
                    "• General farming questions\n\n" +
                    "Upload a photo of your crop or ask me anything!"
        )
        messages.add(welcome)
        chatAdapter.submitList(messages.toList())
    }

    private fun sendMessage() {
        val messageText = binding.etMessage.text.toString().trim()

        if (messageText.isEmpty() && currentImageBase64 == null) {
            Toast.makeText(this, "Please enter a message or select an image", Toast.LENGTH_SHORT).show()
            return
        }

        // Create user message
        val userMessage = ChatMessage.createUserMessage(
            text = messageText,
            imageUrl = currentImageUri?.toString() ?: ""
        )
        messages.add(userMessage)
        chatAdapter.submitList(messages.toList())
        scrollToBottom()

        // Clear input
        binding.etMessage.setText("")

        // Show typing indicator
        val typingIndicator = ChatMessage.createTypingIndicator()
        messages.add(typingIndicator)
        chatAdapter.submitList(messages.toList())
        scrollToBottom()

        // Disable input while processing
        setInputEnabled(false)

        // Send to AI
        lifecycleScope.launch {
            try {
                val result = if (currentImageBase64 != null) {
                    // Send with image
                    api.analyzeImage(
                        message = if (messageText.isEmpty()) "Analyze this crop image and tell me if there are any issues." else messageText,
                        base64Image = currentImageBase64!!
                    )
                } else {
                    // Send text only
                    api.sendMessage(messageText)
                }

                // Remove typing indicator
                messages.removeAt(messages.size - 1)

                result.onSuccess { response ->
                    // Add AI response
                    val aiMessage = ChatMessage.createAIMessage(response)
                    messages.add(aiMessage)
                }.onFailure { error ->
                    // Add error message
                    val errorMessage = ChatMessage.createErrorMessage(
                        error.message ?: "Unknown error occurred"
                    )
                    messages.add(errorMessage)
                }

                chatAdapter.submitList(messages.toList())
                scrollToBottom()

                // Clear image selection
                clearImageSelection()

            } catch (e: Exception) {
                // Remove typing indicator
                messages.removeAt(messages.size - 1)

                val errorMessage = ChatMessage.createErrorMessage(
                    "Failed to get response: ${e.message}"
                )
                messages.add(errorMessage)
                chatAdapter.submitList(messages.toList())
                scrollToBottom()
            } finally {
                setInputEnabled(true)
            }
        }
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val photoFile = File(cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        currentImageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        takePicture.launch(currentImageUri)
    }

    private fun handleImageSelected(uri: Uri) {
        currentImageUri = uri

        // Show image preview
        binding.apply {
            llImagePreview.visibility = View.VISIBLE
            ivImagePreview.setImageURI(uri)
        }

        // Convert to base64 in background
        lifecycleScope.launch {
            currentImageBase64 = ImageHelper.convertToBase64(this@AIChatbotActivity, uri)

            if (currentImageBase64 == null) {
                Toast.makeText(
                    this@AIChatbotActivity,
                    "Failed to process image",
                    Toast.LENGTH_SHORT
                ).show()
                clearImageSelection()
            }
        }
    }

    private fun clearImageSelection() {
        currentImageUri = null
        currentImageBase64 = null
        binding.llImagePreview.visibility = View.GONE
    }

    private fun setInputEnabled(enabled: Boolean) {
        binding.apply {
            etMessage.isEnabled = enabled
            btnSend.isEnabled = enabled
            btnCamera.isEnabled = enabled
            btnGallery.isEnabled = enabled
        }
    }

    private fun scrollToBottom() {
        binding.rvMessages.scrollToPosition(messages.size - 1)
    }
}