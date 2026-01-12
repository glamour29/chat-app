package com.example.client.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.client.model.data.Message
import com.example.client.model.repository.SocketRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val repository = SocketRepository()

    val messages: StateFlow<List<Message>> = repository.messages

    // Giả lập thông tin User hiện tại (Sau này lấy từ Login của Kiên)
    val currentUserId = "user_123"
    val currentRoomId = "room_abc"

    init {
        repository.connect()
        joinRoom(currentRoomId)
    }

    fun sendMessage(content: String) {
        if (content.isNotBlank()) {
            repository.sendMessage(content, currentRoomId, currentUserId)
        }
    }

    fun joinRoom(roomId: String) {
        repository.joinRoom(roomId)
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }
}