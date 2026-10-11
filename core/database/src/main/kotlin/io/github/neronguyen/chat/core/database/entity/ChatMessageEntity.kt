package io.github.neronguyen.chat.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.neronguyen.chat.core.model.MessageDeliveryStatus

@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["chatRoomId", "timestamp"]),
        Index(value = ["tempId"])
    ]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val tempId: String? = null,
    val chatRoomId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val deliveryStatus: MessageDeliveryStatus
)
