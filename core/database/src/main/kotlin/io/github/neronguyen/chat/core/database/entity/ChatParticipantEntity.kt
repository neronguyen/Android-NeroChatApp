package io.github.neronguyen.chat.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_participants")
data class ChatParticipantEntity(
    @PrimaryKey val userId: String,
    val email: String,
    val displayName: String,
    val profilePictureUrl: String? = null
)
