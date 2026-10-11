package io.github.neronguyen.chat.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "chat_room_participants",
    primaryKeys = ["chatId", "userId"],
    indices = [Index(value = ["userId"]), Index(value = ["chatId"])]
)
data class ChatRoomParticipantCrossRef(
    val chatId: String,
    val userId: String
)
