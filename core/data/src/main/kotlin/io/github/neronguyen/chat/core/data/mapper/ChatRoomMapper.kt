package io.github.neronguyen.chat.core.data.mapper

import io.github.neronguyen.chat.core.database.entity.ChatMessageEntity
import io.github.neronguyen.chat.core.database.entity.ChatParticipantEntity
import io.github.neronguyen.chat.core.database.entity.ChatRoomEntity
import io.github.neronguyen.chat.core.database.model.ChatRoomWithDetails
import io.github.neronguyen.chat.core.model.ChatMessage
import io.github.neronguyen.chat.core.model.ChatParticipant
import io.github.neronguyen.chat.core.model.ChatRoom
import io.github.neronguyen.chat.core.model.MessageDeliveryStatus
import io.github.neronguyen.chat.core.network.model.ChatMessageDto
import io.github.neronguyen.chat.core.network.model.ChatParticipantDto
import io.github.neronguyen.chat.core.network.model.ChatRoomDto
import kotlin.time.Instant

fun ChatParticipantDto.toDomain(): ChatParticipant {
    return ChatParticipant(
        userId = userId,
        email = email,
        displayName = displayName,
        profilePictureUrl = profilePictureUrl
    )
}

fun ChatParticipantDto.toEntity(): ChatParticipantEntity {
    return ChatParticipantEntity(
        userId = userId,
        email = email,
        displayName = displayName,
        profilePictureUrl = profilePictureUrl
    )
}

fun ChatParticipantEntity.toDomain(): ChatParticipant {
    return ChatParticipant(
        userId = userId,
        email = email,
        displayName = displayName,
        profilePictureUrl = profilePictureUrl
    )
}

fun ChatMessageDto.toDomain(
    status: MessageDeliveryStatus = MessageDeliveryStatus.SENT
): ChatMessage {
    return ChatMessage(
        id = id,
        roomId = roomId,
        senderId = senderId,
        content = content,
        timestamp = createdAt,
        deliveryStatus = status
    )
}

fun ChatMessageDto.toEntity(
    status: MessageDeliveryStatus = MessageDeliveryStatus.SENT
): ChatMessageEntity {
    return ChatMessageEntity(
        id = id,
        roomId = roomId,
        senderId = senderId,
        content = content,
        timestamp = createdAt.toEpochMilliseconds(),
        deliveryStatus = status
    )
}

fun ChatMessageEntity.toDomain(): ChatMessage {
    return ChatMessage(
        id = id,
        roomId = roomId,
        senderId = senderId,
        content = content,
        timestamp = Instant.fromEpochMilliseconds(timestamp),
        deliveryStatus = deliveryStatus
    )
}

fun ChatMessage.toEntity(): ChatMessageEntity {
    return ChatMessageEntity(
        id = id,
        roomId = roomId,
        senderId = senderId,
        content = content,
        timestamp = timestamp.toEpochMilliseconds(),
        deliveryStatus = deliveryStatus
    )
}

fun ChatRoomDto.toDomain(): ChatRoom {
    return ChatRoom(
        id = id,
        type = type,
        name = name,
        creator = creator.toDomain(),
        participants = participants.map { it.toDomain() },
        lastMessage = lastMessage?.toDomain(),
        lastActivityAt = lastActivityAt
    )
}

fun ChatRoomDto.toEntity(): ChatRoomEntity {
    return ChatRoomEntity(
        id = id,
        type = type,
        name = name,
        creatorId = creator.userId,
        lastMessageId = lastMessage?.id,
        lastActivityAt = lastActivityAt?.toEpochMilliseconds() ?: 0L
    )
}

fun ChatRoomWithDetails.toDomain(): ChatRoom {
    val creatorParticipant = creator?.toDomain()
        ?: ChatParticipant(userId = room.creatorId, email = "", displayName = "")
    return ChatRoom(
        id = room.id,
        type = room.type,
        name = room.name,
        creator = creatorParticipant,
        participants = participants.map { it.toDomain() },
        lastMessage = lastMessage?.toDomain(),
        lastActivityAt = Instant.fromEpochMilliseconds(room.lastActivityAt)
    )
}
