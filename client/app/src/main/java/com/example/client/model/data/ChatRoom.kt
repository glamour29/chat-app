package com.example.client.model.data

import org.json.JSONObject

/**
 * Thông tin phòng chat (group hoặc 1-1).
 */
data class ChatRoom(
    val id: String,
    val name: String,
    val isGroup: Boolean,
    val memberIds: List<String> = emptyList(),
    val adminId: String = "",
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val lastUpdated: Long = 0L,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false
) {
    companion object {
        fun fromJson(json: JSONObject): ChatRoom {
            val members = mutableListOf<String>()
            val memberArray = json.optJSONArray("members")
            if (memberArray != null) {
                for (i in 0 until memberArray.length()) {
                    members.add(memberArray.optString(i))
                }
            }
            return ChatRoom(
                id = json.optString("id", json.optString("roomId")),
                name = json.optString("name", json.optString("roomName", "")),
                isGroup = json.optBoolean("isGroup", true),
                memberIds = members,
                adminId = json.optString("adminId", ""),
                lastMessage = json.optString("lastMessage", ""),
                lastSenderId = json.optString("lastSenderId", ""),
                lastUpdated = json.optLong("lastUpdated", 0L),
                unreadCount = json.optInt("unreadCount", 0),
                isPinned = json.optBoolean("isPinned", false),
                isMuted = json.optBoolean("isMuted", false),
                isArchived = json.optBoolean("isArchived", false)
            )
        }
    }
}
