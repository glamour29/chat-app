package com.example.client.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.client.model.*
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

class ChatViewModel(
    private val repository: SocketRepository = SocketRepository()
) : ViewModel() {

    private val apiService = RetrofitClient.instance

    val users: StateFlow<List<User>> = repository.users
    
    private val _friends = MutableStateFlow<List<User>>(emptyList())
    val friends: StateFlow<List<User>> = _friends

    private val _pendingRequests = MutableStateFlow<List<User>>(emptyList())
    val pendingRequests: StateFlow<List<User>> = _pendingRequests

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _rooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val rooms: StateFlow<List<ChatRoom>> = _rooms

    var currentUserId: String = ""
    private var authToken: String = ""

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private var activeRoomId: String = ""

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.messagesByRoom.collectLatest { map ->
                if (activeRoomId.isNotBlank()) {
                    val list = map[activeRoomId] ?: emptyList()
                    _messages.value = list
                }
            }
        }
        
        viewModelScope.launch {
            repository.rooms.collect { socketRooms ->
                if (socketRooms.isNotEmpty()) {
                    _rooms.value = socketRooms
                }
            }
        }
    }

    fun connect(token: String, userId: String) {
        if (token.isBlank()) return
        authToken = token
        currentUserId = userId
        Log.d("CHAT_VM", "Connect thành công. Token: ${token.take(10)}...")
        repository.connect(token)
        refreshData()
    }

    fun refreshData() {
        if (authToken.isBlank()) return
        fetchRooms()
        fetchFriends()
        fetchPendingRequests()
    }

    fun fetchRooms() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getRooms("Bearer $authToken")
                _rooms.value = response
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun fetchFriends() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val friendsList = apiService.getFriends("Bearer $authToken")
                _friends.value = friendsList
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun fetchPendingRequests() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requests = apiService.getPendingRequests("Bearer $authToken")
                _pendingRequests.value = requests
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun searchUsers(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank() || authToken.isBlank()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            _isSearching.value = true
            try {
                var results = apiService.searchUsers("Bearer $authToken", trimmedQuery)
                if (results.isEmpty() && !trimmedQuery.startsWith("0") && !trimmedQuery.startsWith("+")) {
                    results = apiService.searchUsers("Bearer $authToken", "0$trimmedQuery")
                }
                _searchResults.value = results
            } catch (e: Exception) { 
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    fun joinExistingRoom(room: ChatRoom) {
        repository.joinRoom(room.id)
    }

    fun markAsSeen(message: Message) {
        // repository.markAsSeen(message.id)
    }

    fun onUserInputChanged(text: String) {
        // repository.sendTypingIndicator(activeRoomId)
    }

    fun markRoomAsRead(roomId: String) = repository.markRoomAsRead(roomId)

    fun sendMessage(content: String) {
        if (activeRoomId.isBlank() || currentUserId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.sendMessage(content, activeRoomId, currentUserId, if (content.startsWith("image:")) "image" else "text")
        }
    }

    fun sendImage(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
                    repository.sendMessage(base64String, activeRoomId, currentUserId, "image")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createGroup(name: String, memberIds: List<String>): ChatRoom {
        val tempRoom = ChatRoom(id = "temp_${UUID.randomUUID()}", name = name, isGroup = true, memberIds = memberIds)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiService.createGroup("Bearer $authToken", CreateGroupRequest(name, memberIds))
                fetchRooms()
            } catch (e: Exception) { e.printStackTrace() }
        }
        return tempRoom
    }

    fun addMember(roomId: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiService.addMember("Bearer $authToken", roomId, MemberRequest(userId))
                repository.addMember(roomId, userId)
                fetchRooms()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun sendFriendRequest(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiService.sendFriendRequest("Bearer $authToken", FriendRequest(userId))
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun acceptFriendRequest(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiService.acceptFriendRequest("Bearer $authToken", FriendRequest(userId))
                refreshData()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun setActiveRoom(roomId: String, roomName: String) {
        activeRoomId = roomId
        repository.syncMessages(roomId)
    }

    fun leaveRoom(roomId: String) = repository.leaveRoom(roomId)
    fun startPrivateChat(user: User): ChatRoom = repository.ensurePrivateRoom(currentUserId, user)
    fun disconnect() = repository.disconnect()
    fun pinRoom(roomId: String) = repository.pinRoom(roomId)
    fun unpinRoom(roomId: String) = repository.unpinRoom(roomId)
    fun archiveRoom(roomId: String) = repository.archiveRoom(roomId)
    fun muteRoom(roomId: String) = repository.muteRoom(roomId)
    fun unmuteRoom(roomId: String) = repository.unmuteRoom(roomId)
    fun renameGroup(roomId: String, newName: String) = repository.renameGroup(roomId, newName)
    fun kickMember(roomId: String, userId: String) = repository.kickMember(roomId, userId)
    fun transferAdmin(roomId: String, newAdminId: String) = repository.transferAdmin(roomId, newAdminId)
}
