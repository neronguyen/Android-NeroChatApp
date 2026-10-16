package io.github.neronguyen.chat.core.database.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import io.github.neronguyen.chat.core.database.entity.ChatMessageEntity
import io.github.neronguyen.chat.core.database.entity.ChatParticipantEntity
import io.github.neronguyen.chat.core.database.entity.ChatRoomEntity
import io.github.neronguyen.chat.core.database.entity.ChatRoomParticipantCrossRef

data class ChatRoomWithDetails(
    @Embedded val room: ChatRoomEntity,

    @Relation(
        parentColumn = "creatorId",
        entityColumn = "userId"
    )
    val creator: ChatParticipantEntity?,

    @Relation(
        parentColumn = "id",
        entityColumn = "userId",
        associateBy = Junction(
            value = ChatRoomParticipantCrossRef::class,
            parentColumn = "roomId",
            entityColumn = "userId"
        )
    )
    val participants: List<ChatParticipantEntity>,

    @Relation(
        parentColumn = "lastMessageId",
        entityColumn = "id"
    )
    val lastMessage: ChatMessageEntity?
)
