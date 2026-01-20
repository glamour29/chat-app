// Message.kt
package com.example.client.model.data
import org.json.JSONArray
import org.json.JSONObject

data class MessageReaction(
    val emoji: String,
    val userIds: List<String>,
    val count: Int
)

data class Message(
    val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val content: String = "",
    val type: String = "TEXT",
    val createdAt: String = "", // Server đã gửi string "HH:mm" về đây rồi
    val timestamp: Long = 0L,
    val status: String = "sent",
    val isPinned: Boolean = false,
    val reactions: List<MessageReaction> = emptyList()
) {
    companion object {
        fun fromJson(json: JSONObject): Message {
            // Parse reactions
            val reactions = mutableListOf<MessageReaction>()
            val reactionsArray = json.optJSONArray("reactions")
            if (reactionsArray != null) {
                for (i in 0 until reactionsArray.length()) {
                    val reactionObj = reactionsArray.optJSONObject(i)
                    if (reactionObj != null) {
                        val emoji = reactionObj.optString("emoji")
                        val userIds = mutableListOf<String>()
                        val userIdsArray = reactionObj.optJSONArray("userIds")
                        if (userIdsArray != null) {
                            for (j in 0 until userIdsArray.length()) {
                                userIds.add(userIdsArray.optString(j))
                            }
                        }
                        reactions.add(
                            MessageReaction(
                                emoji = emoji,
                                userIds = userIds,
                                count = reactionObj.optInt("count", userIds.size)
                            )
                        )
                    }
                }
            }

            return Message(
                id = json.optString("id"),
                roomId = json.optString("roomId"),
                senderId = json.optString("senderId"),
                content = json.optString("content"),
                type = json.optString("type", "TEXT"),
                createdAt = json.optString("createdAt", ""),
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                status = json.optString("status", "sent"),
                isPinned = json.optBoolean("isPinned", false),
                reactions = reactions
            )
        }
    }
}
