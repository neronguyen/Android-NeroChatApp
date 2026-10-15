package io.github.neronguyen.chat.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.neronguyen.chat.core.model.MessageDeliveryStatus

@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["roomId", "timestamp"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ChatRoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val deliveryStatus: MessageDeliveryStatus
)
