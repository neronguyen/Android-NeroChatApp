package io.github.neronguyen.chat.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.neronguyen.chat.core.model.ChatRoomType

@Entity(
    tableName = "chat_rooms",
    indices = [Index(value = ["lastActivityAt"])]
)
data class ChatRoomEntity(
    @PrimaryKey val id: String,
    val type: ChatRoomType = ChatRoomType.DIRECT,
    val name: String? = null,
    val creatorId: String,
    val lastMessageId: String? = null,
    val lastActivityAt: Long = 0L
)
