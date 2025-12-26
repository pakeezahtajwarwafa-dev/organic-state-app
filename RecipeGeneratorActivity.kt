package com.example.organicstate.ui.customer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.organicstate.R
import com.example.organicstate.data.model.RecipeMessage
import com.example.organicstate.data.remote.RecipeAIService
import com.example.organicstate.ui.adapters.RecipeMessageAdapter
import com.example.organicstate.databinding.ActivityRecipeGeneratorBinding  // ADD THIS LINE
import kotlinx.coroutines.launch

class RecipeGeneratorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeGeneratorBinding
    private lateinit var messageAdapter: RecipeMessageAdapter
    private val messages = mutableListOf<RecipeMessage>()
    private val aiService = RecipeAIService()

    private var currentMode: ChatMode = ChatMode.ITEMS_TO_RECIPES

    enum class ChatMode {
        ITEMS_TO_RECIPES,
        RECIPES_TO_ITEMS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeGeneratorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        showWelcomeMessage()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "AI Recipe Assistant"
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        messageAdapter = RecipeMessageAdapter()
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@RecipeGeneratorActivity)
            adapter = messageAdapter
        }
    }

    private fun setupListeners() {
        // Mode toggle buttons
        binding.btnItemsToRecipes.setOnClickListener {
            switchMode(ChatMode.ITEMS_TO_RECIPES)
        }

        binding.btnRecipesToItems.setOnClickListener {
            switchMode(ChatMode.RECIPES_TO_ITEMS)
        }

        // Send button
        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
            }
        }

        // Quick suggestion chips
        binding.chipQuickRecipe.setOnClickListener {
            binding.etMessage.setText("I have tomatoes, onions, and rice. What can I cook?")
        }

        binding.chipHealthy.setOnClickListener {
            binding.etMessage.setText("Suggest healthy recipes with vegetables")
        }

        binding.chipIngredients.setOnClickListener {
            binding.etMessage.setText("What ingredients do I need for chicken curry?")
        }
    }

    private fun switchMode(mode: ChatMode) {
        currentMode = mode

        when (mode) {
            ChatMode.ITEMS_TO_RECIPES -> {
                binding.btnItemsToRecipes.setBackgroundColor(getColor(R.color.primary_green))
                binding.btnRecipesToItems.setBackgroundColor(getColor(R.color.background))
                binding.etMessage.hint = "List your ingredients (e.g., tomatoes, rice, chicken...)"
            }
            ChatMode.RECIPES_TO_ITEMS -> {
                binding.btnRecipesToItems.setBackgroundColor(getColor(R.color.primary_green))
                binding.btnItemsToRecipes.setBackgroundColor(getColor(R.color.background))
                binding.etMessage.hint = "Enter recipe name or describe what you want to cook..."
            }
        }

//        // Add mode switch message
//        val modeMessage = when (mode) {
//            ChatMode.ITEMS_TO_RECIPES -> "Switched to: Ingredients → Recipes mode"
//            ChatMode.RECIPES_TO_ITEMS -> "Switched to: Recipe → Ingredients mode"
//        }
//        addSystemMessage(modeMessage)
    }

    private fun showWelcomeMessage() {
        val welcomeText = """
            👋 Welcome to AI Recipe Assistant!
            
            I can help you in two ways:
            
            🥗 **Ingredients → Recipes**
            Tell me what ingredients you have, and I'll suggest delicious recipes!
            
            📝 **Recipe → Ingredients**
            Tell me what you want to cook, and I'll provide the complete ingredient list!
            
            Choose a mode above and let's get cooking! 👨‍🍳
        """.trimIndent()

        addAIMessage(welcomeText)
    }

    private fun sendMessage(text: String) {
        // Add user message
        addUserMessage(text)

        // Clear input
        binding.etMessage.text?.clear()

        // Show typing indicator
        addTypingIndicator()

        // Send to AI
        lifecycleScope.launch {
            try {
                val response = when (currentMode) {
                    ChatMode.ITEMS_TO_RECIPES -> {
                        aiService.getRecipesFromIngredients(text)
                    }
                    ChatMode.RECIPES_TO_ITEMS -> {
                        aiService.getIngredientsFromRecipe(text)
                    }
                }

                response.fold(
                    onSuccess = { aiResponse ->
                        removeTypingIndicator()
                        addAIMessage(aiResponse)
                    },
                    onFailure = { error ->
                        removeTypingIndicator()
                        addErrorMessage("Sorry, I couldn't process that. ${error.message}")
                    }
                )
            } catch (e: Exception) {
                removeTypingIndicator()
                addErrorMessage("An error occurred: ${e.message}")
            }
        }
    }

    private fun addUserMessage(text: String) {
        val message = RecipeMessage.createUserMessage(text)
        messages.add(message)
        messageAdapter.submitList(messages.toList())
        scrollToBottom()
    }

    private fun addAIMessage(text: String) {
        val message = RecipeMessage.createAIMessage(text)
        messages.add(message)
        messageAdapter.submitList(messages.toList())
        scrollToBottom()
    }

    private fun addSystemMessage(text: String) {
        val message = RecipeMessage.createSystemMessage(text)
        messages.add(message)
        messageAdapter.submitList(messages.toList())
        scrollToBottom()
    }

    private fun addTypingIndicator() {
        val message = RecipeMessage.createTypingIndicator()
        messages.add(message)
        messageAdapter.submitList(messages.toList())
        scrollToBottom()
    }

    private fun removeTypingIndicator() {
        messages.removeAll { it.isTyping }
        messageAdapter.submitList(messages.toList())
    }

    private fun addErrorMessage(text: String) {
        val message = RecipeMessage.createErrorMessage(text)
        messages.add(message)
        messageAdapter.submitList(messages.toList())
        scrollToBottom()
        Toast.makeText(this, "Error occurred", Toast.LENGTH_SHORT).show()
    }

    private fun scrollToBottom() {
        binding.rvMessages.post {
            if (messages.isNotEmpty()) {
                binding.rvMessages.smoothScrollToPosition(messages.size - 1)
            }
        }
    }
}