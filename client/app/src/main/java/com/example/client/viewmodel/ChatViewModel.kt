// File: `app/src/main/java/com/example/client/viewmodel/ChatViewModel.kt`
package com.example.client.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.client.model.data.ChatRoom
import com.example.client.model.data.Message
import com.example.client.model.data.User
import com.example.client.model.repository.SocketRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel shim that provides the methods and properties the UI expects.
 * This is intentionally simple and delegates to the SocketRepository stub.
 */
class ChatViewModel(
    private val repository: SocketRepository = SocketRepository()
) : ViewModel() {

    // Public flows from repository
    val users: StateFlow<List<User>> = repository.users
    val rooms: StateFlow<List<ChatRoom>> = repository.rooms

    // Current logged-in user id and auth token
    var currentUserId: String = ""
    private var authToken: String = ""

    // Active room and messages for that room
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private var activeRoomId: String = ""
    // currentRoomId is used by UI; keep a public copy
    var currentRoomId: String = ""

    // typing user indicator (username or user id) - null when nobody typing
    private val _typingUser = MutableStateFlow<String?>(null)
    val typingUser: StateFlow<String?> = _typingUser

    init {
        // Observe repository messages map and update active messages when it changes
        viewModelScope.launch(Dispatchers.IO) {
            repository.messagesByRoom.collectLatest { map ->
                if (activeRoomId.isNotBlank()) {
                    val list = map[activeRoomId] ?: emptyList()
                    _messages.value = list
                }
            }
        }
    }

    // Connect with token (auth)
    fun connect(token: String, userId: String) {
        authToken = token
        currentUserId = userId
        repository.connect(token)
    }

    fun disconnect() {
        repository.disconnect()
    }

    // Set active room and update messages flow
    fun setActiveRoom(roomId: String, roomName: String) {
        activeRoomId = roomId
        // Ask repository to sync messages for this room
        repository.syncMessages(roomId)
        // If repository already has messages, they'll be reflected through the collector
    }

    fun markRoomAsRead(roomId: String) {
        repository.markRoomAsRead(roomId)
    }

    fun sendImage(context: Context, uri: Uri) {
        // Convert to a placeholder string or call repository to upload/send
        val placeholder = "image:$uri"
        sendMessage(placeholder)
    }

    // Join room by id (sets currentRoomId and asks repository to join/sync)
    fun joinRoom(roomId: String) {
        currentRoomId = roomId
        activeRoomId = roomId
        repository.joinRoom(roomId)
        repository.syncMessages(roomId)
    }

    fun markAsSeen(message: Message) {
        // Emit message_seen event
        try {
            repository.sendMessage(message.content, message.roomId, message.senderId, message.type)
            // In a real backend you'd emit a separate message_seen event; here we reuse send
        } catch (_: Exception) { }
    }

    fun onUserInputChanged(text: String) {
        // Could emit typing events via repository.socket - stub no-op
        repository.sendStopTyping(activeRoomId)
    }

    // UI calls sendMessage with a single content param; use activeRoomId and currentUserId
    fun sendMessage(content: String) {
        if (activeRoomId.isBlank() || currentUserId.isBlank()) return
        val type = if (content.startsWith("data:image") || content.startsWith("image:")) "image" else "text"

        // create temp message locally
        val tempId = "local_${UUID.randomUUID()}"
        val tempMessage = Message(
            id = tempId,
            roomId = activeRoomId,
            senderId = currentUserId,
            content = content,
            type = type,
            createdAt = "",
            timestamp = System.currentTimeMillis(),
            status = "sending"
        )

        // append to local list immediately
        val current = _messages.value.toMutableList()
        current.add(tempMessage)
        _messages.value = current

        // send to repository; repository will append real message when server acks
        viewModelScope.launch(Dispatchers.IO) {
            repository.sendMessage(content, activeRoomId, currentUserId, type)
        }
    }

    // Room and member management helpers expected by UI

    fun leaveRoom(roomId: String) = repository.leaveRoom(roomId)
    fun pinRoom(roomId: String) = repository.pinRoom(roomId)
    fun unpinRoom(roomId: String) = repository.unpinRoom(roomId)
    fun muteRoom(roomId: String) = repository.muteRoom(roomId)
    fun unmuteRoom(roomId: String) = repository.unmuteRoom(roomId)
    fun archiveRoom(roomId: String) = repository.archiveRoom(roomId)
    fun unarchiveRoom(roomId: String) = repository.unarchiveRoom(roomId)

    fun addMember(roomId: String, userId: String) = repository.addMember(roomId, userId)
    fun kickMember(roomId: String, userId: String) = repository.kickMember(roomId, userId)
    fun renameGroup(roomId: String, newName: String) = repository.renameGroup(roomId, newName)
    fun transferAdmin(roomId: String, newAdminId: String) = repository.transferAdmin(roomId, newAdminId)

    // Start or get a private room with a user
    fun startPrivateChat(user: User): ChatRoom {
        return repository.ensurePrivateRoom(currentUserId, user)
    }

    // Create group (UI calls without currentUserId)
    fun createGroup(name: String, memberIds: List<String>): ChatRoom {
        return repository.createGroup(name, memberIds, currentUserId)
    }
}