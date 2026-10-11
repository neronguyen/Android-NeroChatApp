package io.github.neronguyen.chat.core.network.model

import io.github.neronguyen.chat.core.model.ChatRoomType
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ChatRoomDto(
    val id: String,
    val type: ChatRoomType,
    val name: String? = null,
    val creator: ChatParticipantDto,
    val participants: List<ChatParticipantDto>,
    val lastMessage: ChatMessageDto? = null,
    val lastActivityAt: Instant? = null
)
