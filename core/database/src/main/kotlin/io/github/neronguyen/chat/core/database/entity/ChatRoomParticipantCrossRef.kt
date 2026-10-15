package io.github.neronguyen.chat.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "chat_room_participants",
    primaryKeys = ["roomId", "userId"],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["roomId"])
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
data class ChatRoomParticipantCrossRef(
    val roomId: String,
    val userId: String
)
