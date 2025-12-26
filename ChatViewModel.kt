package com.example.organicstate.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.organicstate.data.model.CustomerMessage
import com.example.organicstate.data.repository.ChatRepository
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val repository = ChatRepository()

    private val _messages = MutableLiveData<List<CustomerMessage>>()
    val messages: LiveData<List<CustomerMessage>> = _messages

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _messageSent = MutableLiveData<Boolean>()
    val messageSent: LiveData<Boolean> = _messageSent

    /**
     * Load messages for a chat
     */
    fun loadMessages(chatId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
                repository.getMessages(chatId).collect { messageList ->
                    _messages.value = messageList
                    _loading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _loading.value = false
            }
        }
    }

    /**
     * Send a message
     */
    fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        messageText: String
    ) {
        viewModelScope.launch {
            try {
                _loading.value = true
                val result = repository.sendMessage(chatId, senderId, senderName, messageText)

                if (result.isSuccess) {
                    _messageSent.value = true
                    _error.value = null
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to send message"
                }

                _loading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _loading.value = false
            }
        }
    }

    /**
     * Mark messages as read
     */
    fun markAsRead(chatId: String, userId: String) {
        viewModelScope.launch {
            repository.markMessagesAsRead(chatId, userId)
        }
    }
}