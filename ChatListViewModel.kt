package com.example.organicstate.ui.farmer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.organicstate.data.model.Chat
import com.example.organicstate.data.repository.ChatRepository
import kotlinx.coroutines.launch

class ChatListViewModel : ViewModel() {

    private val repository = ChatRepository()

    private val _chats = MutableLiveData<List<Chat>>()
    val chats: LiveData<List<Chat>> = _chats

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun loadUserChats(userId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
                repository.getUserChats(userId).collect { chatList ->
                    _chats.value = chatList
                    _loading.value = false
                }
            } catch (e: Exception) {
                _loading.value = false
            }
        }
    }
}