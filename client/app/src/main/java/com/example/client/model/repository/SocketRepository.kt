package com.example.client.model.repository

import android.util.Log
import com.example.client.model.data.Message
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.net.URISyntaxException

class SocketRepository {
    private var socket: Socket? = null
    private val gson = Gson()

    // Server URL (đổi thành IP máy tính của bạn nếu chạy máy ảo Android: http://10.0.2.2:3000)
    private val SERVER_URL = "http://10.0.2.2:3000"

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    init {
        try {
            socket = IO.socket(SERVER_URL)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    fun connect() {
        socket?.connect()
        setupEvents()
    }

    fun disconnect() {
        socket?.disconnect()
    }

    private fun setupEvents() {
        socket?.on(Socket.EVENT_CONNECT) {
            Log.d("SocketRepo", "Connected: ${socket?.id()}\")")
        }

        // Lắng nghe tin nhắn mới từ server gửi về
        socket?.on("receive_message") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val message = gson.fromJson(data.toString(), Message::class.java)
                // Cập nhật list tin nhắn mới nhất
                val currentList = _messages.value.toMutableList()
                currentList.add(message)
                _messages.value = currentList
            }
        }
    }

    fun sendMessage(content: String, roomId: String, senderId: String) {
        val messageJson = JSONObject()
        messageJson.put("roomId", roomId)
        messageJson.put("senderId", senderId)
        messageJson.put("content", content)
        messageJson.put("type", "TEXT")

        // Gửi event 'send_message' lên server
        socket?.emit("send_message", messageJson)
    }

    fun joinRoom(roomId: String) {
        socket?.emit("join_room", roomId)
    }
}